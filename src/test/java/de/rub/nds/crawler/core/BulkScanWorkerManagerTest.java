/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.Serializable;
import java.util.function.Consumer;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class BulkScanWorkerManagerTest {

    static class CapturingScanConfig extends ScanConfig implements Serializable {
        private int capturedParallelProbes = -1;

        CapturingScanConfig() {
            super(ScannerDetail.NORMAL, 0, 60);
        }

        int getCapturedParallelProbes() {
            return capturedParallelProbes;
        }

        @Override
        public BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID,
                int parallelConnectionThreads,
                int parallelScanThreads,
                IPersistenceProvider persistenceProvider) {
            return new CapturingBulkScanWorker(
                    bulkScanID, this, parallelScanThreads, persistenceProvider);
        }

        @Override
        public BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID,
                int parallelConnectionThreads,
                int parallelScanThreads,
                int parallelProbes,
                IPersistenceProvider persistenceProvider) {
            capturedParallelProbes = parallelProbes;
            return createWorker(
                    bulkScanID,
                    parallelConnectionThreads,
                    parallelScanThreads,
                    persistenceProvider);
        }
    }

    static class CapturingBulkScanWorker extends BulkScanWorker<CapturingScanConfig> {

        CapturingBulkScanWorker(
                String bulkScanId,
                CapturingScanConfig scanConfig,
                int parallelScanThreads,
                IPersistenceProvider persistenceProvider) {
            super(bulkScanId, scanConfig, parallelScanThreads, persistenceProvider);
        }

        @Override
        public Document scan(
                ScanJobDescription jobDescription, Consumer<Document> progressConsumer) {
            return new Document("status", "ok");
        }

        @Override
        protected void initInternal() {}

        @Override
        protected void cleanupInternal() {}
    }

    @Test
    void handleStaticPropagatesParallelProbes() throws Exception {
        CapturingScanConfig scanConfig = new CapturingScanConfig();
        ScanJobDescription scanJobDescription = getScanJobDescription(scanConfig);

        BulkScanWorkerManager.handleStatic(
                        scanJobDescription, 5, 2, 9, new DummyPersistenceProvider())
                .get();

        assertEquals(9, scanConfig.getCapturedParallelProbes());
    }

    @Test
    void handleOldSignatureDefaultsParallelProbesToOne() throws Exception {
        CapturingScanConfig scanConfig = new CapturingScanConfig();
        ScanJobDescription scanJobDescription = getScanJobDescription(scanConfig);

        BulkScanWorkerManager.handleStatic(scanJobDescription, 5, 2, new DummyPersistenceProvider())
                .get();

        assertEquals(1, scanConfig.getCapturedParallelProbes());
    }

    private static ScanJobDescription getScanJobDescription(ScanConfig scanConfig) {
        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1");
        target.setPort(443);

        String bulkScanId = "bulk-scan-" + System.nanoTime();
        BulkScan bulkScan =
                new BulkScan(
                        BulkScanWorkerManagerTest.class,
                        BulkScanWorkerManagerTest.class,
                        "test-scan",
                        scanConfig,
                        System.currentTimeMillis(),
                        false,
                        null);
        bulkScan.set_id(bulkScanId);

        return new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);
    }
}
