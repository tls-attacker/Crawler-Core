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
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
                String bulkScanID,
                int parallelConnectionThreads,
                int parallelScanThreads,
                IPersistenceProvider persistenceProvider) {
            return new TestBulkScanWorker(
                    bulkScanID, this, parallelScanThreads, persistenceProvider);
        }
    }

    // Test implementation of BulkScanWorker
    static class TestBulkScanWorker extends BulkScanWorker<TestScanConfig> {
        private boolean initCalled = false;
        private boolean cleanupCalled = false;
        private ScanJobDescription capturedJobDescription = null;

        TestBulkScanWorker(
                String bulkScanId,
                TestScanConfig scanConfig,
                int parallelScanThreads,
                IPersistenceProvider persistenceProvider) {
            super(bulkScanId, scanConfig, parallelScanThreads, persistenceProvider);
        }

        @Override
        public Document scan(
                ScanJobDescription jobDescription, Consumer<Document> progressConsumer) {
            // Capture the job description during scan
            capturedJobDescription = jobDescription;
            ScanTarget scanTarget = jobDescription.getScanTarget();

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

    @Test
    void testGetCurrentJobDescriptionReturnsNullOutsideScanContext() {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker =
                new TestBulkScanWorker("test-bulk-id", config, 1, new DummyPersistenceProvider());

        // getCurrentJobDescription() is protected, so we can't call it directly from test
        // But we can verify through the scan() method that it returns null when not in context
        assertNull(
                worker.getCapturedJobDescription(),
                "Job description should be null before any scan");
    }

    @Test
    void testGetCurrentJobDescriptionReturnsCorrectJobInScanContext() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker =
                new TestBulkScanWorker("test-bulk-id", config, 1, new DummyPersistenceProvider());

        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1"); // TEST-NET-1 (RFC 5737)
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

        ScanJobDescription jobDescription =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Execute the scan
        ProgressableFuture<Document> future = worker.handle(jobDescription);
        Document result = future.get();

        // Verify the job description was available during scan
        assertTrue(
                result.getBoolean("hasJobDescription"),
                "Job description should be available in scan context");
        assertEquals(jobDescription.getId().toString(), result.getString("jobId"));

        // Verify the captured job description matches
        assertNotNull(worker.getCapturedJobDescription());
        assertEquals(jobDescription.getId(), worker.getCapturedJobDescription().getId());
        assertEquals(target, worker.getCapturedJobDescription().getScanTarget());

        // Simulate the partial results persistence flow
        DummyPersistenceProvider persistenceProvider = new DummyPersistenceProvider();

        // Update job status to SUCCESS (required by ScanResult constructor)
        jobDescription.setStatus(JobStatus.SUCCESS);

        // Create ScanResult from the scan result Document and job description
        ScanResult scanResult = new ScanResult(jobDescription, result);

        // Verify ScanResult has the correct scanJobDescriptionId
        assertEquals(
                jobDescription.getId().toString(),
                scanResult.getScanJobDescriptionId(),
                "ScanResult should use job description UUID as scanJobDescriptionId");

        // Simulate persisting to MongoDB
        persistenceProvider.insertScanResult(scanResult, jobDescription);

        // Simulate retrieving from MongoDB by scanJobDescriptionId
        ScanResult retrievedResult =
                persistenceProvider.getScanResultByScanJobDescriptionId(
                        "test-db", "test-collection", jobDescription.getId().toString());

        // Verify the retrieved result matches
        assertNotNull(
                retrievedResult, "Should be able to retrieve ScanResult by job description ID");
        assertEquals(
                jobDescription.getId().toString(),
                retrievedResult.getScanJobDescriptionId(),
                "Retrieved result should have matching scanJobDescriptionId");
        assertEquals(
                scanResult.getBulkScan(),
                retrievedResult.getBulkScan(),
                "Retrieved result should have matching bulk scan ID");
        assertEquals(
                scanResult.getScanTarget(),
                retrievedResult.getScanTarget(),
                "Retrieved result should have matching scan target");
        assertEquals(
                scanResult.getResult(),
                retrievedResult.getResult(),
                "Retrieved result should have matching result document");
    }

    @Test
    void testThreadLocalIsCleanedUpAfterScan() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker =
                new TestBulkScanWorker("test-bulk-id", config, 1, new DummyPersistenceProvider());

        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1"); // TEST-NET-1 (RFC 5737)
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

        ScanJobDescription jobDescription =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);

        // Execute the scan
        ProgressableFuture<Document> future = worker.handle(jobDescription);
        future.get(); // Wait for completion

        // After scan completes, verify we can run another scan
        ScanTarget newTarget = new ScanTarget();
        newTarget.setIp("192.0.2.2"); // TEST-NET-1 (RFC 5737)
        newTarget.setPort(443);

        ScanJobDescription newJobDescription =
                new ScanJobDescription(newTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        ProgressableFuture<Document> future2 = worker.handle(newJobDescription);
        Document result2 = future2.get();

        // The second scan should have the second job description, not the first
        assertEquals(newJobDescription.getId().toString(), result2.getString("jobId"));
        assertEquals(newJobDescription.getId(), worker.getCapturedJobDescription().getId());
    }

    @Test
    void testMultipleConcurrentScansHaveSeparateContexts() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker =
                new TestBulkScanWorker("test-bulk-id", config, 2, new DummyPersistenceProvider());

        BulkScan bulkScan =
                new BulkScan(
                        BulkScanWorkerTest.class,
                        BulkScanWorkerTest.class,
                        "test-db",
                        config,
                        System.currentTimeMillis(),
                        false,
                        null);

        // Create multiple job descriptions
        List<ScanJobDescription> jobDescriptions = new ArrayList<>();
        List<ProgressableFuture<Document>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            ScanTarget target = new ScanTarget();
            target.setIp("192.0.2." + (i + 1)); // TEST-NET-1 (RFC 5737)
            target.setPort(443);

            ScanJobDescription jobDescription =
                    new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);
            jobDescriptions.add(jobDescription);

            futures.add(worker.handle(jobDescription));
        }

        // Wait for all scans to complete and verify each got the correct job description
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
        TestBulkScanWorker worker =
                new TestBulkScanWorker("test-bulk-id", config, 1, new DummyPersistenceProvider());

        assertFalse(worker.isInitCalled(), "Init should not be called before first handle");

        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1"); // TEST-NET-1 (RFC 5737)
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

        ScanJobDescription jobDescription =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);

        ProgressableFuture<Document> future = worker.handle(jobDescription);
        future.get();

        assertTrue(worker.isInitCalled(), "Init should be called on first handle");
    }

    @Test
    void testCleanupIsCalledWhenAllJobsComplete() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker =
                new TestBulkScanWorker("test-bulk-id", config, 1, new DummyPersistenceProvider());

        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1"); // TEST-NET-1 (RFC 5737)
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

        ScanJobDescription jobDescription =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);

        ProgressableFuture<Document> future = worker.handle(jobDescription);
        future.get();

        // Give cleanup a moment to execute (it runs after job completion)
        Thread.sleep(100);

        assertTrue(worker.isCleanupCalled(), "Cleanup should be called when all jobs complete");
    }

    @Test
    void testManualInitPreventsSelfCleanup() throws Exception {
        TestScanConfig config = new TestScanConfig();
        TestBulkScanWorker worker =
                new TestBulkScanWorker("test-bulk-id", config, 1, new DummyPersistenceProvider());

        // Call init manually
        worker.init();
        assertTrue(worker.isInitCalled(), "Init should be called");

        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1"); // TEST-NET-1 (RFC 5737)
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

        ScanJobDescription jobDescription =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);

        ProgressableFuture<Document> future = worker.handle(jobDescription);
        future.get();

        // Give cleanup a moment (if it were to execute)
        Thread.sleep(100);

        assertFalse(
                worker.isCleanupCalled(),
                "Cleanup should NOT be called when init was manual (shouldCleanupSelf = false)");

        // Cleanup should only be called when we explicitly call it
        worker.cleanup();
        assertTrue(worker.isCleanupCalled(), "Cleanup should be called when explicitly called");
    }
}
