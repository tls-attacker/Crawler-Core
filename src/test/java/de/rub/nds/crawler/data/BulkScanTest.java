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
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import javax.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BulkScanTest {

    private ScanConfig testScanConfig;

    private BulkScan bulkScan;
    private static final long TEST_START_TIME = 1640995200000L; // 2022-01-01 00:00:00 UTC
    private static final String TEST_NAME = "TestScan";
    private static final String TEST_NOTIFY_URL = "https://example.com/notify";

    // Test classes for version extraction
    static class TestScannerClass {
        // Mock scanner class
    }

    static class TestCrawlerClass {
        // Mock crawler class
    }

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
        testScanConfig = new TestScanConfig();

        // Create a bulkScan with the main constructor
        bulkScan =
                new BulkScan(
                        TestScannerClass.class,
                        TestCrawlerClass.class,
                        TEST_NAME,
                        testScanConfig,
                        TEST_START_TIME,
                        true,
                        TEST_NOTIFY_URL);
    }

    @Test
    void testMainConstructor() {
        assertNotNull(bulkScan);
        assertEquals(TEST_NAME, bulkScan.getName());
        assertEquals(testScanConfig, bulkScan.getScanConfig());
        assertEquals(TEST_START_TIME, bulkScan.getStartTime());
        assertTrue(bulkScan.isMonitored());
        assertFalse(bulkScan.isFinished());
        assertEquals(TEST_NOTIFY_URL, bulkScan.getNotifyUrl());

        // Test collection name generation
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String expectedCollectionName =
                TEST_NAME
                        + "_"
                        + dateFormat.format(Date.from(Instant.ofEpochMilli(TEST_START_TIME)));
        assertEquals(expectedCollectionName, bulkScan.getCollectionName());

        // Version extraction from packages (will be null in test environment)
        assertNull(bulkScan.getScannerVersion());
        assertNull(bulkScan.getCrawlerVersion());
    }

    @Test
    void testCollectionNameFormat() {
        // Test with different timestamps
        long timestamp1 = 1640995200000L; // 2022-01-01 00:00:00 UTC
        long timestamp2 = 1672531200000L; // 2023-01-01 00:00:00 UTC

        BulkScan scan1 =
                new BulkScan(
                        TestScannerClass.class,
                        TestCrawlerClass.class,
                        "Scan1",
                        testScanConfig,
                        timestamp1,
                        false,
                        null);

        BulkScan scan2 =
                new BulkScan(
                        TestScannerClass.class,
                        TestCrawlerClass.class,
                        "Scan2",
                        testScanConfig,
                        timestamp2,
                        false,
                        null);

        assertTrue(scan1.getCollectionName().contains("Scan1"));
        assertTrue(scan2.getCollectionName().contains("Scan2"));
        assertTrue(scan1.getCollectionName().contains("2022-01-01"));
        assertTrue(scan2.getCollectionName().contains("2023-01-01"));
    }

    @Test
    void testGetAndSetId() {
        assertNull(bulkScan.get_id());

        bulkScan.set_id("bulk-scan-12345");
        assertEquals("bulk-scan-12345", bulkScan.get_id());
    }

    @Test
    void testGetAndSetName() {
        assertEquals(TEST_NAME, bulkScan.getName());

        bulkScan.setName("NewName");
        assertEquals("NewName", bulkScan.getName());
    }

    @Test
    void testGetAndSetCollectionName() {
        String originalName = bulkScan.getCollectionName();
        assertNotNull(originalName);

        bulkScan.setCollectionName("custom_collection");
        assertEquals("custom_collection", bulkScan.getCollectionName());
    }

    @Test
    void testGetAndSetScanConfig() {
        assertEquals(testScanConfig, bulkScan.getScanConfig());

        ScanConfig newConfig = new TestScanConfig();
        bulkScan.setScanConfig(newConfig);
        assertEquals(newConfig, bulkScan.getScanConfig());
    }

    @Test
    void testGetAndSetMonitored() {
        assertTrue(bulkScan.isMonitored());

        bulkScan.setMonitored(false);
        assertFalse(bulkScan.isMonitored());
    }

    @Test
    void testGetAndSetFinished() {
        assertFalse(bulkScan.isFinished());

        bulkScan.setFinished(true);
        assertTrue(bulkScan.isFinished());
    }

    @Test
    void testGetAndSetStartTime() {
        assertEquals(TEST_START_TIME, bulkScan.getStartTime());

        long newTime = 1672531200000L;
        bulkScan.setStartTime(newTime);
        assertEquals(newTime, bulkScan.getStartTime());
    }

    @Test
    void testGetAndSetEndTime() {
        assertEquals(0, bulkScan.getEndTime());

        long endTime = 1640998800000L;
        bulkScan.setEndTime(endTime);
        assertEquals(endTime, bulkScan.getEndTime());
    }

    @Test
    void testGetAndSetTargetsGiven() {
        assertEquals(0, bulkScan.getTargetsGiven());

        bulkScan.setTargetsGiven(1000);
        assertEquals(1000, bulkScan.getTargetsGiven());
    }

    @Test
    void testGetAndSetScanJobsPublished() {
        assertEquals(0, bulkScan.getScanJobsPublished());

        bulkScan.setScanJobsPublished(500L);
        assertEquals(500L, bulkScan.getScanJobsPublished());
    }

    @Test
    void testGetAndSetSuccessfulScans() {
        assertEquals(0, bulkScan.getSuccessfulScans());

        bulkScan.setSuccessfulScans(250);
        assertEquals(250, bulkScan.getSuccessfulScans());
    }

    @Test
    void testGetAndSetNotifyUrl() {
        assertEquals(TEST_NOTIFY_URL, bulkScan.getNotifyUrl());

        bulkScan.setNotifyUrl("https://new.example.com");
        assertEquals("https://new.example.com", bulkScan.getNotifyUrl());
    }

    @Test
    void testGetAndSetScannerVersion() {
        assertNull(bulkScan.getScannerVersion());

        bulkScan.setScannerVersion("1.2.3");
        assertEquals("1.2.3", bulkScan.getScannerVersion());
    }

    @Test
    void testGetAndSetCrawlerVersion() {
        assertNull(bulkScan.getCrawlerVersion());

        bulkScan.setCrawlerVersion("2.3.4");
        assertEquals("2.3.4", bulkScan.getCrawlerVersion());
    }

    @Test
    void testGetAndSetJobStatusCounters() {
        Map<JobStatus, Integer> counters = bulkScan.getJobStatusCounters();
        assertNotNull(counters);
        assertTrue(counters instanceof EnumMap);
        assertTrue(counters.isEmpty());

        Map<JobStatus, Integer> newCounters = new EnumMap<>(JobStatus.class);
        newCounters.put(JobStatus.SUCCESS, 100);
        newCounters.put(JobStatus.ERROR, 10);

        bulkScan.setJobStatusCounters(newCounters);
        assertEquals(newCounters, bulkScan.getJobStatusCounters());
        assertEquals(100, bulkScan.getJobStatusCounters().get(JobStatus.SUCCESS));
        assertEquals(10, bulkScan.getJobStatusCounters().get(JobStatus.ERROR));
    }

    @Test
    void testGetAndSetScanJobsResolutionErrors() {
        assertEquals(0, bulkScan.getScanJobsResolutionErrors());

        bulkScan.setScanJobsResolutionErrors(25L);
        assertEquals(25L, bulkScan.getScanJobsResolutionErrors());
    }

    @Test
    void testGetAndSetScanJobsDenylisted() {
        assertEquals(0, bulkScan.getScanJobsDenylisted());

        bulkScan.setScanJobsDenylisted(15L);
        assertEquals(15L, bulkScan.getScanJobsDenylisted());
    }

    @Test
    void testSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(bulkScan.getClass()));
    }

    @Test
    void testIdAnnotation() throws NoSuchFieldException {
        assertTrue(BulkScan.class.getDeclaredField("_id").isAnnotationPresent(Id.class));
    }

    @Test
    void testConstructorWithNullValues() {
        BulkScan scanWithNulls =
                new BulkScan(
                        TestScannerClass.class,
                        TestCrawlerClass.class,
                        null,
                        null,
                        TEST_START_TIME,
                        false,
                        null);

        assertNull(scanWithNulls.getName());
        assertNull(scanWithNulls.getScanConfig());
        assertNull(scanWithNulls.getNotifyUrl());
        assertFalse(scanWithNulls.isMonitored());

        // Collection name should still be generated
        assertNotNull(scanWithNulls.getCollectionName());
        assertTrue(scanWithNulls.getCollectionName().startsWith("null_"));
    }

    @Test
    void testBoundaryValues() {
        bulkScan.setStartTime(0L);
        assertEquals(0L, bulkScan.getStartTime());

        bulkScan.setStartTime(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, bulkScan.getStartTime());

        bulkScan.setTargetsGiven(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, bulkScan.getTargetsGiven());

        bulkScan.setScanJobsPublished(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, bulkScan.getScanJobsPublished());
    }

    @Test
    void testEmptyStrings() {
        bulkScan.set_id("");
        assertEquals("", bulkScan.get_id());

        bulkScan.setName("");
        assertEquals("", bulkScan.getName());

        bulkScan.setCollectionName("");
        assertEquals("", bulkScan.getCollectionName());

        bulkScan.setNotifyUrl("");
        assertEquals("", bulkScan.getNotifyUrl());
    }

    @Test
    void testStaticDateFormatField() throws NoSuchFieldException {
        assertTrue(
                java.lang.reflect.Modifier.isStatic(
                        BulkScan.class.getDeclaredField("dateFormat").getModifiers()));
    }

    @Test
    void testPrivateConstructor() {
        // Verify the private constructor exists
        java.lang.reflect.Constructor<?>[] constructors = BulkScan.class.getDeclaredConstructors();
        boolean hasPrivateConstructor = false;

        for (java.lang.reflect.Constructor<?> constructor : constructors) {
            if (java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())
                    && constructor.getParameterCount() == 0) {
                hasPrivateConstructor = true;
                break;
            }
        }

        assertTrue(hasPrivateConstructor);
    }

    @Test
    void testAllFieldsInitialization() {
        // Test that all numeric fields are initialized to 0
        BulkScan newScan =
                new BulkScan(
                        TestScannerClass.class,
                        TestCrawlerClass.class,
                        "Test",
                        testScanConfig,
                        TEST_START_TIME,
                        false,
                        null);

        assertEquals(0, newScan.getEndTime());
        assertEquals(0, newScan.getTargetsGiven());
        assertEquals(0, newScan.getScanJobsPublished());
        assertEquals(0, newScan.getSuccessfulScans());
        assertEquals(0, newScan.getScanJobsResolutionErrors());
        assertEquals(0, newScan.getScanJobsDenylisted());
        assertNull(newScan.get_id());
    }
}
