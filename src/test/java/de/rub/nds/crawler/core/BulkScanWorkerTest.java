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

import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.test.TestScanConfig;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BulkScanWorkerTest {

    private TestBulkScanWorker worker;
    private final String bulkScanId = "test-bulk-scan-id";

    @BeforeEach
    void setUp() {
        worker = new TestBulkScanWorker(bulkScanId, 4);
    }

    @Test
    void testBasicScan() throws Exception {
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);

        Future<Document> future = worker.handle(scanTarget);
        Document result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals("example.com", result.getString("hostname"));
        assertEquals(443, result.getInteger("port"));
        assertTrue(result.getBoolean("test"));
        assertTrue(worker.wasInitialized());

        // Wait a bit to see if it auto-cleaned up
        Thread.sleep(200);
        assertTrue(worker.wasCleanedUp()); // Should auto-cleanup after completing the job
    }

    @Test
    void testInitCalledOnceForMultipleScans() throws Exception {
        ScanTarget scanTarget1 = new ScanTarget();
        scanTarget1.setHostname("example1.com");
        scanTarget1.setPort(443);

        ScanTarget scanTarget2 = new ScanTarget();
        scanTarget2.setHostname("example2.com");
        scanTarget2.setPort(443);

        Future<Document> future1 = worker.handle(scanTarget1);
        Future<Document> future2 = worker.handle(scanTarget2);

        Document result1 = future1.get(5, TimeUnit.SECONDS);
        Document result2 = future2.get(5, TimeUnit.SECONDS);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(1, worker.getInitCount());
    }

    @Test
    void testManualInitAndCleanup() {
        // Test manual initialization
        assertTrue(worker.init());
        assertTrue(worker.wasInitialized());
        assertFalse(worker.init()); // Should return false when already initialized

        // Test manual cleanup
        assertTrue(worker.cleanup());
        assertTrue(worker.wasCleanedUp());
        assertFalse(worker.cleanup()); // Should return false when already cleaned up
    }

    @Test
    void testCleanupWithActiveJobs() throws Exception {
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("slow.example.com");
        scanTarget.setPort(443);

        // Configure worker to have a slow scan
        worker.setSlowScan(true);
        Future<Document> future = worker.handle(scanTarget);

        // Give it time to start
        Thread.sleep(100);

        // Try to cleanup while job is running
        assertFalse(worker.cleanup());
        assertFalse(worker.wasCleanedUp());

        // Let the job complete
        Document result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);

        // Wait a bit for cleanup to happen
        Thread.sleep(200);

        // Should have cleaned up automatically
        assertTrue(worker.wasCleanedUp());
    }

    @Test
    void testConcurrentInitialization() throws Exception {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulInits = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(
                            () -> {
                                try {
                                    startLatch.await();
                                    if (worker.init()) {
                                        successfulInits.incrementAndGet();
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    endLatch.countDown();
                                }
                            })
                    .start();
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));

        // Only one thread should have successfully initialized
        assertEquals(1, successfulInits.get());
        assertEquals(1, worker.getInitCount());
    }

    @Test
    void testConcurrentScansWithAutoCleanup() throws Exception {
        int scanCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CyclicBarrier barrier = new CyclicBarrier(scanCount);
        List<Future<Document>> futures = new ArrayList<>();

        // Configure worker to track cleanup
        worker.setTrackCleanup(true);

        for (int i = 0; i < scanCount; i++) {
            final int index = i;
            Future<Document> future =
                    CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    startLatch.await();
                                    barrier.await(); // Ensure all threads start scanning at the
                                    // same time

                                    ScanTarget scanTarget = new ScanTarget();
                                    scanTarget.setHostname("example" + index + ".com");
                                    scanTarget.setPort(443);

                                    return worker.handle(scanTarget).get();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
            futures.add(future);
        }

        // Start all scans
        startLatch.countDown();

        // Wait for all scans to complete
        for (Future<Document> future : futures) {
            assertNotNull(future.get(10, TimeUnit.SECONDS));
        }

        // Give cleanup a chance to run
        Thread.sleep(500);

        // Worker should have cleaned up automatically
        assertTrue(worker.wasCleanedUp());
    }

    @Test
    void testScanException() {
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("error.example.com");
        scanTarget.setPort(443);

        worker.setThrowException(true);
        Future<Document> future = worker.handle(scanTarget);

        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void testCleanupBeforeInit() {
        // Cleanup should return false if not initialized
        assertFalse(worker.cleanup());
        assertFalse(worker.wasCleanedUp());
    }

    // Test implementation
    private static class TestBulkScanWorker extends BulkScanWorker<TestScanConfig> {
        private final AtomicInteger initCount = new AtomicInteger(0);
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private final AtomicBoolean cleanedUp = new AtomicBoolean(false);
        private boolean slowScan = false;
        private boolean throwException = false;
        private boolean trackCleanup = false;

        public TestBulkScanWorker(String bulkScanId, int parallelScanThreads) {
            super(bulkScanId, new TestScanConfig(ScannerDetail.ALL, 1, 5000), parallelScanThreads);
        }

        @Override
        public Document scan(ScanTarget scanTarget) {
            if (throwException) {
                throw new RuntimeException("Simulated scan exception");
            }

            if (slowScan) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            Document doc = new Document();
            doc.put("test", true);
            doc.put("hostname", scanTarget.getHostname());
            doc.put("port", scanTarget.getPort());
            return doc;
        }

        @Override
        protected void initInternal() {
            initCount.incrementAndGet();
            initialized.set(true);
        }

        @Override
        protected void cleanupInternal() {
            cleanedUp.set(true);
            if (trackCleanup) {
                try {
                    // Small delay to ensure proper ordering in tests
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public int getInitCount() {
            return initCount.get();
        }

        public boolean wasInitialized() {
            return initialized.get();
        }

        public boolean wasCleanedUp() {
            return cleanedUp.get();
        }

        public void setSlowScan(boolean slowScan) {
            this.slowScan = slowScan;
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }

        public void setTrackCleanup(boolean trackCleanup) {
            this.trackCleanup = trackCleanup;
        }
    }
}
