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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

/** A persistence provider implementation using MongoDB as the persistence layer. */
public class MongoPersistenceProvider implements IPersistenceProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String BULK_SCAN_COLLECTION_NAME = "bulkScans";

    private static final Set<JsonSerializer<?>> serializers = new HashSet<>();
    private static final Set<Module> modules = new HashSet<>();
    private static final Object registrationLock = new Object();
    private static volatile boolean registrationClosed = false;

    public static void registerSerializer(JsonSerializer<?> serializer) {
        synchronized (registrationLock) {
            if (registrationClosed) {
                throw new RuntimeException("Cannot register serializer after initialization");
            }
            serializers.add(serializer);
        }
    }

    public static void registerSerializer(JsonSerializer<?>... serializers) {
        for (JsonSerializer<?> serializer : serializers) {
            registerSerializer(serializer);
        }
    }

    public static void registerModule(Module module) {
        synchronized (registrationLock) {
            if (registrationClosed) {
                throw new RuntimeException("Cannot register module after initialization");
            }
            modules.add(module);
        }
    }

    public static void registerModule(Module... modules) {
        for (Module module : modules) {
            registerModule(module);
        }
    }

    private final MongoClient mongoClient;
    private final ObjectMapper mapper;
    private final LoadingCache<String, MongoDatabase> databaseCache;
    private final LoadingCache<Pair<String, String>, JacksonMongoCollection<ScanResult>>
            resultCollectionCache;
    private JacksonMongoCollection<BulkScan> bulkScanCollection;

    private static MongoClient createMongoClient(MongoDbDelegate mongoDbDelegate) {
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

        MongoClientSettings mongoClientSettings =
                MongoClientSettings.builder()
                        .credential(credentials)
                        .applyConnectionString(connectionString)
                        .build();
        LOGGER.info("MongoDB persistence provider prepared to connect to {}", connectionString);
        return MongoClients.create(mongoClientSettings);
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();

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

        return mapper;
    }

    /**
     * Initialize connection to mongodb and setup MongoJack PojoToBson mapper.
     *
     * @param mongoDbDelegate Mongodb command line configuration parameters
     */
    public MongoPersistenceProvider(MongoDbDelegate mongoDbDelegate) {
        synchronized (registrationLock) {
            registrationClosed = true;
        }

        mapper = createMapper();
        mongoClient = createMongoClient(mongoDbDelegate);

        try {
            mongoClient.startSession();
            LOGGER.info("MongoDB persistence provider initialized and connected.");
        } catch (Exception e) {
            LOGGER.error("Could not connect to MongoDB: ", e);
            throw new RuntimeException(e);
        }

        databaseCache =
                CacheBuilder.newBuilder()
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .build(CacheLoader.from(this::initDatabase));
        resultCollectionCache =
                CacheBuilder.newBuilder()
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .build(
                                CacheLoader.from(
                                        key ->
                                                this.initResultCollection(
                                                        key.getLeft(), key.getRight())));
    }

    private MongoDatabase initDatabase(String dbName) {
        LOGGER.info("Initializing database: {}.", dbName);
        return mongoClient.getDatabase(dbName);
    }

    private JacksonMongoCollection<ScanResult> initResultCollection(
            String dbName, String collectionName) {
        LOGGER.info("Initializing collection: {}.{}.", dbName, collectionName);
        var collection =
                JacksonMongoCollection.builder()
                        .withObjectMapper(mapper)
                        .build(
                                databaseCache.getUnchecked(dbName),
                                collectionName,
                                ScanResult.class,
                                UuidRepresentation.STANDARD);
        // createIndex is idempotent, hence we do not need to check if an index exists
        collection.createIndex(Indexes.ascending("scanTarget.ip"));
        collection.createIndex(Indexes.ascending("scanTarget.hostname"));
        collection.createIndex(Indexes.ascending("scanTarget.trancoRank"));
        collection.createIndex(Indexes.ascending("scanTarget.resultStatus"));
        return collection;
    }

    private JacksonMongoCollection<BulkScan> getBulkScanCollection(String dbName) {
        if (this.bulkScanCollection == null) {
            this.bulkScanCollection =
                    JacksonMongoCollection.builder()
                            .withObjectMapper(mapper)
                            .build(
                                    databaseCache.getUnchecked(dbName),
                                    BULK_SCAN_COLLECTION_NAME,
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

    private void writeResultToDatabase(
            String dbName, String collectionName, ScanResult scanResult) {
        LOGGER.info(
                "Writing result ({}) for {} into collection: {}",
                scanResult.getResultStatus(),
                scanResult.getScanTarget().getHostname(),
                collectionName);
        resultCollectionCache.getUnchecked(Pair.of(dbName, collectionName)).insertOne(scanResult);
    }

    @Override
    public void insertScanResult(ScanResult scanResult, ScanJobDescription scanJobDescription) {
        if (scanResult.getResultStatus() != scanJobDescription.getStatus()) {
            LOGGER.error(
                    "ScanResult status ({}) does not match ScanJobDescription status ({})",
                    scanResult.getResultStatus(),
                    scanJobDescription.getStatus());
            throw new IllegalArgumentException(
                    "ScanResult status does not match ScanJobDescription status");
        }
        try {
            writeResultToDatabase(
                    scanJobDescription.getDbName(),
                    scanJobDescription.getCollectionName(),
                    scanResult);
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
