/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.constant.JobStatus;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.mockito.*;

class ScanResultTest {

    @Mock private ScanJobDescription mockScanJobDescription;
    @Mock private BulkScanInfo mockBulkScanInfo;

    private ScanTarget scanTarget;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        testDocument = new Document("test", "data");

        when(mockScanJobDescription.getBulkScanInfo()).thenReturn(mockBulkScanInfo);
        when(mockScanJobDescription.getScanTarget()).thenReturn(scanTarget);
        when(mockBulkScanInfo.getBulkScanId()).thenReturn("bulk-scan-123");
    }

    @Test
    void testConstructorWithScanJobDescription() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.SUCCESS);

        // When
        ScanResult result = new ScanResult(mockScanJobDescription, testDocument);

        // Then
        assertNotNull(result.getId());
        assertEquals("bulk-scan-123", result.getBulkScan());
        assertEquals(scanTarget, result.getScanTarget());
        assertEquals(JobStatus.SUCCESS, result.getResultStatus());
        assertEquals(testDocument, result.getResult());
    }

    @Test
    void testConstructorWithScanJobDescriptionThrowsOnToBeExecuted() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.TO_BE_EXECUTED);

        // When/Then
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ScanResult(mockScanJobDescription, testDocument));
        assertEquals(
                "ScanJobDescription must not be in TO_BE_EXECUTED state", exception.getMessage());
    }

    @Test
    void testFromExceptionWithErrorStatus() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.ERROR);
        Exception testException = new RuntimeException("Test error");

        // When
        ScanResult result = ScanResult.fromException(mockScanJobDescription, testException);

        // Then
        assertNotNull(result.getId());
        assertEquals("bulk-scan-123", result.getBulkScan());
        assertEquals(scanTarget, result.getScanTarget());
        assertEquals(JobStatus.ERROR, result.getResultStatus());
        assertNotNull(result.getResult());
        assertEquals(testException, result.getResult().get("exception"));
    }

    @Test
    void testFromExceptionWithCancelledStatus() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.CANCELLED);
        Exception testException = new RuntimeException("Cancelled");

        // When
        ScanResult result = ScanResult.fromException(mockScanJobDescription, testException);

        // Then
        assertEquals(JobStatus.CANCELLED, result.getResultStatus());
        assertEquals(testException, result.getResult().get("exception"));
    }

    @Test
    void testFromExceptionThrowsOnNonErrorStatus() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.SUCCESS);
        Exception testException = new RuntimeException("Test error");

        // When/Then
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> ScanResult.fromException(mockScanJobDescription, testException));
        assertEquals("ScanJobDescription must be in an error state", exception.getMessage());
    }

    @Test
    void testSetId() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.SUCCESS);
        ScanResult result = new ScanResult(mockScanJobDescription, testDocument);
        String newId = "new-id-456";

        // When
        result.setId(newId);

        // Then
        assertEquals(newId, result.getId());
    }

    @Test
    void testAllGetters() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.SUCCESS);

        // When
        ScanResult result = new ScanResult(mockScanJobDescription, testDocument);

        // Then
        assertNotNull(result.getId());
        assertTrue(result.getId().matches("[a-f0-9\\-]{36}")); // UUID format
        assertEquals("bulk-scan-123", result.getBulkScan());
        assertEquals(scanTarget, result.getScanTarget());
        assertEquals(testDocument, result.getResult());
        assertEquals(JobStatus.SUCCESS, result.getResultStatus());
    }

    @Test
    void testWithNullDocument() {
        // Given
        when(mockScanJobDescription.getStatus()).thenReturn(JobStatus.EMPTY);

        // When
        ScanResult result = new ScanResult(mockScanJobDescription, null);

        // Then
        assertNull(result.getResult());
        assertEquals(JobStatus.EMPTY, result.getResultStatus());
    }

    @Test
    void testAllErrorStatuses() {
        // Test that all error statuses work with fromException
        JobStatus[] errorStatuses = {
            JobStatus.ERROR, JobStatus.CANCELLED, JobStatus.INTERNAL_ERROR, JobStatus.CRAWLER_ERROR
        };

        for (JobStatus status : errorStatuses) {
            when(mockScanJobDescription.getStatus()).thenReturn(status);
            Exception testException = new RuntimeException("Test error for " + status);

            ScanResult result = ScanResult.fromException(mockScanJobDescription, testException);

            assertEquals(status, result.getResultStatus());
            assertEquals(testException, result.getResult().get("exception"));
        }
    }
}
