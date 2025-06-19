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
import java.io.*;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScanJobDescriptionTest {

    @Mock private ScanTarget mockScanTarget;

    @Mock private BulkScanInfo mockBulkScanInfo;

    @Mock private BulkScan mockBulkScan;

    private ScanJobDescription scanJobDescription;

    @BeforeEach
    void setUp() {
        when(mockBulkScan.getName()).thenReturn("TestScan");
        when(mockBulkScan.getCollectionName()).thenReturn("test_collection");
        when(mockBulkScan.get_id()).thenReturn("bulk-scan-id");
        when(mockBulkScan.getScanConfig()).thenReturn(mock(ScanConfig.class));
        when(mockBulkScan.isMonitored()).thenReturn(true);
    }

    @Test
    void testConstructorWithBulkScanInfo() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.TO_BE_EXECUTED);

        assertNotNull(scanJobDescription);
        assertEquals(mockScanTarget, scanJobDescription.getScanTarget());
        assertEquals(mockBulkScanInfo, scanJobDescription.getBulkScanInfo());
        assertEquals("test_db", scanJobDescription.getDbName());
        assertEquals("test_collection", scanJobDescription.getCollectionName());
        assertEquals(JobStatus.TO_BE_EXECUTED, scanJobDescription.getStatus());
    }

    @Test
    void testConstructorWithBulkScan() {
        scanJobDescription =
                new ScanJobDescription(mockScanTarget, mockBulkScan, JobStatus.SUCCESS);

        assertNotNull(scanJobDescription);
        assertEquals(mockScanTarget, scanJobDescription.getScanTarget());
        assertNotNull(scanJobDescription.getBulkScanInfo());
        assertEquals("TestScan", scanJobDescription.getDbName());
        assertEquals("test_collection", scanJobDescription.getCollectionName());
        assertEquals(JobStatus.SUCCESS, scanJobDescription.getStatus());

        // Verify BulkScanInfo was created
        verify(mockBulkScan).getName();
        verify(mockBulkScan).getCollectionName();
    }

    @Test
    void testGetScanTarget() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals(mockScanTarget, scanJobDescription.getScanTarget());
    }

    @Test
    void testGetDbName() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "production_db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals("production_db", scanJobDescription.getDbName());
    }

    @Test
    void testGetCollectionName() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "scan_results",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals("scan_results", scanJobDescription.getCollectionName());
    }

    @Test
    void testGetAndSetStatus() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals(JobStatus.TO_BE_EXECUTED, scanJobDescription.getStatus());

        scanJobDescription.setStatus(JobStatus.SUCCESS);
        assertEquals(JobStatus.SUCCESS, scanJobDescription.getStatus());

        scanJobDescription.setStatus(JobStatus.ERROR);
        assertEquals(JobStatus.ERROR, scanJobDescription.getStatus());
    }

    @Test
    void testDeliveryTagInitiallyEmpty() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        // Should throw NoSuchElementException as deliveryTag is empty
        assertThrows(
                NoSuchElementException.class,
                () -> {
                    scanJobDescription.getDeliveryTag();
                });
    }

    @Test
    void testSetAndGetDeliveryTag() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        scanJobDescription.setDeliveryTag(12345L);
        assertEquals(12345L, scanJobDescription.getDeliveryTag());
    }

    @Test
    void testSetDeliveryTagTwiceThrowsException() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        scanJobDescription.setDeliveryTag(12345L);

        // Second set should throw IllegalStateException
        assertThrows(
                IllegalStateException.class,
                () -> {
                    scanJobDescription.setDeliveryTag(67890L);
                });

        // Original value should remain
        assertEquals(12345L, scanJobDescription.getDeliveryTag());
    }

    @Test
    void testGetBulkScanInfo() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals(mockBulkScanInfo, scanJobDescription.getBulkScanInfo());
    }

    @Test
    void testSerializable() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertTrue(Serializable.class.isAssignableFrom(scanJobDescription.getClass()));
    }

    @Test
    void testSerializationDeserialization() throws IOException, ClassNotFoundException {
        // Create a real ScanTarget for serialization
        ScanTarget realTarget = new ScanTarget();
        realTarget.setIp("192.168.1.1");
        realTarget.setPort(443);

        // Create a real BulkScan and BulkScanInfo
        BulkScan realBulkScan = mock(BulkScan.class);
        when(realBulkScan.get_id()).thenReturn("real-id");
        when(realBulkScan.getScanConfig()).thenReturn(mock(ScanConfig.class));
        when(realBulkScan.isMonitored()).thenReturn(false);
        BulkScanInfo realBulkScanInfo = new BulkScanInfo(realBulkScan);

        scanJobDescription =
                new ScanJobDescription(
                        realTarget,
                        realBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.SUCCESS);

        // Set delivery tag before serialization
        scanJobDescription.setDeliveryTag(999L);

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(scanJobDescription);
        oos.close();

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ScanJobDescription deserialized = (ScanJobDescription) ois.readObject();
        ois.close();

        // Verify deserialized object
        assertNotNull(deserialized);
        assertEquals("192.168.1.1", deserialized.getScanTarget().getIp());
        assertEquals(443, deserialized.getScanTarget().getPort());
        assertEquals("test_db", deserialized.getDbName());
        assertEquals("test_collection", deserialized.getCollectionName());
        assertEquals(JobStatus.SUCCESS, deserialized.getStatus());
        assertEquals("real-id", deserialized.getBulkScanInfo().getBulkScanId());

        // Delivery tag should be reset after deserialization
        assertThrows(
                NoSuchElementException.class,
                () -> {
                    deserialized.getDeliveryTag();
                });

        // Should be able to set new delivery tag
        deserialized.setDeliveryTag(111L);
        assertEquals(111L, deserialized.getDeliveryTag());
    }

    @Test
    void testAllJobStatusValues() {
        for (JobStatus status : JobStatus.values()) {
            ScanJobDescription jobDesc =
                    new ScanJobDescription(
                            mockScanTarget, mockBulkScanInfo, "db", "collection", status);
            assertEquals(status, jobDesc.getStatus());
        }
    }

    @Test
    void testNullValues() {
        // Test with null values
        scanJobDescription = new ScanJobDescription(null, null, null, null, null);

        assertNull(scanJobDescription.getScanTarget());
        assertNull(scanJobDescription.getBulkScanInfo());
        assertNull(scanJobDescription.getDbName());
        assertNull(scanJobDescription.getCollectionName());
        assertNull(scanJobDescription.getStatus());
    }

    @Test
    void testEmptyStrings() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget, mockBulkScanInfo, "", "", JobStatus.TO_BE_EXECUTED);

        assertEquals("", scanJobDescription.getDbName());
        assertEquals("", scanJobDescription.getCollectionName());
    }

    @Test
    void testSetDeliveryTagWithNull() {
        scanJobDescription =
                new ScanJobDescription(
                        mockScanTarget,
                        mockBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        // This should work fine and create Optional.of(null) which will throw NPE later
        assertThrows(
                NullPointerException.class,
                () -> {
                    scanJobDescription.setDeliveryTag(null);
                });
    }

    @Test
    void testFinalFields() throws NoSuchFieldException {
        // Verify that certain fields are final
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanJobDescription.class.getDeclaredField("scanTarget").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanJobDescription.class.getDeclaredField("bulkScanInfo").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanJobDescription.class.getDeclaredField("dbName").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        ScanJobDescription.class
                                .getDeclaredField("collectionName")
                                .getModifiers()));

        // Status should not be final as it has a setter
        assertFalse(
                java.lang.reflect.Modifier.isFinal(
                        ScanJobDescription.class.getDeclaredField("status").getModifiers()));
    }

    @Test
    void testTransientDeliveryTag() throws NoSuchFieldException {
        // Verify deliveryTag is transient
        assertTrue(
                java.lang.reflect.Modifier.isTransient(
                        ScanJobDescription.class.getDeclaredField("deliveryTag").getModifiers()));
    }
}
