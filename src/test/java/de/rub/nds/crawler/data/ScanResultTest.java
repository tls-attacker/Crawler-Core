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
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.constant.JobStatus;
import java.io.Serializable;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScanResultTest {

    @Mock private ScanJobDescription mockScanJobDescription;

    @Mock private BulkScanInfo mockBulkScanInfo;

    @Mock private ScanTarget mockScanTarget;

    private Document testDocument;

    @BeforeEach
    void setUp() {
        when(mockScanJobDescription.getBulkScanInfo()).thenReturn(mockBulkScanInfo);
        when(mockBulkScanInfo.getBulkScanId()).thenReturn("bulk-scan-123");
        when(mockScanJobDescription.getScanTarget()).thenReturn(mockScanTarget);
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.SUCCESS);

        testDocument = new Document();
        testDocument.put("key", "value");
        testDocument.put("number", 42);
    }

    @Test
    void testConstructorWithScanJobDescription() {
        ScanResult scanResult = new ScanResult(mockScanJobDescription, testDocument);

        assertNotNull(scanResult);
        assertNotNull(scanResult.getId());
        assertTrue(
                scanResult
                        .getId()
                        .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertEquals("bulk-scan-123", scanResult.getBulkScan());
        assertEquals(mockScanTarget, scanResult.getScanTarget());
        assertEquals(JobStatus.SUCCESS, scanResult.getResultStatus());
        assertEquals(testDocument, scanResult.getResult());
    }

    @Test
    void testConstructorWithToBeExecutedStatusThrowsException() {
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.TO_BE_EXECUTED);

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new ScanResult(mockScanJobDescription, testDocument);
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
            when(mockScanJobDescription.getStatus()).thenReturn(status);
            ScanResult result = new ScanResult(mockScanJobDescription, testDocument);
            assertEquals(status, result.getResultStatus());
        }
    }

    @Test
    void testFromExceptionWithErrorStatus() {
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.ERROR);
        Exception testException = new RuntimeException("Test error message");

        ScanResult errorResult = ScanResult.fromException(mockScanJobDescription, testException);

        assertNotNull(errorResult);
        assertNotNull(errorResult.getId());
        assertEquals("bulk-scan-123", errorResult.getBulkScan());
        assertEquals(mockScanTarget, errorResult.getScanTarget());
        assertEquals(JobStatus.ERROR, errorResult.getResultStatus());

        Document resultDoc = errorResult.getResult();
        assertNotNull(resultDoc);
        assertEquals(testException, resultDoc.get("exception"));
    }

    @Test
    void testFromExceptionWithNonErrorStatusThrowsException() {
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.SUCCESS);
        Exception testException = new RuntimeException("Test error");

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    ScanResult.fromException(mockScanJobDescription, testException);
                });
    }

    @Test
    void testFromExceptionWithAllErrorStatuses() {
        Exception testException = new RuntimeException("Test error");

        for (JobStatus status : JobStatus.values()) {
            if (status.isError()) {
                when(mockScanJobDescription.getStatus()).thenReturn(status);
                ScanResult result = ScanResult.fromException(mockScanJobDescription, testException);
                assertEquals(status, result.getResultStatus());
                assertEquals(testException, result.getResult().get("exception"));
            }
        }
    }

    @Test
    void testGetAndSetId() {
        ScanResult scanResult = new ScanResult(mockScanJobDescription, testDocument);

        String originalId = scanResult.getId();
        assertNotNull(originalId);

        String newId = "custom-id-12345";
        scanResult.setId(newId);
        assertEquals(newId, scanResult.getId());
    }

    @Test
    void testGetBulkScan() {
        ScanResult scanResult = new ScanResult(mockScanJobDescription, testDocument);
        assertEquals("bulk-scan-123", scanResult.getBulkScan());
    }

    @Test
    void testGetScanTarget() {
        ScanResult scanResult = new ScanResult(mockScanJobDescription, testDocument);
        assertEquals(mockScanTarget, scanResult.getScanTarget());
    }

    @Test
    void testGetResult() {
        ScanResult scanResult = new ScanResult(mockScanJobDescription, testDocument);
        assertEquals(testDocument, scanResult.getResult());
        assertEquals("value", scanResult.getResult().get("key"));
        assertEquals(42, scanResult.getResult().get("number"));
    }

    @Test
    void testGetResultStatus() {
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.EMPTY);
        ScanResult scanResult = new ScanResult(mockScanJobDescription, testDocument);
        assertEquals(JobStatus.EMPTY, scanResult.getResultStatus());
    }

    @Test
    void testSerializable() {
        ScanResult scanResult = new ScanResult(mockScanJobDescription, testDocument);
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
        ScanResult scanResult = new ScanResult(mockScanJobDescription, null);
        assertNull(scanResult.getResult());
    }

    @Test
    void testEmptyDocument() {
        Document emptyDoc = new Document();
        ScanResult scanResult = new ScanResult(mockScanJobDescription, emptyDoc);
        assertNotNull(scanResult.getResult());
        assertTrue(scanResult.getResult().isEmpty());
    }

    @Test
    void testLargeDocument() {
        Document largeDoc = new Document();
        for (int i = 0; i < 1000; i++) {
            largeDoc.put("key" + i, "value" + i);
        }

        ScanResult scanResult = new ScanResult(mockScanJobDescription, largeDoc);
        assertEquals(largeDoc, scanResult.getResult());
        assertEquals(1000, scanResult.getResult().size());
    }

    @Test
    void testUniqueIdGeneration() {
        // Create multiple ScanResults and verify unique IDs
        ScanResult result1 = new ScanResult(mockScanJobDescription, testDocument);
        ScanResult result2 = new ScanResult(mockScanJobDescription, testDocument);
        ScanResult result3 = new ScanResult(mockScanJobDescription, testDocument);

        assertNotEquals(result1.getId(), result2.getId());
        assertNotEquals(result1.getId(), result3.getId());
        assertNotEquals(result2.getId(), result3.getId());
    }

    @Test
    void testFromExceptionWithNullException() {
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.ERROR);

        ScanResult errorResult = ScanResult.fromException(mockScanJobDescription, null);

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
