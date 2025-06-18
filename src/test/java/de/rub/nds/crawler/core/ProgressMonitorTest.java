/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.orchestration.DoneNotificationConsumer;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

class ProgressMonitorTest {

    @Mock private IOrchestrationProvider orchestrationProvider;

    @Mock private IPersistenceProvider persistenceProvider;

    @Mock private Scheduler scheduler;

    @Mock private HttpClient httpClient;

    @Mock private HttpResponse<String> httpResponse;

    private ProgressMonitor progressMonitor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        progressMonitor =
                new ProgressMonitor(orchestrationProvider, persistenceProvider, scheduler);
    }

    private BulkScan createTestBulkScan(String id, String name) {
        ScanConfig scanConfig =
                new ScanConfig(ScannerDetail.NORMAL, 3, 2000) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads) {
                        return null;
                    }
                };
        BulkScan bulkScan =
                new BulkScan(
                        ProgressMonitorTest.class,
                        ProgressMonitorTest.class,
                        name,
                        scanConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScan.set_id(id);
        return bulkScan;
    }

    private ScanJobDescription createTestScanJob(BulkScan bulkScan, JobStatus status) {
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("192.0.2.1");
        return new ScanJobDescription(target, bulkScan, status);
    }

    @Test
    void testStartMonitoringBulkScanProgress() throws Exception {
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Verify that done notification consumer is registered
        verify(orchestrationProvider)
                .registerDoneNotificationConsumer(
                        eq(bulkScan), any(DoneNotificationConsumer.class));

        // Check that bulk scan is tracked internally
        Field scanJobDetailsByIdField =
                ProgressMonitor.class.getDeclaredField("scanJobDetailsById");
        scanJobDetailsByIdField.setAccessible(true);
        Map<String, ?> scanJobDetailsById =
                (Map<String, ?>) scanJobDetailsByIdField.get(progressMonitor);
        assertTrue(scanJobDetailsById.containsKey("test-id"));
    }

    @Test
    void testStartMonitoringMultipleBulkScans() throws Exception {
        BulkScan bulkScan1 = createTestBulkScan("test-id-1", "test-scan-1");
        BulkScan bulkScan2 = createTestBulkScan("test-id-2", "test-scan-2");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan1);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan2);

        // Should only register listener once
        verify(orchestrationProvider, times(1))
                .registerDoneNotificationConsumer(any(), any(DoneNotificationConsumer.class));

        // Check that both bulk scans are tracked
        Field scanJobDetailsByIdField =
                ProgressMonitor.class.getDeclaredField("scanJobDetailsById");
        scanJobDetailsByIdField.setAccessible(true);
        Map<String, ?> scanJobDetailsById =
                (Map<String, ?>) scanJobDetailsByIdField.get(progressMonitor);
        assertEquals(2, scanJobDetailsById.size());
        assertTrue(scanJobDetailsById.containsKey("test-id-1"));
        assertTrue(scanJobDetailsById.containsKey("test-id-2"));
    }

    @Test
    void testStopMonitoringAndFinalizeBulkScanWithoutNotification() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Execute
        progressMonitor.stopMonitoringAndFinalizeBulkScan("test-id");

        // Verify
        verify(persistenceProvider)
                .updateBulkScan(
                        argThat(
                                scan -> {
                                    return scan.isFinished() && scan.getEndTime() > 0;
                                }));

        // Check that bulk scan is removed from tracking
        Field scanJobDetailsByIdField =
                ProgressMonitor.class.getDeclaredField("scanJobDetailsById");
        scanJobDetailsByIdField.setAccessible(true);
        Map<String, ?> scanJobDetailsById =
                (Map<String, ?>) scanJobDetailsByIdField.get(progressMonitor);
        assertFalse(scanJobDetailsById.containsKey("test-id"));
    }

    @Test
    void testStopMonitoringAndFinalizeBulkScanWithNotification() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setNotifyUrl("http://example.com/notify");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(httpResponse);
            when(httpResponse.body()).thenReturn("OK");

            // Execute
            progressMonitor.stopMonitoringAndFinalizeBulkScan("test-id");

            // Verify notification was sent
            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(httpClient).send(requestCaptor.capture(), any());

            HttpRequest request = requestCaptor.getValue();
            assertEquals("http://example.com/notify", request.uri().toString());
            assertEquals("POST", request.method());
        }

        verify(persistenceProvider).updateBulkScan(any());
    }

    @Test
    void testStopMonitoringWithSchedulerShutdown() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);
        when(scheduler.isShutdown()).thenReturn(true);

        // Execute
        progressMonitor.stopMonitoringAndFinalizeBulkScan("test-id");

        // Verify
        verify(orchestrationProvider).closeConnection();
    }

    @Test
    void testStopMonitoringWithSchedulerException() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);
        when(scheduler.isShutdown()).thenThrow(new SchedulerException("Test exception"));

        // Execute - should not throw
        assertDoesNotThrow(() -> progressMonitor.stopMonitoringAndFinalizeBulkScan("test-id"));
    }

    @Test
    void testNotifyIOException() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setNotifyUrl("http://example.com/notify");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            when(httpClient.send(any(HttpRequest.class), any()))
                    .thenThrow(new IOException("Network error"));

            // Execute - should not throw
            assertDoesNotThrow(() -> progressMonitor.stopMonitoringAndFinalizeBulkScan("test-id"));
        }

        verify(persistenceProvider).updateBulkScan(any());
    }

    @Test
    void testNotifyInterruptedException() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setNotifyUrl("http://example.com/notify");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        try (MockedStatic<HttpClient> mockedHttpClient = mockStatic(HttpClient.class)) {
            mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(httpClient);
            when(httpClient.send(any(HttpRequest.class), any()))
                    .thenThrow(new InterruptedException("Interrupted"));

            // Execute - should not throw
            assertDoesNotThrow(() -> progressMonitor.stopMonitoringAndFinalizeBulkScan("test-id"));

            // Verify thread interrupt flag is set
            assertTrue(Thread.currentThread().isInterrupted());
            // Clear interrupt flag
            Thread.interrupted();
        }

        verify(persistenceProvider).updateBulkScan(any());
    }

    @Test
    void testBulkScanMonitorConsumeDoneNotification() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setStartTime(System.currentTimeMillis());
        bulkScan.setScanJobsPublished(10);

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Get the registered consumer
        ArgumentCaptor<DoneNotificationConsumer> consumerCaptor =
                ArgumentCaptor.forClass(DoneNotificationConsumer.class);
        verify(orchestrationProvider)
                .registerDoneNotificationConsumer(eq(bulkScan), consumerCaptor.capture());
        DoneNotificationConsumer consumer = consumerCaptor.getValue();

        // Create scan job
        ScanJobDescription scanJob = createTestScanJob(bulkScan, JobStatus.SUCCESS);

        // Consume notification
        consumer.consumeDoneNotification("test-tag", scanJob);

        // Should not finalize yet (1 of 10 done)
        verify(persistenceProvider, never()).updateBulkScan(any());
    }

    @Test
    void testBulkScanMonitorCompleteAllJobs() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setStartTime(System.currentTimeMillis());
        bulkScan.setScanJobsPublished(2);

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Get the registered consumer
        ArgumentCaptor<DoneNotificationConsumer> consumerCaptor =
                ArgumentCaptor.forClass(DoneNotificationConsumer.class);
        verify(orchestrationProvider)
                .registerDoneNotificationConsumer(eq(bulkScan), consumerCaptor.capture());
        DoneNotificationConsumer consumer = consumerCaptor.getValue();

        // Create and consume first job
        ScanJobDescription scanJob1 = createTestScanJob(bulkScan, JobStatus.SUCCESS);
        consumer.consumeDoneNotification("test-tag", scanJob1);

        // Create and consume second job (should trigger completion)
        ScanJobDescription scanJob2 = createTestScanJob(bulkScan, JobStatus.CANCELLED);
        consumer.consumeDoneNotification("test-tag", scanJob2);

        // Should finalize bulk scan
        verify(persistenceProvider)
                .updateBulkScan(
                        argThat(
                                scan -> {
                                    return scan.isFinished() && scan.getSuccessfulScans() == 1;
                                }));
    }

    @Test
    void testBulkScanMonitorWithException() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setStartTime(System.currentTimeMillis());
        bulkScan.setScanJobsPublished(1);

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Get the registered consumer
        ArgumentCaptor<DoneNotificationConsumer> consumerCaptor =
                ArgumentCaptor.forClass(DoneNotificationConsumer.class);
        verify(orchestrationProvider)
                .registerDoneNotificationConsumer(eq(bulkScan), consumerCaptor.capture());
        DoneNotificationConsumer consumer = consumerCaptor.getValue();

        // Create scan job with null bulk scan info to trigger exception
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        ScanJobDescription scanJob =
                new ScanJobDescription(
                        target, null, "test-db", "test-collection", JobStatus.SUCCESS);

        // Should not throw
        assertDoesNotThrow(() -> consumer.consumeDoneNotification("test-tag", scanJob));
    }

    @Test
    void testFormatTime() throws Exception {
        // Get access to private formatTime method
        Method formatTimeMethod = null;
        for (Class<?> innerClass : ProgressMonitor.class.getDeclaredClasses()) {
            if (innerClass.getSimpleName().equals("BulkscanMonitor")) {
                formatTimeMethod = innerClass.getDeclaredMethod("formatTime", double.class);
                formatTimeMethod.setAccessible(true);
                break;
            }
        }
        assertNotNull(formatTimeMethod);

        // Create instance of inner class
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        ArgumentCaptor<DoneNotificationConsumer> consumerCaptor =
                ArgumentCaptor.forClass(DoneNotificationConsumer.class);
        verify(orchestrationProvider)
                .registerDoneNotificationConsumer(eq(bulkScan), consumerCaptor.capture());
        Object bulkScanMonitor = consumerCaptor.getValue();

        // Test different time formats
        assertEquals(" 500 ms", formatTimeMethod.invoke(bulkScanMonitor, 500.0));
        assertEquals(" 5.00 s", formatTimeMethod.invoke(bulkScanMonitor, 5000.0));
        assertEquals("90.00 s", formatTimeMethod.invoke(bulkScanMonitor, 90000.0));
        assertEquals(" 3 h 30 m", formatTimeMethod.invoke(bulkScanMonitor, 9000000.0));
        assertEquals("2.1 d", formatTimeMethod.invoke(bulkScanMonitor, 180000000.0));
    }

    @Test
    void testBulkScanMonitorUsesTargetsGivenWhenNoJobsPublished() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setStartTime(System.currentTimeMillis());
        bulkScan.setScanJobsPublished(0); // No jobs published
        bulkScan.setTargetsGiven(5); // But targets given

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Get the registered consumer
        ArgumentCaptor<DoneNotificationConsumer> consumerCaptor =
                ArgumentCaptor.forClass(DoneNotificationConsumer.class);
        verify(orchestrationProvider)
                .registerDoneNotificationConsumer(eq(bulkScan), consumerCaptor.capture());
        DoneNotificationConsumer consumer = consumerCaptor.getValue();

        // Consume notifications for all 5 targets
        for (int i = 0; i < 5; i++) {
            ScanJobDescription scanJob = createTestScanJob(bulkScan, JobStatus.SUCCESS);
            consumer.consumeDoneNotification("test-tag", scanJob);
        }

        // Should finalize after 5 jobs (using targetsGiven)
        verify(persistenceProvider).updateBulkScan(any());
    }

    @Test
    void testDifferentJobStatuses() throws Exception {
        // Setup
        BulkScan bulkScan = createTestBulkScan("test-id", "test-scan");
        bulkScan.setStartTime(System.currentTimeMillis());
        bulkScan.setScanJobsPublished(6);

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Get the registered consumer
        ArgumentCaptor<DoneNotificationConsumer> consumerCaptor =
                ArgumentCaptor.forClass(DoneNotificationConsumer.class);
        verify(orchestrationProvider)
                .registerDoneNotificationConsumer(eq(bulkScan), consumerCaptor.capture());
        DoneNotificationConsumer consumer = consumerCaptor.getValue();

        // Test different job statuses
        JobStatus[] statuses = {
            JobStatus.SUCCESS,
            JobStatus.EMPTY,
            JobStatus.CANCELLED,
            JobStatus.ERROR,
            JobStatus.SERIALIZATION_ERROR,
            JobStatus.INTERNAL_ERROR
        };

        for (JobStatus status : statuses) {
            ScanJobDescription scanJob = createTestScanJob(bulkScan, status);
            consumer.consumeDoneNotification("test-tag", scanJob);
        }

        // Verify the bulk scan has correct counters
        ArgumentCaptor<BulkScan> bulkScanCaptor = ArgumentCaptor.forClass(BulkScan.class);
        verify(persistenceProvider).updateBulkScan(bulkScanCaptor.capture());

        BulkScan finalizedBulkScan = bulkScanCaptor.getValue();
        assertEquals(1, finalizedBulkScan.getSuccessfulScans());
        assertNotNull(finalizedBulkScan.getJobStatusCounters());
    }
}
