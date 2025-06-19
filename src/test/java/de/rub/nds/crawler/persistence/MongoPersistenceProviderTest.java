/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.mongodb.client.*;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mongojack.JacksonMongoCollection;

class MongoPersistenceProviderTest {

    @Mock private MongoDbDelegate mongoDbDelegate;

    @Mock private MongoClient mongoClient;

    @Mock private ClientSession clientSession;

    @Mock private MongoDatabase mongoDatabase;

    @Mock private MongoCollection<ScanResult> scanResultCollection;

    @Mock private MongoCollection<BulkScan> bulkScanCollection;

    @Mock private JacksonMongoCollection<ScanResult> jacksonScanResultCollection;

    @Mock private JacksonMongoCollection<BulkScan> jacksonBulkScanCollection;

    @Mock private JsonSerializer<?> mockSerializer;

    @Mock private Module mockModule;

    @TempDir Path tempDir;

    private MongoPersistenceProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Clear static state
        Field isInitializedField = MongoPersistenceProvider.class.getDeclaredField("isInitialized");
        isInitializedField.setAccessible(true);
        isInitializedField.set(null, false);

        Field serializersField = MongoPersistenceProvider.class.getDeclaredField("serializers");
        serializersField.setAccessible(true);
        ((java.util.Set<?>) serializersField.get(null)).clear();

        Field modulesField = MongoPersistenceProvider.class.getDeclaredField("modules");
        modulesField.setAccessible(true);
        ((java.util.Set<?>) modulesField.get(null)).clear();

        when(mongoDbDelegate.getMongoDbHost()).thenReturn("localhost");
        when(mongoDbDelegate.getMongoDbPort()).thenReturn(27017);
        when(mongoDbDelegate.getMongoDbUser()).thenReturn("user");
        when(mongoDbDelegate.getMongoDbPass()).thenReturn("pass");
        when(mongoDbDelegate.getMongoDbAuthSource()).thenReturn("admin");
    }

    @AfterEach
    void tearDown() throws Exception {
        // Reset static state
        Field isInitializedField = MongoPersistenceProvider.class.getDeclaredField("isInitialized");
        isInitializedField.setAccessible(true);
        isInitializedField.set(null, false);
    }

    private BulkScan createTestBulkScan(String id, String name) {
        de.rub.nds.crawler.data.ScanConfig scanConfig =
                new de.rub.nds.crawler.data.ScanConfig(ScannerDetail.NORMAL, 3, 2000) {
                    @Override
                    public BulkScanWorker<? extends de.rub.nds.crawler.data.ScanConfig>
                            createWorker(
                                    String bulkScanID,
                                    int parallelConnectionThreads,
                                    int parallelScanThreads) {
                        return null;
                    }
                };
        BulkScan bulkScan =
                new BulkScan(
                        MongoPersistenceProviderTest.class,
                        MongoPersistenceProviderTest.class,
                        name,
                        scanConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScan.set_id(id);
        return bulkScan;
    }

    @Test
    void testRegisterSerializer() {
        assertDoesNotThrow(() -> MongoPersistenceProvider.registerSerializer(mockSerializer));
    }

    @Test
    void testRegisterSerializerAfterInitialization() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {
            mockedMongoClients
                    .when(() -> MongoClients.create((String) any()))
                    .thenReturn(mongoClient);
            when(mongoClient.startSession()).thenReturn(clientSession);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            assertThrows(
                    RuntimeException.class,
                    () -> MongoPersistenceProvider.registerSerializer(mockSerializer));
        }
    }

    @Test
    void testRegisterMultipleSerializers() {
        JsonSerializer<?> serializer1 = mock(JsonSerializer.class);
        JsonSerializer<?> serializer2 = mock(JsonSerializer.class);
        assertDoesNotThrow(
                () -> MongoPersistenceProvider.registerSerializer(serializer1, serializer2));
    }

    @Test
    void testRegisterModule() {
        assertDoesNotThrow(() -> MongoPersistenceProvider.registerModule(mockModule));
    }

    @Test
    void testRegisterModuleAfterInitialization() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {
            mockedMongoClients
                    .when(() -> MongoClients.create((String) any()))
                    .thenReturn(mongoClient);
            when(mongoClient.startSession()).thenReturn(clientSession);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            assertThrows(
                    RuntimeException.class,
                    () -> MongoPersistenceProvider.registerModule(mockModule));
        }
    }

    @Test
    void testRegisterMultipleModules() {
        Module module1 = mock(Module.class);
        Module module2 = mock(Module.class);
        assertDoesNotThrow(() -> MongoPersistenceProvider.registerModule(module1, module2));
    }

    @Test
    void testConstructorWithPassword() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {
            mockedMongoClients
                    .when(() -> MongoClients.create((String) any()))
                    .thenReturn(mongoClient);
            when(mongoClient.startSession()).thenReturn(clientSession);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            assertNotNull(provider);
        }
    }

    @Test
    void testConstructorWithPasswordFile() throws IOException {
        Path passFile = tempDir.resolve("password.txt");
        Files.write(passFile, "filepass".getBytes());

        when(mongoDbDelegate.getMongoDbPass()).thenReturn(null);
        when(mongoDbDelegate.getMongoDbPassFile()).thenReturn(passFile.toString());

        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {
            mockedMongoClients
                    .when(() -> MongoClients.create((String) any()))
                    .thenReturn(mongoClient);
            when(mongoClient.startSession()).thenReturn(clientSession);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            assertNotNull(provider);
        }
    }

    @Test
    void testConstructorConnectionException() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {
            mockedMongoClients
                    .when(() -> MongoClients.create((String) any()))
                    .thenReturn(mongoClient);
            when(mongoClient.startSession()).thenThrow(new RuntimeException("Connection failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> provider = new MongoPersistenceProvider(mongoDbDelegate));
        }
    }

    @Test
    void testInsertBulkScan() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");

            provider.insertBulkScan(bulkScan);

            verify(jacksonBulkScanCollection).insertOne(bulkScan);
        }
    }

    @Test
    void testInsertBulkScanNull() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            assertThrows(NullPointerException.class, () -> provider.insertBulkScan(null));
        }
    }

    @Test
    void testUpdateBulkScan() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");

            provider.updateBulkScan(bulkScan);

            verify(jacksonBulkScanCollection).removeById("test-id");
            verify(jacksonBulkScanCollection).insertOne(bulkScan);
        }
    }

    @Test
    void testInsertScanResult() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            ScanTarget scanTarget = new ScanTarget();
            scanTarget.setHostname("example.com");

            BulkScan bulkScan = createTestBulkScan("bulk-id", "test-db");
            ScanJobDescription scanJob =
                    new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

            org.bson.Document resultDoc = new org.bson.Document();
            ScanResult scanResult = new ScanResult(scanJob, resultDoc);

            provider.insertScanResult(scanResult, scanJob);

            verify(jacksonScanResultCollection).insertOne(scanResult);
        }
    }

    @Test
    void testInsertScanResultStatusMismatch() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            ScanTarget scanTarget = new ScanTarget();
            scanTarget.setHostname("example.com");

            BulkScan bulkScan = createTestBulkScan("bulk-id", "test-db");
            ScanJobDescription scanJob =
                    new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

            // Create a result with different status
            org.bson.Document resultDoc = new org.bson.Document();
            ScanJobDescription wrongStatusJob =
                    new ScanJobDescription(scanTarget, bulkScan, JobStatus.ERROR);
            ScanResult scanResult = new ScanResult(wrongStatusJob, resultDoc);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> provider.insertScanResult(scanResult, scanJob));
        }
    }

    @Test
    void testInsertScanResultWithException() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            ScanTarget scanTarget = new ScanTarget();
            scanTarget.setHostname("example.com");

            BulkScan bulkScan = createTestBulkScan("bulk-id", "test-db");
            ScanJobDescription scanJob =
                    new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

            org.bson.Document resultDoc = new org.bson.Document();
            ScanResult scanResult = new ScanResult(scanJob, resultDoc);

            // First insertion throws exception
            doThrow(new RuntimeException("Serialization error"))
                    .doNothing()
                    .when(jacksonScanResultCollection)
                    .insertOne(any());

            provider.insertScanResult(scanResult, scanJob);

            // Should insert twice - once with original result (fails), once with error result
            verify(jacksonScanResultCollection, times(2)).insertOne(any());
            assertEquals(JobStatus.SERIALIZATION_ERROR, scanJob.getStatus());
        }
    }

    @Test
    void testInsertScanResultWithSerializationErrorRecursion() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            ScanTarget scanTarget = new ScanTarget();
            scanTarget.setHostname("example.com");

            BulkScan bulkScan = createTestBulkScan("bulk-id", "test-db");
            ScanJobDescription scanJob =
                    new ScanJobDescription(scanTarget, bulkScan, JobStatus.SERIALIZATION_ERROR);

            org.bson.Document resultDoc = new org.bson.Document();
            ScanResult scanResult = new ScanResult(scanJob, resultDoc);

            // Always throw exception
            doThrow(new RuntimeException("Serialization error"))
                    .when(jacksonScanResultCollection)
                    .insertOne(any());

            provider.insertScanResult(scanResult, scanJob);

            // Should only try once to avoid infinite recursion
            verify(jacksonScanResultCollection, times(1)).insertOne(any());
            assertEquals(JobStatus.INTERNAL_ERROR, scanJob.getStatus());
        }
    }

    @Test
    void testInitDatabaseCaching() throws Exception {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            BulkScan bulkScan1 = createTestBulkScan("id1", "test-db");
            BulkScan bulkScan2 = createTestBulkScan("id2", "test-db"); // Same DB name

            provider.insertBulkScan(bulkScan1);
            provider.insertBulkScan(bulkScan2);

            // Database should only be initialized once due to caching
            verify(mongoClient, times(1)).getDatabase("test-db");
        }
    }

    @Test
    void testResultCollectionIndexCreation() {
        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class);
                MockedStatic<JacksonMongoCollection> mockedJackson =
                        mockStatic(JacksonMongoCollection.class)) {

            setupMocks(mockedMongoClients, mockedJackson);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            ScanTarget scanTarget = new ScanTarget();
            scanTarget.setHostname("example.com");

            BulkScan bulkScan = createTestBulkScan("bulk-id", "test-db");
            ScanJobDescription scanJob =
                    new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

            org.bson.Document resultDoc = new org.bson.Document();
            ScanResult scanResult = new ScanResult(scanJob, resultDoc);

            provider.insertScanResult(scanResult, scanJob);

            // Verify indexes are created
            verify(jacksonScanResultCollection, times(4)).createIndex(any(Bson.class));
        }
    }

    @Test
    void testWithRegisteredSerializersAndModules() {
        // Register serializers and modules before initialization
        MongoPersistenceProvider.registerSerializer(mockSerializer);
        MongoPersistenceProvider.registerModule(mockModule);

        try (MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {
            mockedMongoClients
                    .when(() -> MongoClients.create((String) any()))
                    .thenReturn(mongoClient);
            when(mongoClient.startSession()).thenReturn(clientSession);

            provider = new MongoPersistenceProvider(mongoDbDelegate);

            assertNotNull(provider);
        }
    }

    private void setupMocks(
            MockedStatic<MongoClients> mockedMongoClients,
            MockedStatic<JacksonMongoCollection> mockedJackson) {
        mockedMongoClients.when(() -> MongoClients.create((String) any())).thenReturn(mongoClient);
        when(mongoClient.startSession()).thenReturn(clientSession);
        when(mongoClient.getDatabase(anyString())).thenReturn(mongoDatabase);

        JacksonMongoCollection.JacksonMongoCollectionBuilder resultBuilder =
                mock(JacksonMongoCollection.JacksonMongoCollectionBuilder.class);
        JacksonMongoCollection.JacksonMongoCollectionBuilder bulkScanBuilder =
                mock(JacksonMongoCollection.JacksonMongoCollectionBuilder.class);

        mockedJackson
                .when(JacksonMongoCollection::builder)
                .thenReturn(resultBuilder, bulkScanBuilder);

        when(resultBuilder.withObjectMapper(any())).thenReturn(resultBuilder);
        when(resultBuilder.build(
                        any(MongoDatabase.class), anyString(), eq(ScanResult.class), any()))
                .thenReturn(jacksonScanResultCollection);

        when(bulkScanBuilder.withObjectMapper(any())).thenReturn(bulkScanBuilder);
        when(bulkScanBuilder.build(
                        any(MongoDatabase.class), anyString(), eq(BulkScan.class), any()))
                .thenReturn(jacksonBulkScanCollection);

        when(jacksonScanResultCollection.createIndex(any(Bson.class))).thenReturn("index");
    }
}
