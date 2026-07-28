/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScanResultTest {

    private BulkScan bulkScan;

    @BeforeEach
    void setUp() {
        ScanConfig scanConfig =
                new ScanConfig(ScannerDetail.NORMAL, 1, 5000, 1, List.of()) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads,
                            IPersistenceProvider persistenceProvider) {
                        return null;
                    }
                };
        bulkScan =
                new BulkScan(
                        this.getClass(),
                        this.getClass(),
                        "test-scan",
                        scanConfig,
                        System.currentTimeMillis(),
                        false,
                        null);
        bulkScan.set_id("test-bulk-scan-id");
    }

    private ScanJobDescription job(JobStatus status) {
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("93.184.216.34");
        target.setPort(443);
        return new ScanJobDescription(target, bulkScan, status);
    }

    @Test
    void partialResultIsKeyedByJobIdAndMarkedRunning() {
        ScanJobDescription jobDescription = job(JobStatus.TO_BE_EXECUTED);
        String jobId = jobDescription.getId().toString();

        Document content = new Document("report", "partial").append("isPartial", true);
        ScanResult partial = ScanResult.partialResult(jobDescription, content);

        assertEquals(jobId, partial.getId(), "Partial result must be keyed by the job ID");
        assertEquals(
                jobId,
                partial.getScanJobDescriptionId(),
                "Partial result must reference the job description ID");
        assertEquals(JobStatus.RUNNING, partial.getResultStatus());
        assertEquals(content, partial.getResult());
        assertEquals("test-bulk-scan-id", partial.getBulkScan());
    }

    @Test
    void finalResultSharesIdWithPartialSoItOverwrites() {
        ScanJobDescription jobDescription = job(JobStatus.TO_BE_EXECUTED);
        String jobId = jobDescription.getId().toString();

        ScanResult partial =
                ScanResult.partialResult(jobDescription, new Document("isPartial", true));

        jobDescription.setStatus(JobStatus.SUCCESS);
        ScanResult finalResult = new ScanResult(jobDescription, new Document("isPartial", false));

        assertEquals(jobId, finalResult.getId(), "Final result must be keyed by the job ID");
        assertEquals(
                partial.getId(),
                finalResult.getId(),
                "Partial and final results must share the same _id so the final overwrites the partial");
        assertEquals(JobStatus.SUCCESS, finalResult.getResultStatus());
    }

    @Test
    void distinctJobsProduceDistinctIds() {
        ScanResult a = ScanResult.partialResult(job(JobStatus.TO_BE_EXECUTED), new Document());
        ScanResult b = ScanResult.partialResult(job(JobStatus.TO_BE_EXECUTED), new Document());
        assertNotEquals(a.getId(), b.getId());
    }
}
