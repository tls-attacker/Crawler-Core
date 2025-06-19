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
import java.io.Serializable;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScanResultTest {

    private ScanJobDescription testScanJobDescription;

    private BulkScanInfo testBulkScanInfo;

    private ScanTarget testScanTarget;

    private BulkScan testBulkScan;

    private Document testDocument;

    // Test implementation of ScanConfig
    private static class TestScanConfig extends ScanConfig {
        public TestScanConfig() {
            super(de.rub.nds.scanner.core.config.ScannerDetail.NORMAL, 1, 1000);
        }

        @Override
        public de.rub.nds.crawler.core.BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        // Create test ScanTarget
        testScanTarget = new ScanTarget();
        testScanTarget.setIp("192.168.1.100");
        testScanTarget.setPort(443);

        // Create test BulkScan
        testBulkScan =
                new BulkScan(
                        ScanResultTest.class,
                        ScanResultTest.class,
                        "TestScan",
                        new TestScanConfig(),
                        System.currentTimeMillis(),
                        true,
                        null);
        testBulkScan.set_id("bulk-scan-123");

        // Create test BulkScanInfo
        testBulkScanInfo = new BulkScanInfo(testBulkScan);

        // Create test ScanJobDescription
        testScanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.SUCCESS);

        testDocument = new Document();
        testDocument.put("key", "value");
        testDocument.put("number", 42);
    }

    @Test
    void testConstructorWithScanJobDescription() {
        ScanResult scanResult = new ScanResult(testScanJobDescription, testDocument);

        assertNotNull(scanResult);
        assertNotNull(scanResult.getId());
        assertTrue(
                scanResult
                        .getId()
                        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertEquals("bulk-scan-123", scanResult.getBulkScan());
        assertEquals(testScanTarget, scanResult.getScanTarget());
        assertEquals(JobStatus.SUCCESS, scanResult.getResultStatus());
        assertEquals(testDocument, scanResult.getResult());
    }

    @Test
    void testConstructorWithToBeExecutedStatusThrowsException() {
        // Create a new ScanJobDescription with TO_BE_EXECUTED status
        ScanJobDescription toBeExecutedJob =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.TO_BE_EXECUTED);

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new ScanResult(toBeExecutedJob, testDocument);
                });
    }

    @Test
    void testConstructorWithDifferentStatuses() {
        // Test all valid statuses (except TO_BE_EXECUTED)
        JobStatus[] validStatuses = {
            JobStatus.SUCCESS,
            JobStatus.ERROR,
            JobStatus.EMPTY,
            JobStatus.DENYLISTED,
            JobStatus.UNRESOLVABLE,
            JobStatus.CANCELLED,
            JobStatus.SERIALIZATION_ERROR,
            JobStatus.INTERNAL_ERROR,
            JobStatus.RESOLUTION_ERROR,
            JobStatus.CRAWLER_ERROR
        };

        for (JobStatus status : validStatuses) {
            ScanJobDescription jobWithStatus =
                    new ScanJobDescription(
                            testScanTarget, testBulkScanInfo, "test_db", "test_collection", status);
            ScanResult result = new ScanResult(jobWithStatus, testDocument);
            assertEquals(status, result.getResultStatus());
        }
    }

    @Test
    void testFromExceptionWithErrorStatus() {
        ScanJobDescription errorJob =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.ERROR);
        Exception testException = new RuntimeException("Test error message");

        ScanResult errorResult = ScanResult.fromException(errorJob, testException);

        assertNotNull(errorResult);
        assertNotNull(errorResult.getId());
        assertEquals("bulk-scan-123", errorResult.getBulkScan());
        assertEquals(testScanTarget, errorResult.getScanTarget());
        assertEquals(JobStatus.ERROR, errorResult.getResultStatus());

        Document resultDoc = errorResult.getResult();
        assertNotNull(resultDoc);
        assertEquals(testException, resultDoc.get("exception"));
    }

    @Test
    void testFromExceptionWithNonErrorStatusThrowsException() {
        // testScanJobDescription already has SUCCESS status from setUp
        Exception testException = new RuntimeException("Test error");

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    ScanResult.fromException(testScanJobDescription, testException);
                });
    }

    @Test
    void testFromExceptionWithAllErrorStatuses() {
        Exception testException = new RuntimeException("Test error");

        for (JobStatus status : JobStatus.values()) {
            if (status.isError()) {
                ScanJobDescription errorJob =
                        new ScanJobDescription(
                                testScanTarget,
                                testBulkScanInfo,
                                "test_db",
                                "test_collection",
                                status);
                ScanResult result = ScanResult.fromException(errorJob, testException);
                assertEquals(status, result.getResultStatus());
                assertEquals(testException, result.getResult().get("exception"));
            }
        }
    }

    @Test
    void testGetAndSetId() {
        ScanResult scanResult = new ScanResult(testScanJobDescription, testDocument);

        String originalId = scanResult.getId();
        assertNotNull(originalId);

        String newId = "custom-id-12345";
        scanResult.setId(newId);
        assertEquals(newId, scanResult.getId());
    }

    @Test
    void testGetBulkScan() {
        ScanResult scanResult = new ScanResult(testScanJobDescription, testDocument);
        assertEquals("bulk-scan-123", scanResult.getBulkScan());
    }

    @Test
    void testGetScanTarget() {
        ScanResult scanResult = new ScanResult(testScanJobDescription, testDocument);
        assertEquals(testScanTarget, scanResult.getScanTarget());
    }

    @Test
    void testGetResult() {
        ScanResult scanResult = new ScanResult(testScanJobDescription, testDocument);
        assertEquals(testDocument, scanResult.getResult());
        assertEquals("value", scanResult.getResult().get("key"));
        assertEquals(42, scanResult.getResult().get("number"));
    }

    @Test
    void testGetResultStatus() {
        ScanJobDescription emptyJob =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.EMPTY);
        ScanResult scanResult = new ScanResult(emptyJob, testDocument);
        assertEquals(JobStatus.EMPTY, scanResult.getResultStatus());
    }

    @Test
    void testSerializable() {
        ScanResult scanResult = new ScanResult(testScanJobDescription, testDocument);
        assertTrue(Serializable.class.isAssignableFrom(scanResult.getClass()));
    }

    @Test
    void testFinalFields() throws NoSuchFieldException {
        // Verify that certain fields are final
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanResult.class.getDeclaredField("bulkScan").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanResult.class.getDeclaredField("scanTarget").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanResult.class.getDeclaredField("jobStatus").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanResult.class.getDeclaredField("result").getModifiers()));

        // id should not be final as it has a setter
        assertFalse(
                java.lang.reflect.Modifier.isFinal(
                        ScanResult.class.getDeclaredField("id").getModifiers()));
    }

    @Test
    void testNullDocument() {
        ScanResult scanResult = new ScanResult(testScanJobDescription, null);
        assertNull(scanResult.getResult());
    }

    @Test
    void testEmptyDocument() {
        Document emptyDoc = new Document();
        ScanResult scanResult = new ScanResult(testScanJobDescription, emptyDoc);
        assertNotNull(scanResult.getResult());
        assertTrue(scanResult.getResult().isEmpty());
    }

    @Test
    void testLargeDocument() {
        Document largeDoc = new Document();
        for (int i = 0; i < 1000; i++) {
            largeDoc.put("key" + i, "value" + i);
        }

        ScanResult scanResult = new ScanResult(testScanJobDescription, largeDoc);
        assertEquals(largeDoc, scanResult.getResult());
        assertEquals(1000, scanResult.getResult().size());
    }

    @Test
    void testUniqueIdGeneration() {
        // Create multiple ScanResults and verify unique IDs
        ScanResult result1 = new ScanResult(testScanJobDescription, testDocument);
        ScanResult result2 = new ScanResult(testScanJobDescription, testDocument);
        ScanResult result3 = new ScanResult(testScanJobDescription, testDocument);

        assertNotEquals(result1.getId(), result2.getId());
        assertNotEquals(result1.getId(), result3.getId());
        assertNotEquals(result2.getId(), result3.getId());
    }

    @Test
    void testFromExceptionWithNullException() {
        ScanJobDescription errorJob =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.ERROR);

        ScanResult errorResult = ScanResult.fromException(errorJob, null);

        assertNotNull(errorResult);
        assertNull(errorResult.getResult().get("exception"));
    }

    @Test
    void testJsonPropertyAnnotations() throws NoSuchMethodException {
        // Verify @JsonProperty annotations
        assertTrue(
                ScanResult.class
                        .getMethod("getId")
                        .isAnnotationPresent(com.fasterxml.jackson.annotation.JsonProperty.class));
        assertTrue(
                ScanResult.class
                        .getMethod("setId", String.class)
                        .isAnnotationPresent(com.fasterxml.jackson.annotation.JsonProperty.class));

        // Check annotation value
        assertEquals(
                "_id",
                ScanResult.class
                        .getMethod("getId")
                        .getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class)
                        .value());
    }

    @Test
    void testPrivateConstructor() throws NoSuchMethodException {
        // Verify the private constructor exists
        java.lang.reflect.Constructor<?>[] constructors =
                ScanResult.class.getDeclaredConstructors();
        boolean hasPrivateConstructor = false;

        for (java.lang.reflect.Constructor<?> constructor : constructors) {
            if (java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())
                    && constructor.getParameterCount() == 4) {
                hasPrivateConstructor = true;
                break;
            }
        }

        assertTrue(hasPrivateConstructor);
    }
}
