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

import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class BulkScanWorkerManagerTest {

    static class CapturingScanConfig extends ScanConfig implements Serializable {
        private int capturedParallelProbes = -1;

        CapturingScanConfig() {
            super(ScannerDetail.NORMAL, 0, 60, 1, List.of());
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
            capturedParallelProbes = getParallelProbes();
            return new CapturingBulkScanWorker(
                    bulkScanID, this, parallelScanThreads, persistenceProvider);
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
    void getBulkScanWorkerPropagatesParallelProbes() {
        CapturingScanConfig scanConfig = new CapturingScanConfig();
        scanConfig.setParallelProbes(9);
        BulkScanWorkerManager.getInstance()
                .getBulkScanWorker(
                        "bulk-scan-" + System.nanoTime(),
                        scanConfig,
                        5,
                        2,
                        new DummyPersistenceProvider());

        assertEquals(9, scanConfig.getCapturedParallelProbes());
    }

    @Test
    void oldGetBulkScanWorkerSignatureUsesScanConfigParallelProbes() {
        CapturingScanConfig scanConfig = new CapturingScanConfig();
        scanConfig.setParallelProbes(6);
        BulkScanWorkerManager.getInstance()
                .getBulkScanWorker(
                        "bulk-scan-" + System.nanoTime(),
                        scanConfig,
                        5,
                        2,
                        new DummyPersistenceProvider());

        assertEquals(6, scanConfig.getCapturedParallelProbes());
    }
}
