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
import java.io.*;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScanJobDescriptionTest {

    private ScanTarget testScanTarget;

    private BulkScanInfo testBulkScanInfo;

    private BulkScan testBulkScan;

    private ScanJobDescription scanJobDescription;

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
                        ScanJobDescriptionTest.class,
                        ScanJobDescriptionTest.class,
                        "TestScan",
                        new TestScanConfig(),
                        System.currentTimeMillis(),
                        true,
                        null);
        testBulkScan.set_id("bulk-scan-id");
        testBulkScan.setCollectionName("test_collection");

        // Create test BulkScanInfo
        testBulkScanInfo = new BulkScanInfo(testBulkScan);
    }

    @Test
    void testConstructorWithBulkScanInfo() {
        scanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "test_db",
                        "test_collection",
                        JobStatus.TO_BE_EXECUTED);

        assertNotNull(scanJobDescription);
        assertEquals(testScanTarget, scanJobDescription.getScanTarget());
        assertEquals(testBulkScanInfo, scanJobDescription.getBulkScanInfo());
        assertEquals("test_db", scanJobDescription.getDbName());
        assertEquals("test_collection", scanJobDescription.getCollectionName());
        assertEquals(JobStatus.TO_BE_EXECUTED, scanJobDescription.getStatus());
    }

    @Test
    void testConstructorWithBulkScan() {
        scanJobDescription =
                new ScanJobDescription(testScanTarget, testBulkScan, JobStatus.SUCCESS);

        assertNotNull(scanJobDescription);
        assertEquals(testScanTarget, scanJobDescription.getScanTarget());
        assertNotNull(scanJobDescription.getBulkScanInfo());
        assertEquals("TestScan", scanJobDescription.getDbName());
        assertEquals("test_collection", scanJobDescription.getCollectionName());
        assertEquals(JobStatus.SUCCESS, scanJobDescription.getStatus());

        // Verify BulkScanInfo was created properly
        assertEquals("bulk-scan-id", scanJobDescription.getBulkScanInfo().getBulkScanId());
        assertTrue(scanJobDescription.getBulkScanInfo().isMonitored());
    }

    @Test
    void testGetScanTarget() {
        scanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals(testScanTarget, scanJobDescription.getScanTarget());
    }

    @Test
    void testGetDbName() {
        scanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "production_db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals("production_db", scanJobDescription.getDbName());
    }

    @Test
    void testGetCollectionName() {
        scanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
                        "db",
                        "scan_results",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals("scan_results", scanJobDescription.getCollectionName());
    }

    @Test
    void testGetAndSetStatus() {
        scanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
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
                        testScanTarget,
                        testBulkScanInfo,
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
                        testScanTarget,
                        testBulkScanInfo,
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
                        testScanTarget,
                        testBulkScanInfo,
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
                        testScanTarget,
                        testBulkScanInfo,
                        "db",
                        "collection",
                        JobStatus.TO_BE_EXECUTED);

        assertEquals(testBulkScanInfo, scanJobDescription.getBulkScanInfo());
    }

    @Test
    void testSerializable() {
        scanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
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
        BulkScan realBulkScan =
                new BulkScan(
                        ScanJobDescriptionTest.class,
                        ScanJobDescriptionTest.class,
                        "RealScan",
                        new TestScanConfig(),
                        System.currentTimeMillis(),
                        false,
                        null);
        realBulkScan.set_id("real-id");
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
                            testScanTarget, testBulkScanInfo, "db", "collection", status);
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
                        testScanTarget, testBulkScanInfo, "", "", JobStatus.TO_BE_EXECUTED);

        assertEquals("", scanJobDescription.getDbName());
        assertEquals("", scanJobDescription.getCollectionName());
    }

    @Test
    void testSetDeliveryTagWithNull() {
        scanJobDescription =
                new ScanJobDescription(
                        testScanTarget,
                        testBulkScanInfo,
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
