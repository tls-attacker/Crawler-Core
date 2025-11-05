/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.dummy;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.scanner.core.config.ScannerDetail;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DummyPersistenceProviderTest {

    private DummyPersistenceProvider provider;
    private BulkScan testBulkScan;

    @BeforeEach
    void setUp() {
        provider = new DummyPersistenceProvider();

        // Create a test BulkScan
        ScanConfig scanConfig =
                new ScanConfig(ScannerDetail.NORMAL, 1, 5000) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads) {
                        return null;
                    }
                };

        testBulkScan =
                new BulkScan(
                        this.getClass(),
                        this.getClass(),
                        "test-scan",
                        scanConfig,
                        System.currentTimeMillis(),
                        false,
                        null);
        testBulkScan.set_id("test-bulk-scan-id");
    }

    @Test
    void testGetScanResultByScanJobDescriptionId_ReturnsMostRecent() throws InterruptedException {
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("93.184.216.34");
        target.setPort(443);

        ScanJobDescription jobDescription1 =
                new ScanJobDescription(target, testBulkScan, JobStatus.SUCCESS);
        Document resultDoc1 = new Document();
        resultDoc1.put("attempt", 1);
        ScanResult scanResult1 = new ScanResult(jobDescription1, resultDoc1);
        provider.insertScanResult(scanResult1, jobDescription1);

        Thread.sleep(10);

        ScanJobDescription jobDescription2 =
                new ScanJobDescription(target, testBulkScan, JobStatus.SUCCESS);
        Document resultDoc2 = new Document();
        resultDoc2.put("attempt", 2);
        ScanResult scanResult2 = new ScanResult(jobDescription2, resultDoc2);
        provider.insertScanResult(scanResult2, jobDescription2);

        Thread.sleep(10);

        ScanJobDescription jobDescription3 =
                new ScanJobDescription(target, testBulkScan, JobStatus.SUCCESS);
        Document resultDoc3 = new Document();
        resultDoc3.put("attempt", 3);
        ScanResult scanResult3 = new ScanResult(jobDescription3, resultDoc3);
        provider.insertScanResult(scanResult3, jobDescription3);

        String scanJobDescriptionId = scanResult2.getScanJobDescriptionId();

        ScanResult retrieved =
                provider.getScanResultByScanJobDescriptionId(
                        "test-db", "test-collection", scanJobDescriptionId);

        assertNotNull(retrieved);
        assertEquals(scanJobDescriptionId, retrieved.getScanJobDescriptionId());
        assertTrue(
                retrieved.getTimestamp().equals(scanResult3.getTimestamp())
                        || retrieved.getTimestamp().equals(scanResult2.getTimestamp())
                        || retrieved.getTimestamp().equals(scanResult1.getTimestamp()));
        assertTrue(
                retrieved.getTimestamp().compareTo(scanResult1.getTimestamp()) >= 0
                        && retrieved.getTimestamp().compareTo(scanResult2.getTimestamp()) >= 0
                        && retrieved.getTimestamp().compareTo(scanResult3.getTimestamp()) >= 0);
    }
}
