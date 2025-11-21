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
import de.rub.nds.crawler.test.TestScanConfig;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.util.concurrent.Future;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BulkScanWorkerManagerTest {

    private BulkScanWorkerManager manager;
    private TestScanConfig scanConfig;
    private ScanTarget scanTarget;

    @BeforeEach
    void setUp() {
        manager = BulkScanWorkerManager.getInstance();
        scanConfig = new TestScanConfig(ScannerDetail.ALL, 1, 5000);
        scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
    }

    @Test
    void testGetInstance() {
        BulkScanWorkerManager instance1 = BulkScanWorkerManager.getInstance();
        BulkScanWorkerManager instance2 = BulkScanWorkerManager.getInstance();
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    @Test
    void testGetBulkScanWorker() {
        String bulkScanId = "test-scan-1";
        BulkScanWorker<?> worker1 = manager.getBulkScanWorker(bulkScanId, scanConfig, 4, 8);
        assertNotNull(worker1, "Worker should not be null");

        // Should return the same worker for same bulkScanId
        BulkScanWorker<?> worker2 = manager.getBulkScanWorker(bulkScanId, scanConfig, 4, 8);
        assertSame(worker1, worker2, "Should return cached worker for same bulkScanId");

        // Different bulkScanId should create new worker
        BulkScanWorker<?> worker3 = manager.getBulkScanWorker("test-scan-2", scanConfig, 4, 8);
        assertNotSame(worker1, worker3, "Should create new worker for different bulkScanId");
    }

    @Test
    void testHandle() {
        // Create a mock BulkScan with the required constructor
        BulkScan bulkScan =
                new BulkScan(
                        this.getClass(), // scannerClass
                        this.getClass(), // crawlerClass
                        "TestScan", // name
                        scanConfig, // scanConfig
                        System.currentTimeMillis(), // startTime
                        false, // monitored
                        null // notifyUrl
                        );
        bulkScan.set_id("bulk-scan-123");

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        Future<Document> future = manager.handle(jobDescription, 4, 8);
        assertNotNull(future, "Future should not be null");
    }

    @Test
    void testHandleStatic() {
        // Create a mock BulkScan
        BulkScan bulkScan =
                new BulkScan(
                        this.getClass(), // scannerClass
                        this.getClass(), // crawlerClass
                        "TestScan", // name
                        scanConfig, // scanConfig
                        System.currentTimeMillis(), // startTime
                        false, // monitored
                        null // notifyUrl
                        );
        bulkScan.set_id("bulk-scan-456");

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        Future<Document> future = BulkScanWorkerManager.handleStatic(jobDescription, 4, 8);
        assertNotNull(future, "Future should not be null");
    }

    @Test
    void testGetBulkScanWorkerWithFailingWorkerCreation() {
        // Create a ScanConfig that throws an exception when creating worker
        ScanConfig failingConfig =
                new ScanConfig(ScannerDetail.ALL, 1, 5000) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads) {
                        throw new RuntimeException("Test exception");
                    }
                };

        String bulkScanId = "failing-scan";
        assertThrows(
                RuntimeException.class,
                () -> manager.getBulkScanWorker(bulkScanId, failingConfig, 4, 8),
                "Should throw UncheckedException when worker creation fails");
    }
}
