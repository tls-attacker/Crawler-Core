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
import java.io.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

class ScanJobDescriptionTest {

    @Mock private BulkScan mockBulkScan;
    @Mock private BulkScanInfo mockBulkScanInfo;

    private ScanTarget scanTarget;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);

        when(mockBulkScan.getName()).thenReturn("test-scan");
        when(mockBulkScan.getCollectionName()).thenReturn("test-collection");
    }

    @Test
    void testConstructorWithBulkScanInfo() {
        // When
        ScanJobDescription job =
                new ScanJobDescription(
                        scanTarget,
                        mockBulkScanInfo,
                        "dbName",
                        "collectionName",
                        JobStatus.TO_BE_EXECUTED);

        // Then
        assertEquals(scanTarget, job.getScanTarget());
        assertEquals(mockBulkScanInfo, job.getBulkScanInfo());
        assertEquals("dbName", job.getDbName());
        assertEquals("collectionName", job.getCollectionName());
        assertEquals(JobStatus.TO_BE_EXECUTED, job.getStatus());
    }

    @Test
    void testConstructorWithBulkScan() {
        // When
        ScanJobDescription job =
                new ScanJobDescription(scanTarget, mockBulkScan, JobStatus.TO_BE_EXECUTED);

        // Then
        assertEquals(scanTarget, job.getScanTarget());
        assertNotNull(job.getBulkScanInfo());
        assertEquals("test-scan", job.getDbName());
        assertEquals("test-collection", job.getCollectionName());
        assertEquals(JobStatus.TO_BE_EXECUTED, job.getStatus());
    }

    @Test
    void testSetStatus() {
        // Given
        ScanJobDescription job =
                new ScanJobDescription(scanTarget, mockBulkScan, JobStatus.TO_BE_EXECUTED);

        // When
        job.setStatus(JobStatus.SUCCESS);

        // Then
        assertEquals(JobStatus.SUCCESS, job.getStatus());
    }

    @Test
    void testSetDeliveryTag() {
        // Given
        ScanJobDescription job =
                new ScanJobDescription(scanTarget, mockBulkScan, JobStatus.TO_BE_EXECUTED);

        // When
        job.setDeliveryTag(123L);

        // Then
        assertEquals(123L, job.getDeliveryTag());
    }

    @Test
    void testSetDeliveryTagTwiceThrowsException() {
        // Given
        ScanJobDescription job =
                new ScanJobDescription(scanTarget, mockBulkScan, JobStatus.TO_BE_EXECUTED);
        job.setDeliveryTag(123L);

        // When/Then
        assertThrows(IllegalStateException.class, () -> job.setDeliveryTag(456L));
    }

    @Test
    void testGetDeliveryTagBeforeSettingThrowsException() {
        // Given
        ScanJobDescription job =
                new ScanJobDescription(scanTarget, mockBulkScan, JobStatus.TO_BE_EXECUTED);

        // When/Then
        assertThrows(Exception.class, job::getDeliveryTag);
    }

    @Test
    void testSerialization() throws Exception {
        // Given
        ScanJobDescription job =
                new ScanJobDescription(scanTarget, mockBulkScan, JobStatus.TO_BE_EXECUTED);
        job.setDeliveryTag(123L);

        // When - serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(job);
        oos.close();

        // And deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ScanJobDescription deserialized = (ScanJobDescription) ois.readObject();
        ois.close();

        // Then
        assertEquals(job.getScanTarget().getHostname(), deserialized.getScanTarget().getHostname());
        assertEquals(job.getDbName(), deserialized.getDbName());
        assertEquals(job.getCollectionName(), deserialized.getCollectionName());
        assertEquals(job.getStatus(), deserialized.getStatus());

        // Delivery tag should not be serialized (transient)
        assertThrows(Exception.class, deserialized::getDeliveryTag);
    }

    @Test
    void testAllJobStatuses() {
        // Test that all job statuses can be set
        JobStatus[] statuses = JobStatus.values();

        for (JobStatus status : statuses) {
            ScanJobDescription job = new ScanJobDescription(scanTarget, mockBulkScan, status);
            assertEquals(status, job.getStatus());
        }
    }

    @Test
    void testGettersReturnCorrectValues() {
        // Given
        String dbName = "test-db";
        String collectionName = "test-collection";
        JobStatus status = JobStatus.TO_BE_EXECUTED;

        // When
        ScanJobDescription job =
                new ScanJobDescription(
                        scanTarget, mockBulkScanInfo, dbName, collectionName, status);

        // Then
        assertEquals(scanTarget, job.getScanTarget());
        assertEquals(mockBulkScanInfo, job.getBulkScanInfo());
        assertEquals(dbName, job.getDbName());
        assertEquals(collectionName, job.getCollectionName());
        assertEquals(status, job.getStatus());
    }
}
