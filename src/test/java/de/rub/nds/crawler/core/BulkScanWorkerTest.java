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
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.scanner.core.constants.ScannerDetail;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BulkScanWorkerTest {

    @Test
    void testHandle() throws ExecutionException, InterruptedException, TimeoutException {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);
        ScanTarget target = new ScanTarget("example.com", 443);

        Future<ScanResult> future = worker.handle(target);
        assertNotNull(future);

        ScanResult result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(target, result.getScanTarget());
    }

    @Test
    void testInitialization() {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);

        assertFalse(worker.isInitialized(), "Worker should not be initialized at creation");

        ScanTarget target = new ScanTarget("example.com", 443);
        worker.handle(target);

        assertTrue(worker.isInitialized(), "Worker should be initialized after first handle call");
    }

    @Test
    void testConcurrentInitialization() throws InterruptedException {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger initCount = new AtomicInteger(0);

        worker.setInitCounter(initCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(
                            () -> {
                                try {
                                    startLatch.await();
                                    ScanTarget target = new ScanTarget("example.com", 443);
                                    worker.handle(target);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    endLatch.countDown();
                                }
                            })
                    .start();
        }

        startLatch.countDown();
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));

        assertEquals(
                1, initCount.get(), "Init should only be called once despite concurrent access");
    }

    @Test
    void testCleanup() throws InterruptedException {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);

        // Initialize the worker
        ScanTarget target = new ScanTarget("example.com", 443);
        Future<ScanResult> future = worker.handle(target);

        // Wait for the scan to complete
        Thread.sleep(100);

        // Cleanup should succeed when no active jobs
        worker.cleanup();
        assertTrue(worker.isCleanedUp(), "Worker should be cleaned up");
    }

    @Test
    void testCleanupWithActiveJobs() {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);
        worker.setDelayForScans(2000); // 2 second delay

        // Start a scan that will take time
        ScanTarget target = new ScanTarget("example.com", 443);
        Future<ScanResult> future = worker.handle(target);

        // Try to cleanup while job is active
        worker.cleanup();
        assertFalse(worker.isCleanedUp(), "Worker should not be cleaned up while jobs are active");
    }

    @Test
    void testAutoCleanupAfterJobsComplete() throws InterruptedException {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);
        worker.setDelayForScans(100); // Short delay

        // Start a scan
        ScanTarget target = new ScanTarget("example.com", 443);
        Future<ScanResult> future = worker.handle(target);

        // Wait for scan to complete
        Thread.sleep(200);

        // Try cleanup
        worker.cleanup();
        assertTrue(worker.isCleanedUp(), "Worker should be cleaned up after jobs complete");
    }

    @Test
    void testMultipleConcurrentScans() throws InterruptedException, ExecutionException {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);

        List<Future<ScanResult>> futures = new ArrayList<>();
        int scanCount = 20;

        for (int i = 0; i < scanCount; i++) {
            ScanTarget target = new ScanTarget("example" + i + ".com", 443);
            futures.add(worker.handle(target));
        }

        // Wait for all scans to complete
        for (Future<ScanResult> future : futures) {
            assertNotNull(future.get());
        }

        assertEquals(scanCount, worker.getScanCount(), "All scans should have been performed");
    }

    @Test
    void testScanWithException() {
        BulkScan bulkScan = createTestBulkScan();
        TestBulkScanWorker worker = new TestBulkScanWorker(bulkScan);
        worker.setThrowException(true);

        ScanTarget target = new ScanTarget("example.com", 443);
        Future<ScanResult> future = worker.handle(target);

        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    }

    private BulkScan createTestBulkScan() {
        BulkScan bulkScan = new BulkScan();
        bulkScan.setId("test-bulk-scan");
        bulkScan.setScanConfig(new ScanConfig(ScannerDetail.NORMAL, 5, 5));
        bulkScan.setStartTime(ZonedDateTime.now());
        return bulkScan;
    }

    private static class TestBulkScanWorker extends BulkScanWorker {
        private AtomicInteger initCounter;
        private AtomicBoolean cleanedUp = new AtomicBoolean(false);
        private AtomicInteger scanCount = new AtomicInteger(0);
        private int delayMillis = 0;
        private boolean throwException = false;

        public TestBulkScanWorker(BulkScan bulkScan) {
            super(bulkScan);
        }

        @Override
        protected void init() {
            super.init();
            if (initCounter != null) {
                initCounter.incrementAndGet();
            }
        }

        @Override
        protected void cleanup() {
            super.cleanup();
            cleanedUp.set(true);
        }

        @Override
        protected ScanResult performScan(ScanTarget scanTarget) {
            if (throwException) {
                throw new RuntimeException("Test exception");
            }

            scanCount.incrementAndGet();

            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            Map<String, Object> details = new HashMap<>();
            details.put("test", true);
            return new ScanResult(scanTarget, ZonedDateTime.now(), null, details);
        }

        public void setInitCounter(AtomicInteger counter) {
            this.initCounter = counter;
        }

        public boolean isCleanedUp() {
            return cleanedUp.get();
        }

        public int getScanCount() {
            return scanCount.get();
        }

        public void setDelayForScans(int millis) {
            this.delayMillis = millis;
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }
    }
}
