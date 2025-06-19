/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.test;

import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class TestScanConfig extends ScanConfig {

    public TestScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
        super(scannerDetail, reexecutions, timeout);
    }

    @Override
    public BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
        return new TestBulkScanWorker(bulkScanID);
    }

    private static class TestBulkScanWorker extends BulkScanWorker<TestScanConfig> {
        public TestBulkScanWorker(String bulkScanId) {
            super(null); // We'll create a simple test worker
        }

        @Override
        protected ScanResult performScan(ScanTarget scanTarget) {
            Map<String, Object> details = new HashMap<>();
            details.put("test", true);
            return new ScanResult(scanTarget, ZonedDateTime.now(), null, details);
        }
    }
}
