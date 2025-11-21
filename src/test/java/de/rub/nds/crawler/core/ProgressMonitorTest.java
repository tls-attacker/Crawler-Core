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

import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.test.TestScanConfig;
import de.rub.nds.scanner.core.config.ScannerDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

class ProgressMonitorTest {

    private ProgressMonitor progressMonitor;
    private IOrchestrationProvider orchestrationProvider;
    private IPersistenceProvider persistenceProvider;
    private Scheduler scheduler;
    private BulkScan bulkScan;
    private TestScanConfig scanConfig;

    @BeforeEach
    void setUp() throws SchedulerException {
        // Create test implementations of the providers
        orchestrationProvider = new TestOrchestrationProvider();
        persistenceProvider = new TestPersistenceProvider();
        scheduler = StdSchedulerFactory.getDefaultScheduler();

        progressMonitor =
                new ProgressMonitor(orchestrationProvider, persistenceProvider, scheduler);

        // Create a test bulk scan
        scanConfig = new TestScanConfig(ScannerDetail.ALL, 1, 5000);
        bulkScan =
                new BulkScan(
                        this.getClass(),
                        this.getClass(),
                        "TestScan",
                        scanConfig,
                        System.currentTimeMillis(),
                        true, // monitored
                        "http://example.com/notify");
        bulkScan.set_id("test-bulk-scan-id");
        bulkScan.setTargetsGiven(100);
        bulkScan.setScanJobsPublished(100);
    }

    @Test
    void testStartMonitoringBulkScanProgress() {
        // Should not throw any exceptions
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);
    }

    @Test
    void testStopMonitoringAndFinalizeBulkScan() {
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);
        // Should not throw any exceptions
        progressMonitor.stopMonitoringAndFinalizeBulkScan("test-bulk-scan-id");
    }

    @Test
    void testStartMonitoringBulkScanProgressForUnmonitoredScan() {
        // Create an unmonitored bulk scan
        BulkScan unmonitoredScan =
                new BulkScan(
                        this.getClass(),
                        this.getClass(),
                        "UnmonitoredScan",
                        scanConfig,
                        System.currentTimeMillis(),
                        false, // not monitored
                        null);
        unmonitoredScan.set_id("unmonitored-scan-id");

        // Should not start monitoring for unmonitored scans
        progressMonitor.startMonitoringBulkScanProgress(unmonitoredScan);
    }

    // Test implementation of IOrchestrationProvider
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

    // Test implementation of IPersistenceProvider
    private static class TestPersistenceProvider implements IPersistenceProvider {
        @Override
        public void insertScanResult(
                de.rub.nds.crawler.data.ScanResult scanResult, ScanJobDescription job) {}

        @Override
        public void insertBulkScan(BulkScan bulkScan) {}

        @Override
        public void updateBulkScan(BulkScan bulkScan) {}
    }
}
