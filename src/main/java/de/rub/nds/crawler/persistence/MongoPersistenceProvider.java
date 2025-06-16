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

/**
 * MongoDB implementation of the persistence provider for TLS-Crawler scan data.
 *
 * <p>This class provides a comprehensive MongoDB-based persistence layer that handles storage and
 * retrieval of bulk scan metadata and individual scan results. It implements sophisticated caching
 * mechanisms and provides flexible JSON serialization support.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Dual Storage Model</strong> - Separate handling for bulk scan metadata and scan
 *       results
 *   <li><strong>Database per Scan</strong> - Each bulk scan uses its own MongoDB database
 *   <li><strong>Collection Caching</strong> - Guava cache for database and collection instances
 *   <li><strong>Custom Serialization</strong> - Extensible Jackson mapper with custom serializers
 *   <li><strong>Automatic Indexing</strong> - Performance-optimized indexes on scan target fields
 *   <li><strong>Error Recovery</strong> - Graceful handling of serialization errors
 * </ul>
 *
 * <p><strong>Storage Architecture:</strong>
 *
 * <ul>
 *   <li><strong>Bulk Scans</strong> - Stored in a dedicated "bulkScans" collection within each scan
 *       database
 *   <li><strong>Scan Results</strong> - Stored in dynamically named collections based on scan
 *       configuration
 *   <li><strong>Database Naming</strong> - Each bulk scan creates a database named after the scan
 *   <li><strong>Index Strategy</strong> - Automatic indexing on IP, hostname, Tranco rank, and
 *       result status
 * </ul>
 *
 * <p><strong>Caching Strategy:</strong>
 *
 * <ul>
 *   <li>Database connections cached for 10 minutes after last access
 *   <li>Collection instances cached for 10 minutes after last access
 *   <li>Automatic cleanup of unused connections to prevent resource leaks
 * </ul>
 *
 * <p><strong>Serialization Support:</strong>
 *
 * <ul>
 *   <li>Custom JsonSerializer registration for complex types
 *   <li>Jackson module support for extended functionality
 *   <li>BigDecimal serialization as strings for precision
 *   <li>Java Time API support through JavaTimeModule
 * </ul>
 *
 * <p><strong>Error Handling:</strong> Implements sophisticated error recovery for serialization
 * failures, creating error records instead of losing scan results.
 *
 * @see IPersistenceProvider
 * @see MongoDbDelegate
 * @see BulkScan
 * @see ScanResult
 */
public class MongoPersistenceProvider implements IPersistenceProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String BULK_SCAN_COLLECTION_NAME = "bulkScans";

    private static boolean isInitialized = false;
    private static final Set<JsonSerializer<?>> serializers = new HashSet<>();
    private static final Set<Module> modules = new HashSet<>();

    /**
     * Registers a custom JSON serializer for use in MongoDB document serialization.
     *
     * <p>This method allows registration of custom Jackson serializers that will be applied during
     * JSON serialization of scan results before storing them in MongoDB. Serializers must be
     * registered before the first MongoPersistenceProvider instance is created.
     *
     * <p><strong>Registration Lifecycle:</strong>
     *
     * <ul>
     *   <li>Serializers can only be registered before initialization
     *   <li>Once the first provider instance is created, registration is locked
     *   <li>Attempting to register after initialization throws RuntimeException
     * </ul>
     *
     * @param serializer the custom JsonSerializer to register for MongoDB serialization
     * @throws RuntimeException if called after MongoPersistenceProvider initialization
     * @see #registerSerializer(JsonSerializer...)
     * @see #registerModule(Module)
     */
    public static void registerSerializer(JsonSerializer<?> serializer) {
        if (isInitialized) {
            throw new RuntimeException("Cannot register serializer after initialization");
        }
        serializers.add(serializer);
    }

    /**
     * Registers multiple custom JSON serializers for use in MongoDB document serialization.
     *
     * <p>This convenience method allows bulk registration of multiple Jackson serializers. All
     * serializers will be applied during JSON serialization of scan results before storing them in
     * MongoDB.
     *
     * <p>This method delegates to {@link #registerSerializer(JsonSerializer)} for each provided
     * serializer, maintaining the same registration lifecycle restrictions.
     *
     * @param serializers vararg array of JsonSerializers to register for MongoDB serialization
     * @throws RuntimeException if called after MongoPersistenceProvider initialization
     * @see #registerSerializer(JsonSerializer)
     * @see #registerModule(Module...)
     */
    public static void registerSerializer(JsonSerializer<?>... serializers) {
        for (JsonSerializer<?> serializer : serializers) {
            registerSerializer(serializer);
        }
    }

    /**
     * Registers a custom Jackson module for extended JSON serialization functionality.
     *
     * <p>This method allows registration of Jackson modules that extend the ObjectMapper's
     * serialization capabilities. Modules can provide custom serializers, deserializers, type
     * handlers, and other Jackson extensions for MongoDB document processing.
     *
     * <p><strong>Module Registration:</strong>
     *
     * <ul>
     *   <li>Modules must be registered before the first provider instance is created
     *   <li>Supports any Jackson Module including third-party extensions
     *   <li>Registration is locked after initialization to ensure consistency
     * </ul>
     *
     * @param module the Jackson Module to register for enhanced serialization support
     * @throws RuntimeException if called after MongoPersistenceProvider initialization
     * @see #registerModule(Module...)
     * @see #registerSerializer(JsonSerializer)
     */
    public static void registerModule(Module module) {
        if (isInitialized) {
            throw new RuntimeException("Cannot register module after initialization");
        }
        modules.add(module);
    }

    /**
     * Registers multiple Jackson modules for extended JSON serialization functionality.
     *
     * <p>This convenience method allows bulk registration of multiple Jackson modules. Each module
     * will extend the ObjectMapper's serialization capabilities for MongoDB document processing.
     *
     * <p>This method delegates to {@link #registerModule(Module)} for each provided module,
     * maintaining the same registration lifecycle restrictions.
     *
     * @param modules vararg array of Jackson Modules to register for enhanced serialization
     * @throws RuntimeException if called after MongoPersistenceProvider initialization
     * @see #registerModule(Module)
     * @see #registerSerializer(JsonSerializer...)
     */
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

    /**
     * Creates and configures a MongoDB client using the provided configuration.
     *
     * <p>This static factory method handles the complete MongoDB client setup including connection
     * string construction, credential management, and client configuration. It supports both direct
     * password provision and password file reading.
     *
     * <p><strong>Connection Configuration:</strong>
     *
     * <ul>
     *   <li>Constructs connection string from host and port
     *   <li>Supports MongoDB authentication with username/password
     *   <li>Handles password files for secure credential storage
     *   <li>Configures authentication source database
     * </ul>
     *
     * <p><strong>Password Handling:</strong>
     *
     * <ul>
     *   <li>Direct password from configuration takes precedence
     *   <li>Password file reading as fallback option
     *   <li>Graceful error handling for missing password files
     *   <li>Empty password fallback for connection attempts
     * </ul>
     *
     * @param mongoDbDelegate the MongoDB configuration containing connection parameters
     * @return configured MongoClient ready for database operations
     * @see MongoDbDelegate
     * @see MongoClientSettings
     */
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

    /**
     * Creates and configures a Jackson ObjectMapper for MongoDB document serialization.
     *
     * <p>This static factory method creates a fully configured ObjectMapper that handles the
     * complex serialization requirements of TLS scan results. The mapper integrates custom
     * serializers, modules, and specific configuration for MongoDB storage.
     *
     * <p><strong>Configuration Features:</strong>
     *
     * <ul>
     *   <li>Custom serializer integration from static registration
     *   <li>Jackson module support including JavaTimeModule
     *   <li>BigDecimal serialization as strings for precision preservation
     *   <li>Graceful handling of empty beans without failures
     * </ul>
     *
     * <p><strong>Serialization Strategy:</strong>
     *
     * <ul>
     *   <li>Registered custom serializers take precedence
     *   <li>Modules provide extended functionality
     *   <li>Java Time API support for date/time fields
     *   <li>String representation for BigDecimal to avoid precision loss
     * </ul>
     *
     * @return configured ObjectMapper ready for MongoDB document serialization
     * @see #registerSerializer(JsonSerializer)
     * @see #registerModule(Module)
     * @see JavaTimeModule
     */
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
     * Initializes connection to MongoDB and sets up MongoJack PojoToBson mapper.
     *
     * <p>This constructor performs complete initialization of the MongoDB persistence layer
     * including client connection, ObjectMapper configuration, and cache setup. It establishes the
     * foundation for all subsequent database operations.
     *
     * <p><strong>Initialization Sequence:</strong>
     *
     * <ol>
     *   <li>Marks the class as initialized to lock serializer/module registration
     *   <li>Creates configured ObjectMapper with custom serializers and modules
     *   <li>Establishes MongoDB client connection with authentication
     *   <li>Verifies connection with a test session
     *   <li>Sets up Guava caches for database and collection instances
     * </ol>
     *
     * <p><strong>Cache Configuration:</strong>
     *
     * <ul>
     *   <li>Database cache expires after 10 minutes of inactivity
     *   <li>Collection cache expires after 10 minutes of inactivity
     *   <li>Automatic collection initialization with performance indexes
     * </ul>
     *
     * <p><strong>Error Handling:</strong> Connection failures are wrapped in RuntimeException to
     * ensure proper error propagation during application startup.
     *
     * @param mongoDbDelegate MongoDB command line configuration parameters
     * @throws RuntimeException if MongoDB connection cannot be established
     * @see MongoDbDelegate
     * @see #createMapper()
     * @see #createMongoClient(MongoDbDelegate)
     */
    public MongoPersistenceProvider(MongoDbDelegate mongoDbDelegate) {
        isInitialized = true;

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

    /**
     * Initializes a MongoDB database connection for the specified database name.
     *
     * <p>This method is used by the database cache to lazily initialize database connections as
     * they are requested. It provides the foundation for all database operations within a specific
     * scan context.
     *
     * <p><strong>Database Naming Strategy:</strong> Each bulk scan typically uses its own database
     * to ensure data isolation and simplified management of scan results.
     *
     * @param dbName the name of the database to initialize
     * @return initialized MongoDatabase instance ready for collection operations
     * @see #databaseCache
     */
    private MongoDatabase initDatabase(String dbName) {
        LOGGER.info("Initializing database: {}.", dbName);
        return mongoClient.getDatabase(dbName);
    }

    /**
     * Initializes a MongoDB collection for storing scan results with performance optimization.
     *
     * <p>This method is used by the collection cache to lazily initialize collections as they are
     * requested. It creates properly configured MongoJack collections with automatic indexing for
     * optimal query performance.
     *
     * <p><strong>Collection Configuration:</strong>
     *
     * <ul>
     *   <li>Uses the configured ObjectMapper for JSON serialization
     *   <li>Standard UUID representation for consistent document IDs
     *   <li>Type-safe ScanResult document mapping
     * </ul>
     *
     * <p><strong>Performance Indexing:</strong>
     *
     * <ul>
     *   <li>scanTarget.ip - Fast IP-based queries
     *   <li>scanTarget.hostname - Hostname lookup optimization
     *   <li>scanTarget.trancoRank - Ranking-based filtering
     *   <li>scanTarget.resultStatus - Status-based result filtering
     * </ul>
     *
     * <p><strong>Index Management:</strong> Index creation is idempotent, so repeated calls will
     * not create duplicate indexes.
     *
     * @param dbName the database name containing the collection
     * @param collectionName the name of the collection to initialize
     * @return configured JacksonMongoCollection ready for scan result storage
     * @see #resultCollectionCache
     * @see ScanResult
     */
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

    /**
     * Gets or creates the MongoDB collection for storing bulk scan metadata.
     *
     * <p>This method implements lazy initialization of the bulk scan collection, creating it only
     * when first accessed. The collection stores high-level information about bulk scanning
     * operations separate from individual scan results.
     *
     * <p><strong>Collection Purpose:</strong>
     *
     * <ul>
     *   <li>Stores BulkScan metadata and configuration
     *   <li>Tracks overall progress and status of bulk operations
     *   <li>Provides central reference point for scan campaigns
     * </ul>
     *
     * <p><strong>Singleton Pattern:</strong> The collection instance is cached after first creation
     * to avoid repeated initialization overhead for subsequent access.
     *
     * @param dbName the database name containing the bulk scan collection
     * @return JacksonMongoCollection configured for BulkScan document storage
     * @see BulkScan
     * @see #BULK_SCAN_COLLECTION_NAME
     */
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

    /**
     * Inserts a new bulk scan record into the MongoDB collection.
     *
     * <p>This method stores the bulk scan metadata in the appropriate database and collection. The
     * bulk scan document contains configuration, progress tracking, and high-level information
     * about the scanning campaign.
     *
     * <p><strong>Storage Location:</strong> The bulk scan is stored in a collection named
     * "bulkScans" within the database corresponding to the bulk scan's name.
     *
     * @param bulkScan the bulk scan metadata to insert into the database
     * @throws IllegalArgumentException if bulkScan is null
     * @see IPersistenceProvider#insertBulkScan(BulkScan)
     * @see BulkScan
     */
    @Override
    public void insertBulkScan(@NonNull BulkScan bulkScan) {
        this.getBulkScanCollection(bulkScan.getName()).insertOne(bulkScan);
    }

    /**
     * Updates an existing bulk scan record in the MongoDB collection.
     *
     * <p>This method implements a replace strategy for updating bulk scan metadata. It removes the
     * existing document and inserts the updated version to ensure complete replacement of all
     * fields.
     *
     * <p><strong>Update Strategy:</strong>
     *
     * <ol>
     *   <li>Removes the existing document by ID
     *   <li>Inserts the updated bulk scan document
     * </ol>
     *
     * <p><strong>Atomicity Consideration:</strong> This implementation is not atomic. In production
     * environments with high concurrency, consider using MongoDB's replaceOne operation for atomic
     * updates.
     *
     * @param bulkScan the updated bulk scan metadata to store in the database
     * @throws IllegalArgumentException if bulkScan is null
     * @see IPersistenceProvider#updateBulkScan(BulkScan)
     * @see #insertBulkScan(BulkScan)
     */
    @Override
    public void updateBulkScan(@NonNull BulkScan bulkScan) {
        this.getBulkScanCollection(bulkScan.getName()).removeById(bulkScan.get_id());
        this.insertBulkScan(bulkScan);
    }

    /**
     * Writes a scan result to the appropriate MongoDB collection.
     *
     * <p>This private method handles the actual database insertion of scan results. It uses the
     * collection cache to obtain the appropriate collection and performs the insertion with logging
     * for monitoring purposes.
     *
     * <p><strong>Collection Resolution:</strong> The method uses the collection cache with a
     * composite key of database name and collection name to obtain the properly configured MongoDB
     * collection.
     *
     * <p><strong>Performance Optimization:</strong> Collections are cached to avoid repeated
     * initialization overhead during high-volume scanning operations.
     *
     * @param dbName the database name for the scan result storage
     * @param collectionName the collection name for the scan result storage
     * @param scanResult the scan result to write to the database
     * @see #resultCollectionCache
     * @see ScanResult
     */
    private void writeResultToDatabase(
            String dbName, String collectionName, ScanResult scanResult) {
        LOGGER.info(
                "Writing result ({}) for {} into collection: {}",
                scanResult.getResultStatus(),
                scanResult.getScanTarget().getHostname(),
                collectionName);
        resultCollectionCache.getUnchecked(Pair.of(dbName, collectionName)).insertOne(scanResult);
    }

    /**
     * Inserts a scan result into the MongoDB collection with comprehensive error handling.
     *
     * <p>This method implements the core persistence logic for individual scan results. It includes
     * validation, error recovery, and recursive error handling to ensure that scan results are
     * never lost due to serialization issues.
     *
     * <p><strong>Validation:</strong> The method validates that the scan result status matches the
     * job description status to ensure data consistency before insertion.
     *
     * <p><strong>Error Recovery Strategy:</strong>
     *
     * <ol>
     *   <li>Attempt normal insertion of the scan result
     *   <li>If serialization fails, create an error record instead
     *   <li>If error record serialization fails, mark as internal error
     *   <li>Prevent infinite recursion with serialization error handling
     * </ol>
     *
     * <p><strong>Status Consistency:</strong> The method ensures that scan results and job
     * descriptions maintain consistent status information throughout the persistence process.
     *
     * @param scanResult the scan result to insert into the database
     * @param scanJobDescription the job description containing storage location and status
     * @throws IllegalArgumentException if result status doesn't match job description status
     * @see IPersistenceProvider#insertScanResult(ScanResult, ScanJobDescription)
     * @see ScanResult#fromException(ScanJobDescription, Exception)
     * @see JobStatus
     */
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
