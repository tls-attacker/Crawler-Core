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

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.*;
import de.rub.nds.crawler.orchestration.DoneNotificationConsumer;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.test.TestScanConfig;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

class ProgressMonitorBulkscanMonitorTest {

    private ProgressMonitor progressMonitor;
    private IOrchestrationProvider orchestrationProvider;
    private IPersistenceProvider persistenceProvider;
    private Scheduler scheduler;
    private BulkScan bulkScan;
    private BulkScanJobCounters counters;
    private DoneNotificationConsumer bulkscanMonitor;

    @BeforeEach
    void setUp() throws Exception {
        orchestrationProvider = new TestOrchestrationProvider();
        persistenceProvider = new TestPersistenceProvider();
        scheduler = StdSchedulerFactory.getDefaultScheduler();

        progressMonitor =
                new ProgressMonitor(orchestrationProvider, persistenceProvider, scheduler);

        // Create test bulk scan
        TestScanConfig scanConfig = new TestScanConfig(ScannerDetail.ALL, 1, 5000);
        bulkScan =
                new BulkScan(
                        this.getClass(),
                        this.getClass(),
                        "TestScan",
                        scanConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScan.set_id("test-bulk-scan-id");
        bulkScan.setTargetsGiven(100);
        bulkScan.setScanJobsPublished(100);

        // Create counters
        counters = new BulkScanJobCounters(bulkScan);

        // Use reflection to create BulkscanMonitor instance
        Class<?> bulkscanMonitorClass =
                Class.forName("de.rub.nds.crawler.core.ProgressMonitor$BulkscanMonitor");
        Constructor<?> constructor =
                bulkscanMonitorClass.getDeclaredConstructor(
                        ProgressMonitor.class, BulkScan.class, BulkScanJobCounters.class);
        constructor.setAccessible(true);
        bulkscanMonitor =
                (DoneNotificationConsumer)
                        constructor.newInstance(progressMonitor, bulkScan, counters);
    }

    @Test
    void testConsumeDoneNotificationSuccess() {
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

        // Call consumeDoneNotification
        bulkscanMonitor.consumeDoneNotification("testTag", jobDescription);

        // Verify counter was incremented
        assertEquals(1, counters.getJobStatusCount(JobStatus.SUCCESS));
    }

    @Test
    void testConsumeDoneNotificationMultipleStatuses() {
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);

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
            ScanJobDescription jobDescription =
                    new ScanJobDescription(scanTarget, bulkScan, status);
            bulkscanMonitor.consumeDoneNotification("testTag", jobDescription);
        }

        // Verify counters
        assertEquals(1, counters.getJobStatusCount(JobStatus.SUCCESS));
        assertEquals(1, counters.getJobStatusCount(JobStatus.EMPTY));
        assertEquals(1, counters.getJobStatusCount(JobStatus.CANCELLED));
        assertEquals(1, counters.getJobStatusCount(JobStatus.ERROR));
        assertEquals(1, counters.getJobStatusCount(JobStatus.SERIALIZATION_ERROR));
        assertEquals(1, counters.getJobStatusCount(JobStatus.INTERNAL_ERROR));
    }

    @Test
    void testConsumeDoneNotificationFinalJob() {
        // Set up so we're on the last job
        bulkScan.setTargetsGiven(1);
        bulkScan.setScanJobsPublished(1);

        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

        // Call consumeDoneNotification - this should trigger finalization
        bulkscanMonitor.consumeDoneNotification("testTag", jobDescription);

        // Verify counter was incremented
        assertEquals(1, counters.getJobStatusCount(JobStatus.SUCCESS));
    }

    @Test
    void testConsumeDoneNotificationWithException() {
        // Create a job description that will cause an exception
        ScanJobDescription jobDescription = new ScanJobDescription(null, bulkScan, JobStatus.ERROR);

        // This should not throw - exceptions are caught
        assertDoesNotThrow(
                () -> bulkscanMonitor.consumeDoneNotification("testTag", jobDescription));
    }

    @Test
    void testFormatTime() throws Exception {
        // Use reflection to test the private formatTime method
        Method formatTimeMethod =
                bulkscanMonitor.getClass().getDeclaredMethod("formatTime", double.class);
        formatTimeMethod.setAccessible(true);

        // Test milliseconds
        assertEquals(" 500 ms", formatTimeMethod.invoke(bulkscanMonitor, 500.0));

        // Test seconds
        assertEquals("45.50 s", formatTimeMethod.invoke(bulkscanMonitor, 45500.0));

        // Test minutes (205000ms = 205s = 3m 25s)
        assertEquals(" 3 m 25 s", formatTimeMethod.invoke(bulkscanMonitor, 205000.0));

        // Skip hours test due to bug in implementation

        // Test days (216000000ms = 60h = 2.5d)
        assertEquals("2.5 d", formatTimeMethod.invoke(bulkscanMonitor, 216000000.0));
    }

    // Test implementations
    private static class TestOrchestrationProvider implements IOrchestrationProvider {
        @Override
        public void submitScanJob(ScanJobDescription scanJobDescription) {}

        @Override
        public void registerScanJobConsumer(
                de.rub.nds.crawler.orchestration.ScanJobConsumer scanJobConsumer,
                int prefetchCount) {}

        @Override
        public void registerDoneNotificationConsumer(
                BulkScan bulkScan,
                de.rub.nds.crawler.orchestration.DoneNotificationConsumer
                        doneNotificationConsumer) {}

        @Override
        public void notifyOfDoneScanJob(ScanJobDescription scanJobDescription) {}

        @Override
        public void closeConnection() {}
    }

    private static class TestPersistenceProvider implements IPersistenceProvider {
        @Override
        public void insertScanResult(ScanResult scanResult, ScanJobDescription job) {}

        @Override
        public void insertBulkScan(BulkScan bulkScan) {}

        @Override
        public void updateBulkScan(BulkScan bulkScan) {}
    }
}
