/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.*;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.orchestration.ScanJobConsumer;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.util.concurrent.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.mockito.*;

class WorkerTest {

    @Mock private WorkerCommandConfig commandConfig;
    @Mock private IOrchestrationProvider orchestrationProvider;
    @Mock private IPersistenceProvider persistenceProvider;
    @Mock private Future<Document> resultFuture;

    private Worker worker;
    private ArgumentCaptor<ScanJobConsumer> consumerCaptor;
    private ArgumentCaptor<ScanResult> scanResultCaptor;
    private ArgumentCaptor<ScanJobDescription> jobDescriptionCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up command config
        when(commandConfig.getParallelScanThreads()).thenReturn(2);
        when(commandConfig.getParallelConnectionThreads()).thenReturn(10);
        when(commandConfig.getScanTimeout()).thenReturn(60000); // 60 seconds

        // Initialize captors
        consumerCaptor = ArgumentCaptor.forClass(ScanJobConsumer.class);
        scanResultCaptor = ArgumentCaptor.forClass(ScanResult.class);
        jobDescriptionCaptor = ArgumentCaptor.forClass(ScanJobDescription.class);

        worker = new Worker(commandConfig, orchestrationProvider, persistenceProvider);
    }

    @AfterEach
    void tearDown() {
        // Clean up any resources if needed
    }

    @Test
    void testConstructor() {
        assertNotNull(worker);
        // Verify that config values were read
        verify(commandConfig).getParallelScanThreads();
        verify(commandConfig).getParallelConnectionThreads();
        verify(commandConfig).getScanTimeout();
    }

    @Test
    void testStart() {
        // When
        worker.start();

        // Then
        verify(orchestrationProvider).registerScanJobConsumer(consumerCaptor.capture(), eq(2));
        assertNotNull(consumerCaptor.getValue());
    }

    @Test
    void testHandleScanJobSuccess() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        Document resultDocument = new Document("result", "success");

        // Mock the static method call to BulkScanWorkerManager
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            when(resultFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(resultDocument);

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When - simulate receiving a scan job
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then
            verify(persistenceProvider)
                    .insertScanResult(scanResultCaptor.capture(), jobDescriptionCaptor.capture());

            ScanResult capturedResult = scanResultCaptor.getValue();
            assertEquals(JobStatus.SUCCESS, capturedResult.getResultStatus());
            assertEquals(resultDocument, capturedResult.getResult());

            verify(orchestrationProvider).notifyOfDoneScanJob(jobDescription);
        }
    }

    @Test
    void testHandleScanJobTimeout() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Mock the static method call
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            // First call throws TimeoutException
            when(resultFuture.get(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new TimeoutException("Scan timeout"))
                    .thenReturn(null);

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then
            verify(resultFuture).cancel(true);
            verify(persistenceProvider)
                    .insertScanResult(scanResultCaptor.capture(), jobDescriptionCaptor.capture());

            ScanResult capturedResult = scanResultCaptor.getValue();
            assertEquals(JobStatus.CANCELLED, capturedResult.getResultStatus());

            verify(orchestrationProvider).notifyOfDoneScanJob(jobDescription);
        }
    }

    @Test
    void testHandleScanJobExecutionException() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        Exception cause = new RuntimeException("Scan failed");

        // Mock the static method call
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            when(resultFuture.get(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new ExecutionException(cause));

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then
            verify(persistenceProvider)
                    .insertScanResult(scanResultCaptor.capture(), jobDescriptionCaptor.capture());

            ScanResult capturedResult = scanResultCaptor.getValue();
            assertEquals(JobStatus.ERROR, capturedResult.getResultStatus());
            assertNotNull(capturedResult.getResult());

            verify(orchestrationProvider).notifyOfDoneScanJob(jobDescription);
        }
    }

    @Test
    void testHandleScanJobInterruptedException() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Mock the static method call
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            when(resultFuture.get(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new InterruptedException("Worker interrupted"));

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then - should not persist on interrupt
            verify(persistenceProvider, never()).insertScanResult(any(), any());
            verify(orchestrationProvider, never()).notifyOfDoneScanJob(any());
            assertEquals(JobStatus.INTERNAL_ERROR, jobDescription.getStatus());
        }
    }

    @Test
    void testHandleScanJobUnexpectedException() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Mock the static method call
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            when(resultFuture.get(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then
            verify(persistenceProvider)
                    .insertScanResult(scanResultCaptor.capture(), jobDescriptionCaptor.capture());

            ScanResult capturedResult = scanResultCaptor.getValue();
            assertEquals(JobStatus.CRAWLER_ERROR, capturedResult.getResultStatus());

            verify(orchestrationProvider).notifyOfDoneScanJob(jobDescription);
        }
    }

    @Test
    void testHandleScanJobNullResult() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Mock the static method call
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            when(resultFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(null);

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then
            verify(persistenceProvider)
                    .insertScanResult(scanResultCaptor.capture(), jobDescriptionCaptor.capture());

            ScanResult capturedResult = scanResultCaptor.getValue();
            assertEquals(JobStatus.EMPTY, capturedResult.getResultStatus());

            verify(orchestrationProvider).notifyOfDoneScanJob(jobDescription);
        }
    }

    @Test
    void testPersistResultException() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        Document resultDocument = new Document("result", "success");

        // Mock persistence to throw exception
        doThrow(new RuntimeException("DB error"))
                .when(persistenceProvider)
                .insertScanResult(any(), any());

        // Mock the static method call
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            when(resultFuture.get(anyLong(), any(TimeUnit.class))).thenReturn(resultDocument);

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then - should still notify even if persist fails
            verify(orchestrationProvider).notifyOfDoneScanJob(jobDescription);
            assertEquals(JobStatus.INTERNAL_ERROR, jobDescription.getStatus());
        }
    }

    @Test
    void testTimeoutWithGracefulShutdownFailure() throws Exception {
        // Given
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScan bulkScan = createMockBulkScan();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Mock the static method call
        try (MockedStatic<BulkScanWorkerManager> mockedStatic =
                mockStatic(BulkScanWorkerManager.class)) {
            mockedStatic
                    .when(
                            () ->
                                    BulkScanWorkerManager.handleStatic(
                                            any(ScanJobDescription.class), anyInt(), anyInt()))
                    .thenReturn(resultFuture);

            // First call throws TimeoutException, second call also throws
            when(resultFuture.get(anyLong(), any(TimeUnit.class)))
                    .thenThrow(new TimeoutException("Scan timeout"))
                    .thenThrow(new TimeoutException("Graceful shutdown failed"));

            // Start worker and capture the consumer
            worker.start();
            verify(orchestrationProvider)
                    .registerScanJobConsumer(consumerCaptor.capture(), anyInt());
            ScanJobConsumer consumer = consumerCaptor.getValue();

            // When
            consumer.consumeScanJob(jobDescription);

            // Wait for async processing
            Thread.sleep(100);

            // Then
            verify(resultFuture, times(2)).cancel(true);
            verify(persistenceProvider)
                    .insertScanResult(scanResultCaptor.capture(), jobDescriptionCaptor.capture());

            ScanResult capturedResult = scanResultCaptor.getValue();
            assertEquals(JobStatus.CANCELLED, capturedResult.getResultStatus());

            verify(orchestrationProvider).notifyOfDoneScanJob(jobDescription);
        }
    }

    private BulkScan createMockBulkScan() {
        BulkScan bulkScan = mock(BulkScan.class);
        when(bulkScan.getName()).thenReturn("test-scan");
        when(bulkScan.getCollectionName()).thenReturn("test-collection");
        return bulkScan;
    }
}
