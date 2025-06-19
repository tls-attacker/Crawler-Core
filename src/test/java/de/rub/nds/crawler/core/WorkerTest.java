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

import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.*;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.orchestration.ScanJobConsumer;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.test.TestScanConfig;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkerTest {

    private Worker worker;
    private WorkerCommandConfig commandConfig;
    private TestOrchestrationProvider orchestrationProvider;
    private TestPersistenceProvider persistenceProvider;

    @BeforeEach
    void setUp() {
        commandConfig = new WorkerCommandConfig();
        commandConfig.setParallelScanThreads(2);
        commandConfig.setParallelConnectionThreads(4);
        commandConfig.setScanTimeout(5000);

        orchestrationProvider = new TestOrchestrationProvider();
        persistenceProvider = new TestPersistenceProvider();

        worker = new Worker(commandConfig, orchestrationProvider, persistenceProvider);
    }

    @Test
    void testConstructor() {
        assertNotNull(worker);
    }

    @Test
    void testStart() {
        worker.start();
        assertTrue(orchestrationProvider.isConsumerRegistered());
        assertEquals(2, orchestrationProvider.getPrefetchCount());
    }

    @Test
    void testGettersFromConfig() {
        // Test that Worker properly uses the configuration
        assertEquals(2, commandConfig.getParallelScanThreads());
        assertEquals(4, commandConfig.getParallelConnectionThreads());
        assertEquals(5000, commandConfig.getScanTimeout());
    }

    @Test
    void testHandleScanJob() throws InterruptedException {
        // Create test data
        TestScanConfig scanConfig = new TestScanConfig(ScannerDetail.ALL, 1, 5000);
        BulkScan bulkScan =
                new BulkScan(
                        this.getClass(),
                        this.getClass(),
                        "TestScan",
                        scanConfig,
                        System.currentTimeMillis(),
                        false,
                        null);
        bulkScan.set_id("test-bulk-scan");

        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Set up a latch to wait for job processing
        CountDownLatch latch = new CountDownLatch(1);
        persistenceProvider.setLatch(latch);

        // Start the worker
        worker.start();

        // Submit a job
        orchestrationProvider.submitJob(jobDescription);

        // Wait for the job to be processed
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Job should be processed within 10 seconds");

        // Verify the result was persisted
        assertTrue(persistenceProvider.hasReceivedScanResult());
    }

    // Test implementation of IOrchestrationProvider
    private static class TestOrchestrationProvider implements IOrchestrationProvider {
        private ScanJobConsumer consumer;
        private int prefetchCount;

        @Override
        public void submitScanJob(ScanJobDescription scanJobDescription) {}

        @Override
        public void registerScanJobConsumer(ScanJobConsumer scanJobConsumer, int prefetchCount) {
            this.consumer = scanJobConsumer;
            this.prefetchCount = prefetchCount;
        }

        @Override
        public void registerDoneNotificationConsumer(
                BulkScan bulkScan,
                de.rub.nds.crawler.orchestration.DoneNotificationConsumer
                        doneNotificationConsumer) {}

        @Override
        public void notifyOfDoneScanJob(ScanJobDescription scanJobDescription) {}

        @Override
        public void closeConnection() {}

        public boolean isConsumerRegistered() {
            return consumer != null;
        }

        public int getPrefetchCount() {
            return prefetchCount;
        }

        public void submitJob(ScanJobDescription job) {
            if (consumer != null) {
                consumer.consumeScanJob(job);
            }
        }
    }

    // Test implementation of IPersistenceProvider
    private static class TestPersistenceProvider implements IPersistenceProvider {
        private boolean receivedScanResult = false;
        private CountDownLatch latch;

        @Override
        public void insertScanResult(ScanResult scanResult, ScanJobDescription job) {
            receivedScanResult = true;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        public void insertBulkScan(BulkScan bulkScan) {}

        @Override
        public void updateBulkScan(BulkScan bulkScan) {}

        public boolean hasReceivedScanResult() {
            return receivedScanResult;
        }

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }
    }
}
