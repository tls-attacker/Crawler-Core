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
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.BulkScanJobCounters;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

class ProgressMonitorTest {

    @Mock private IPersistenceProvider persistenceProvider;

    @Mock private HttpURLConnection mockConnection;

    private ProgressMonitor progressMonitor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        progressMonitor = new ProgressMonitor(persistenceProvider);
    }

    @Test
    void testStartMonitoringBulkScanProgress() {
        BulkScan bulkScan = createTestBulkScan(100);

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Verify monitor was created
        assertTrue(progressMonitor.bulkscanMonitors.containsKey(bulkScan.getId()));

        ProgressMonitor.BulkscanMonitor monitor =
                progressMonitor.bulkscanMonitors.get(bulkScan.getId());
        assertNotNull(monitor);
        assertEquals(100, monitor.jobTotal);
        assertEquals(0, monitor.jobsSuccess);
        assertEquals(0, monitor.jobsTimeout);
        assertEquals(0, monitor.jobsError);
    }

    @Test
    void testConsumeDoneNotificationSuccess() {
        BulkScan bulkScan = createTestBulkScan(10);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        ScanResult result = createScanResult(JobStatus.SUCCESS);
        progressMonitor.consumeDoneNotification(bulkScan, result);

        ProgressMonitor.BulkscanMonitor monitor =
                progressMonitor.bulkscanMonitors.get(bulkScan.getId());
        assertEquals(1, monitor.jobsSuccess);
        assertEquals(0, monitor.jobsTimeout);
        assertEquals(0, monitor.jobsError);
    }

    @Test
    void testConsumeDoneNotificationTimeout() {
        BulkScan bulkScan = createTestBulkScan(10);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        ScanResult result = createScanResult(JobStatus.TIMEOUT);
        progressMonitor.consumeDoneNotification(bulkScan, result);

        ProgressMonitor.BulkscanMonitor monitor =
                progressMonitor.bulkscanMonitors.get(bulkScan.getId());
        assertEquals(0, monitor.jobsSuccess);
        assertEquals(1, monitor.jobsTimeout);
        assertEquals(0, monitor.jobsError);
    }

    @Test
    void testConsumeDoneNotificationError() {
        BulkScan bulkScan = createTestBulkScan(10);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        ScanResult result = createScanResult(JobStatus.ERROR);
        progressMonitor.consumeDoneNotification(bulkScan, result);

        ProgressMonitor.BulkscanMonitor monitor =
                progressMonitor.bulkscanMonitors.get(bulkScan.getId());
        assertEquals(0, monitor.jobsSuccess);
        assertEquals(0, monitor.jobsTimeout);
        assertEquals(1, monitor.jobsError);
    }

    @Test
    void testCompletionDetection() {
        BulkScan bulkScan = createTestBulkScan(3);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Complete all jobs
        progressMonitor.consumeDoneNotification(bulkScan, createScanResult(JobStatus.SUCCESS));
        progressMonitor.consumeDoneNotification(bulkScan, createScanResult(JobStatus.TIMEOUT));
        progressMonitor.consumeDoneNotification(bulkScan, createScanResult(JobStatus.ERROR));

        // Monitor should be removed when all jobs complete
        assertFalse(progressMonitor.bulkscanMonitors.containsKey(bulkScan.getId()));

        // Verify bulk scan was updated
        ArgumentCaptor<BulkScan> captor = ArgumentCaptor.forClass(BulkScan.class);
        verify(persistenceProvider).updateBulkScan(captor.capture());

        BulkScan updatedBulkScan = captor.getValue();
        assertNotNull(updatedBulkScan.getEndTime());
        assertEquals(1, updatedBulkScan.getCounters().getSuccess());
        assertEquals(1, updatedBulkScan.getCounters().getTimeout());
        assertEquals(1, updatedBulkScan.getCounters().getError());
    }

    @Test
    void testStopMonitoringAndFinalizeBulkScan() {
        BulkScan bulkScan = createTestBulkScan(10);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Add some completed jobs
        progressMonitor.consumeDoneNotification(bulkScan, createScanResult(JobStatus.SUCCESS));
        progressMonitor.consumeDoneNotification(bulkScan, createScanResult(JobStatus.SUCCESS));

        progressMonitor.stopMonitoringAndFinalizeBulkScan(bulkScan);

        // Monitor should be removed
        assertFalse(progressMonitor.bulkscanMonitors.containsKey(bulkScan.getId()));

        // Verify bulk scan was updated
        verify(persistenceProvider).updateBulkScan(any(BulkScan.class));
    }

    @Test
    void testNotifyWithValidUrl() throws IOException {
        String notifyUrl = "http://example.com/notify";

        try (MockedStatic<URL> urlMock = mockStatic(URL.class)) {
            URL mockUrl = mock(URL.class);
            when(mockUrl.openConnection()).thenReturn(mockConnection);
            urlMock.when(() -> new URL(notifyUrl)).thenReturn(mockUrl);

            when(mockConnection.getResponseCode()).thenReturn(200);

            progressMonitor.notify(notifyUrl);

            verify(mockConnection).setRequestMethod("POST");
            verify(mockConnection).setDoOutput(true);
            verify(mockConnection).connect();
        }
    }

    @Test
    void testNotifyWithNullUrl() {
        // Should not throw exception
        assertDoesNotThrow(() -> progressMonitor.notify(null));
    }

    @Test
    void testNotifyWithEmptyUrl() {
        // Should not throw exception
        assertDoesNotThrow(() -> progressMonitor.notify(""));
    }

    @Test
    void testETACalculation() {
        BulkScan bulkScan = createTestBulkScan(100);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        ProgressMonitor.BulkscanMonitor monitor =
                progressMonitor.bulkscanMonitors.get(bulkScan.getId());

        // Simulate completing jobs over time
        for (int i = 0; i < 10; i++) {
            progressMonitor.consumeDoneNotification(bulkScan, createScanResult(JobStatus.SUCCESS));
            try {
                Thread.sleep(10); // Small delay to ensure time difference
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // ETA should be calculated based on average completion time
        assertTrue(monitor.movingAverage > 0);
    }

    @Test
    void testFormatTime() {
        // Test private formatTime method indirectly through ETA calculation
        BulkScan bulkScan = createTestBulkScan(1);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        progressMonitor.consumeDoneNotification(bulkScan, createScanResult(JobStatus.SUCCESS));

        // This will trigger formatTime internally
        assertFalse(progressMonitor.bulkscanMonitors.containsKey(bulkScan.getId()));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        BulkScan bulkScan = createTestBulkScan(100);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // Create multiple threads that consume notifications concurrently
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] =
                    new Thread(
                            () -> {
                                for (int j = 0; j < 10; j++) {
                                    progressMonitor.consumeDoneNotification(
                                            bulkScan, createScanResult(JobStatus.SUCCESS));
                                }
                            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // All jobs should be completed
        assertFalse(progressMonitor.bulkscanMonitors.containsKey(bulkScan.getId()));
    }

    private BulkScan createTestBulkScan(int jobTotal) {
        BulkScan bulkScan = new BulkScan();
        bulkScan.setId("test-bulk-scan-" + System.currentTimeMillis());
        bulkScan.setScanConfig(new ScanConfig(ScannerDetail.NORMAL, 1, 1));
        bulkScan.setStartTime(ZonedDateTime.now());
        bulkScan.setJobTotal(jobTotal);
        bulkScan.setCounters(new BulkScanJobCounters());
        return bulkScan;
    }

    private ScanResult createScanResult(JobStatus status) {
        ScanTarget target = new ScanTarget("example.com", 443);
        Map<String, Object> details = new HashMap<>();
        details.put("status", status);
        return new ScanResult(target, ZonedDateTime.now(), status, details);
    }
}
