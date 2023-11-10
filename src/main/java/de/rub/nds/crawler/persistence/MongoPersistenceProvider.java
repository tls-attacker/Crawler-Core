/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.persistence;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.lang.NonNull;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

/** A persistence provider implementation using MongoDB as the persistence layer. */
public class MongoPersistenceProvider implements IPersistenceProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean isInitialized = false;
    private static final Set<JsonSerializer<?>> serializers = new HashSet<>();
    private static final Set<Module> modules = new HashSet<>();

    public static void registerSerializer(JsonSerializer<?> serializer) {
        if (isInitialized) {
            throw new RuntimeException("Cannot register serializer after initialization");
        }
        serializers.add(serializer);
    }

    public static void registerSerializer(JsonSerializer<?>... serializers) {
        for (JsonSerializer<?> serializer : serializers) {
            registerSerializer(serializer);
        }
    }

    public static void registerModule(Module module) {
        if (isInitialized) {
            throw new RuntimeException("Cannot register module after initialization");
        }
        modules.add(module);
    }

    public static void registerModule(Module... modules) {
        for (Module module : modules) {
            registerModule(module);
        }
    }

    private final MongoClient mongoClient;
    private final ObjectMapper mapper;
    private final Map<String, JacksonMongoCollection<ScanResult>> collectionByDbAndCollectionName;
    private JacksonMongoCollection<BulkScan> bulkScanCollection;

    /**
     * Initialize connection to mongodb and setup MongoJack PojoToBson mapper.
     *
     * @param mongoDbDelegate Mongodb command line configuration parameters
     */
    public MongoPersistenceProvider(MongoDbDelegate mongoDbDelegate) {
        isInitialized = true;

        ConnectionString connectionString =
                new ConnectionString(
                        "mongodb://"
                                + mongoDbDelegate.getMongoDbHost()
                                + ":"
                                + mongoDbDelegate.getMongoDbPort());
        String pw = "";
        if (mongoDbDelegate.getMongoDbPass() != null) {
            pw = mongoDbDelegate.getMongoDbPass();
        } else if (mongoDbDelegate.getMongoDbPassFile() != null) {
            try {
                pw = Files.readAllLines(Paths.get(mongoDbDelegate.getMongoDbPassFile())).get(0);
            } catch (IOException e) {
                LOGGER.error("Could not read mongoDb password file: ", e);
            }
        }

        MongoCredential credentials =
                MongoCredential.createCredential(
                        mongoDbDelegate.getMongoDbUser(),
                        mongoDbDelegate.getMongoDbAuthSource(),
                        pw.toCharArray());

        this.mapper = new ObjectMapper();
        LOGGER.trace("Constructor()");
        this.collectionByDbAndCollectionName = new HashMap<>();

        if (!serializers.isEmpty()) {
            SimpleModule serializerModule = new SimpleModule();
            for (JsonSerializer<?> serializer : serializers) {
                serializerModule.addSerializer(serializer);
            }
            mapper.registerModule(serializerModule);
        }
        for (Module module : modules) {
            mapper.registerModule(module);
        }
        mapper.registerModule(new JavaTimeModule());

        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configOverride(BigDecimal.class)
                .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));

        MongoClientSettings mongoClientSettings =
                MongoClientSettings.builder()
                        .credential(credentials)
                        .applyConnectionString(connectionString)
                        .build();
        this.mongoClient = MongoClients.create(mongoClientSettings);

        try {
            this.mongoClient.startSession();
        } catch (Exception e) {
            LOGGER.error("Could not connect to MongoDB: ", e);
            throw new RuntimeException(e);
        }

        LOGGER.info("MongoDB persistence provider initialized, connected to {}.", connectionString);
    }

    /**
     * On first call creates a collection with the specified name for the specified database and
     * saves it in a hashmap. On repeating calls with same parameters returns the saved collection.
     *
     * @param dbName Name of the database to use.
     * @param collectionName Name of the collection to create/return
     */
    private JacksonMongoCollection<ScanResult> getCollection(String dbName, String collectionName) {
        if (collectionByDbAndCollectionName.containsKey(dbName + collectionName)) {
            return collectionByDbAndCollectionName.get(dbName + collectionName);
        } else {
            MongoDatabase database = this.mongoClient.getDatabase(dbName);
            LOGGER.info("Init database: {}.", dbName);
            LOGGER.info("Init collection: {}.", collectionName);

            JacksonMongoCollection<ScanResult> collection =
                    JacksonMongoCollection.builder()
                            .withObjectMapper(mapper)
                            .build(
                                    database,
                                    collectionName,
                                    ScanResult.class,
                                    UuidRepresentation.STANDARD);
            collectionByDbAndCollectionName.put(dbName + collectionName, collection);

            return collection;
        }
    }

    private JacksonMongoCollection<BulkScan> getBulkScanCollection(String dbName) {
        if (this.bulkScanCollection == null) {
            MongoDatabase database = this.mongoClient.getDatabase(dbName);
            this.bulkScanCollection =
                    JacksonMongoCollection.builder()
                            .withObjectMapper(mapper)
                            .build(
                                    database,
                                    "bulkScans",
                                    BulkScan.class,
                                    UuidRepresentation.STANDARD);
        }
        return this.bulkScanCollection;
    }

    @Override
    public void insertBulkScan(@NonNull BulkScan bulkScan) {
        this.getBulkScanCollection(bulkScan.getName()).insertOne(bulkScan);
    }

    @Override
    public void updateBulkScan(@NonNull BulkScan bulkScan) {
        this.getBulkScanCollection(bulkScan.getName()).removeById(bulkScan.get_id());
        this.insertBulkScan(bulkScan);
    }

    /**
     * Inserts the task into a collection named after the scan and a database named after the
     * workspace of the scan.
     *
     * @param scanResult The new scan task.
     */
    @Override
    public void insertScanResult(ScanResult scanResult, ScanJobDescription scanJobDescription) {
        if (scanResult.getResultStatus() != scanJobDescription.getStatus()) {
            LOGGER.warn(
                    "ScanResult status ({}) does not match ScanJobDescription status ({})",
                    scanResult.getResultStatus(),
                    scanJobDescription.getStatus());
            throw new IllegalArgumentException(
                    "ScanResult status does not match ScanJobDescription status");
        }
        try {
            LOGGER.info(
                    "Writing result ({}) for {} into collection: {}",
                    scanResult.getResultStatus(),
                    scanResult.getScanTarget().getHostname(),
                    scanJobDescription.getCollectionName());
            this.getCollection(
                            scanJobDescription.getDbName(), scanJobDescription.getCollectionName())
                    .insertOne(scanResult);
        } catch (Exception e) {
            // catch JsonMappingException etc.
            LOGGER.error("Exception while writing Result to MongoDB: ", e);
            if (scanResult.getResultStatus() != JobStatus.SERIALIZATION_ERROR) {
                scanJobDescription.setStatus(JobStatus.SERIALIZATION_ERROR);
                insertScanResult(
                        ScanResult.fromException(scanJobDescription, e), scanJobDescription);
            } else {
                LOGGER.error(
                        "Did not write serialization exception to MongoDB (to avoid infinite recursion)");
                scanJobDescription.setStatus(JobStatus.INTERNAL_ERROR);
            }
        }
    }
}
