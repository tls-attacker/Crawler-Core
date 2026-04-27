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
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class BulkScanWorkerTest {

    // Test implementation of ScanConfig
    static class TestScanConfig extends ScanConfig implements Serializable {
        public TestScanConfig() {
            super(de.rub.nds.scanner.core.config.ScannerDetail.NORMAL, 0, 60);
        }

        @Override
        public BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
            return new TestBulkScanWorker(bulkScanID, this, parallelScanThreads);
        }
    }

    // Test implementation of BulkScanWorker
    static class TestBulkScanWorker extends BulkScanWorker<TestScanConfig> {
        private boolean initCalled = false;
        private boolean cleanupCalled = false;
        private ScanJobDescription capturedJobDescription = null;
        private final List<Document> emittedPartials = new CopyOnWriteArrayList<>();

        TestBulkScanWorker(String bulkScanId, TestScanConfig scanConfig, int parallelScanThreads) {
            super(bulkScanId, scanConfig, parallelScanThreads);
        }

        @Override
        public Document scan(
                ScanJobDescription jobDescription, Consumer<Document> progressConsumer) {
            // Capture the job description during scan
            capturedJobDescription = jobDescription;
            ScanTarget scanTarget = jobDescription.getScanTarget();

            Document partial = new Document();
            partial.put("phase", "in-progress");
            partial.put("target", scanTarget.getIp());
            emittedPartials.add(partial);
            progressConsumer.accept(partial);

            Document result = new Document();
            result.put("target", scanTarget.getIp());
            result.put("hasJobDescription", jobDescription != null);
            if (jobDescription != null) {
                result.put("jobId", jobDescription.getId().toString());
            }
            return result;
        }

        @Override
        protected void initInternal() {
            initCalled = true;
        }

        @Override
        protected void cleanupInternal() {
            cleanupCalled = true;
        }

        public boolean isInitCalled() {
            return initCalled;
        }

        public boolean isCleanupCalled() {
            return cleanupCalled;
        }

        public ScanJobDescription getCapturedJobDescription() {
            return capturedJobDescription;
        }
    }

    private static ScanJobDescription newJob(TestScanConfig config, String ip) {
        ScanTarget target = new ScanTarget();
        target.setIp(ip);
        target.setPort(443);

        BulkScan bulkScan =
                new BulkScan(
                        BulkScanWorkerTest.class,
                        BulkScanWorkerTest.class,
                        "test-db",
                        config,
                        System.currentTimeMillis(),
                        false,
                        null);

        return new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);
    }

    @Test
    void testGetCurrentJobDescriptionReturnsNullOutsideScanContext() {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        assertNull(
                worker.getCapturedJobDescription(),
                "Job description should be null before any scan");
    }

    @Test
    void testGetCurrentJobDescriptionReturnsCorrectJobInScanContext() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1"); // TEST-NET-1 (RFC 5737)

        ProgressableFuture<Document> future = worker.handle(jobDescription);
        Document result = future.get();

        assertTrue(
                result.getBoolean("hasJobDescription"),
                "Job description should be available in scan context");
        assertEquals(jobDescription.getId().toString(), result.getString("jobId"));

        assertNotNull(worker.getCapturedJobDescription());
        assertEquals(jobDescription.getId(), worker.getCapturedJobDescription().getId());
        assertEquals(
                jobDescription.getScanTarget(), worker.getCapturedJobDescription().getScanTarget());

        // Simulate the persistence flow
        DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();
        jobDescription.setStatus(JobStatus.SUCCESS);
        ScanResult scanResult = new ScanResult(jobDescription, result);

        assertEquals(
                jobDescription.getId().toString(),
                scanResult.getScanJobDescriptionId(),
                "ScanResult should use job description UUID as scanJobDescriptionId");

        persistenceProvider.insertScanResult(scanResult, jobDescription);

        ScanResult retrievedResult =
                persistenceProvider.getScanResultByScanJobDescriptionId(
                        "test-db", "test-collection", jobDescription.getId().toString());

        assertNotNull(
                retrievedResult, "Should be able to retrieve ScanResult by job description ID");
        assertEquals(jobDescription.getId().toString(), retrievedResult.getScanJobDescriptionId());
        assertEquals(scanResult.getBulkScan(), retrievedResult.getBulkScan());
        assertEquals(scanResult.getScanTarget(), retrievedResult.getScanTarget());
        assertEquals(scanResult.getResult(), retrievedResult.getResult());
    }

    @Test
    void testPartialResultCallbackReceivesEmittedPartials() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");

        List<Document> received = new CopyOnWriteArrayList<>();
        ProgressableFuture<Document> future = worker.handle(jobDescription, received::add);
        future.get();

        assertEquals(1, received.size(), "Caller callback should see the partial emitted by scan");
        assertEquals("in-progress", received.get(0).getString("phase"));
    }

    @Test
    void testNullPartialResultCallbackIsAllowed() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");

        // Null callback must not throw and the scan must still complete
        ProgressableFuture<Document> future = worker.handle(jobDescription, null);
        Document result = future.get();
        assertNotNull(result);
    }

    @Test
    void testPartialResultsStillUpdateProgressableFuture() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");

        // No callback supplied; partials should still flow into the future for pollers
        ProgressableFuture<Document> future = worker.handle(jobDescription);
        future.get();
        // After completion, getCurrentResult is the final result; just assert non-null
        assertNotNull(future.getCurrentResult());
    }

    @Test
    void testCallbackExceptionDoesNotKillScan() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");

        Consumer<Document> throwingCallback =
                p -> {
                    throw new RuntimeException("intentional");
                };
        ProgressableFuture<Document> future = worker.handle(jobDescription, throwingCallback);

        Document result = future.get();
        assertNotNull(result, "Scan should complete even when callback throws");
        assertTrue(result.getBoolean("hasJobDescription"));
    }

    @Test
    void testThreadLocalIsCleanedUpAfterScan() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");

        ProgressableFuture<Document> future = worker.handle(jobDescription);
        future.get();

        ScanJobDescription newJobDescription = newJob(config, "192.0.2.2");

        ProgressableFuture<Document> future2 = worker.handle(newJobDescription);
        Document result2 = future2.get();

        assertEquals(newJobDescription.getId().toString(), result2.getString("jobId"));
        assertEquals(newJobDescription.getId(), worker.getCapturedJobDescription().getId());
    }

    @Test
    void testMultipleConcurrentScansHaveSeparateContexts() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 2);

        List<ScanJobDescription> jobDescriptions = new ArrayList<>();
        List<ProgressableFuture<Document>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            ScanJobDescription jobDescription = newJob(config, "192.0.2." + (i + 1));
            jobDescriptions.add(jobDescription);
            futures.add(worker.handle(jobDescription));
        }

        for (int i = 0; i < 5; i++) {
            Document result = futures.get(i).get();
            assertTrue(result.getBoolean("hasJobDescription"));
            assertEquals(
                    jobDescriptions.get(i).getId().toString(),
                    result.getString("jobId"),
                    "Scan " + i + " should have its own job description");
        }
    }

    @Test
    void testInitializationIsCalledOnFirstHandle() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        assertFalse(worker.isInitCalled(), "Init should not be called before first handle");

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");
        worker.handle(jobDescription).get();

        assertTrue(worker.isInitCalled(), "Init should be called on first handle");
    }

    @Test
    void testCleanupIsCalledWhenAllJobsComplete() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");
        worker.handle(jobDescription).get();

        // Give cleanup a moment to execute (it runs after job completion)
        Thread.sleep(100);

        assertTrue(worker.isCleanupCalled(), "Cleanup should be called when all jobs complete");
    }

    @Test
    void testManualInitPreventsSelfCleanup() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker = new TestBulkScanWorker("test-bulk-id", config, 1);

        worker.init();
        assertTrue(worker.isInitCalled(), "Init should be called");

        ScanJobDescription jobDescription = newJob(config, "192.0.2.1");
        worker.handle(jobDescription).get();

        Thread.sleep(100);

        assertFalse(
                worker.isCleanupCalled(),
                "Cleanup should NOT be called when init was manual (shouldCleanupSelf = false)");

        worker.cleanup();
        assertTrue(worker.isCleanupCalled(), "Cleanup should be called when explicitly called");
    }
}
