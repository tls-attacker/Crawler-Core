/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.constant.JobStatus;
import org.bson.Document;
import org.junit.jupiter.api.Test;

public class ScanResultTest {

    private BulkScan createTestBulkScan(String id, String name) {
        BulkScan bulkScan =
                new BulkScan(
                        getClass(), // scannerClass
                        getClass(), // crawlerClass
                        name,
                        null, // scanConfig
                        System.currentTimeMillis(),
                        false, // monitored
                        null // notifyUrl
                        );
        bulkScan.set_id(id);
        return bulkScan;
    }

    @Test
    public void testConstructorWithScanJobDescription() {
        // Prepare test data
        BulkScan bulkScan = createTestBulkScan("bulk-scan-123", "test-scan");

        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setIp("192.168.1.1");

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

        Document resultDocument = new Document();
        resultDocument.put("test", "value");

        // Create ScanResult
        ScanResult scanResult = new ScanResult(jobDescription, resultDocument);

        // Verify properties
        assertNotNull(scanResult.getId());
        assertEquals("bulk-scan-123", scanResult.getBulkScan());
        assertEquals(scanTarget, scanResult.getScanTarget());
        assertEquals(JobStatus.SUCCESS, scanResult.getResultStatus());
        assertEquals(resultDocument, scanResult.getResult());
    }

    @Test
    public void testConstructorWithScanJobDescriptionInvalidStatus() {
        // Prepare test data with TO_BE_EXECUTED status
        BulkScan bulkScan = createTestBulkScan("bulk-scan-123", "test-scan");

        ScanTarget scanTarget = new ScanTarget();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.TO_BE_EXECUTED);

        Document resultDocument = new Document();

        // Should throw exception for TO_BE_EXECUTED status
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new ScanResult(jobDescription, resultDocument);
                },
                "ScanJobDescription must not be in TO_BE_EXECUTED state");
    }

    @Test
    public void testFromExceptionWithErrorStatus() {
        // Prepare test data
        BulkScan bulkScan = createTestBulkScan("bulk-scan-456", "error-scan");

        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("error.com");

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.ERROR);

        Exception testException = new RuntimeException("Test error");

        // Create ScanResult from exception
        ScanResult scanResult = ScanResult.fromException(jobDescription, testException);

        // Verify properties
        assertNotNull(scanResult.getId());
        assertEquals("bulk-scan-456", scanResult.getBulkScan());
        assertEquals(scanTarget, scanResult.getScanTarget());
        assertEquals(JobStatus.ERROR, scanResult.getResultStatus());
        assertNotNull(scanResult.getResult());
        assertEquals(testException, scanResult.getResult().get("exception"));
    }

    @Test
    public void testFromExceptionWithCancelledStatus() {
        // Prepare test data with CANCELLED status (which is also an error status)
        BulkScan bulkScan = createTestBulkScan("bulk-scan-789", "cancelled-scan");

        ScanTarget scanTarget = new ScanTarget();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.CANCELLED);

        Exception timeoutException = new RuntimeException("Timeout occurred");

        // Create ScanResult from exception
        ScanResult scanResult = ScanResult.fromException(jobDescription, timeoutException);

        // Verify properties
        assertNotNull(scanResult);
        assertEquals("bulk-scan-789", scanResult.getBulkScan());
        assertEquals(JobStatus.CANCELLED, scanResult.getResultStatus());
        assertEquals(timeoutException, scanResult.getResult().get("exception"));
    }

    @Test
    public void testFromExceptionWithNonErrorStatus() {
        // Prepare test data with non-error status
        BulkScan bulkScan = createTestBulkScan("bulk-scan-999", "success-scan");
        ScanTarget scanTarget = new ScanTarget();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

        Exception testException = new RuntimeException("Test");

        // Should throw exception for non-error status
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    ScanResult.fromException(jobDescription, testException);
                },
                "ScanJobDescription must be in an error state");
    }

    @Test
    public void testIdGetterAndSetter() {
        // Create ScanResult
        BulkScan bulkScan = createTestBulkScan("bulk-scan", "id-test-scan");

        ScanTarget scanTarget = new ScanTarget();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

        ScanResult scanResult = new ScanResult(jobDescription, new Document());

        // Test default ID
        String originalId = scanResult.getId();
        assertNotNull(originalId);
        assertTrue(originalId.length() > 0);

        // Test setter
        String newId = "custom-id-123";
        scanResult.setId(newId);
        assertEquals(newId, scanResult.getId());
    }

    @Test
    public void testAllGetters() {
        // Prepare test data
        BulkScan bulkScan = createTestBulkScan("test-bulk-scan", "getter-test-scan");

        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("test.com");
        scanTarget.setPort(443);

        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.SUCCESS);

        Document resultDoc = new Document();
        resultDoc.put("key", "value");

        ScanResult scanResult = new ScanResult(jobDescription, resultDoc);

        // Test all getters
        assertNotNull(scanResult.getId());
        assertEquals("test-bulk-scan", scanResult.getBulkScan());
        assertEquals(scanTarget, scanResult.getScanTarget());
        assertEquals("test.com", scanResult.getScanTarget().getHostname());
        assertEquals(443, scanResult.getScanTarget().getPort());
        assertEquals(JobStatus.SUCCESS, scanResult.getResultStatus());
        assertEquals(resultDoc, scanResult.getResult());
        assertEquals("value", scanResult.getResult().get("key"));
    }

    @Test
    public void testWithEmptyStatus() {
        // Test with EMPTY status which is not an error
        BulkScan bulkScan = createTestBulkScan("empty-scan", "empty-scan");
        ScanTarget scanTarget = new ScanTarget();
        ScanJobDescription jobDescription =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.EMPTY);

        Document emptyResult = new Document();
        ScanResult scanResult = new ScanResult(jobDescription, emptyResult);

        assertEquals(JobStatus.EMPTY, scanResult.getResultStatus());
        assertNotNull(scanResult.getId());
    }

    @Test
    public void testWithVariousErrorStatuses() {
        // Test with different error statuses
        BulkScan bulkScan = createTestBulkScan("error-scan", "error-scan");
        ScanTarget scanTarget = new ScanTarget();
        Exception ex = new RuntimeException("Test");

        // Test RESOLUTION_ERROR
        ScanJobDescription jobDesc1 =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.RESOLUTION_ERROR);
        ScanResult result1 = ScanResult.fromException(jobDesc1, ex);
        assertEquals(JobStatus.RESOLUTION_ERROR, result1.getResultStatus());

        // Test CRAWLER_ERROR
        ScanJobDescription jobDesc2 =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.CRAWLER_ERROR);
        ScanResult result2 = ScanResult.fromException(jobDesc2, ex);
        assertEquals(JobStatus.CRAWLER_ERROR, result2.getResultStatus());

        // Test INTERNAL_ERROR
        ScanJobDescription jobDesc3 =
                new ScanJobDescription(scanTarget, bulkScan, JobStatus.INTERNAL_ERROR);
        ScanResult result3 = ScanResult.fromException(jobDesc3, ex);
        assertEquals(JobStatus.INTERNAL_ERROR, result3.getResultStatus());
    }
}
