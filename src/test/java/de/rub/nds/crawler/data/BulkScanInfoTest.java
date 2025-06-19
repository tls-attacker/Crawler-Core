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

import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.Serializable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BulkScanInfoTest {

    private BulkScan testBulkScan;

    private ScanConfig testScanConfig;

    private BulkScanInfo bulkScanInfo;

    // Test implementation of ScanConfig for type-safe testing
    private static class TestScanConfig extends ScanConfig {
        private String testField;

        public TestScanConfig(String testField) {
            super(ScannerDetail.NORMAL, 1, 1000);
            this.testField = testField;
        }

        public String getTestField() {
            return testField;
        }

        @Override
        public de.rub.nds.crawler.core.BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
            return null;
        }
    }

    // Another test implementation to test wrong cast
    private static class AnotherTestScanConfig extends ScanConfig {
        public AnotherTestScanConfig() {
            super(ScannerDetail.QUICK, 0, 500);
        }

        @Override
        public de.rub.nds.crawler.core.BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
            return null;
        }
    }

    // Basic test implementation of ScanConfig
    private static class BasicTestScanConfig extends ScanConfig {
        public BasicTestScanConfig() {
            super(ScannerDetail.NORMAL, 1, 1000);
        }

        @Override
        public de.rub.nds.crawler.core.BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
            return null;
        }
    }

    @BeforeEach
    void setUp() {
        testScanConfig = new BasicTestScanConfig();

        testBulkScan =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "TestScan",
                        testScanConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        testBulkScan.set_id("bulk-scan-123");

        bulkScanInfo = new BulkScanInfo(testBulkScan);
    }

    @Test
    void testConstructor() {
        assertNotNull(bulkScanInfo);
        assertEquals("bulk-scan-123", bulkScanInfo.getBulkScanId());
        assertEquals(testScanConfig, bulkScanInfo.getScanConfig());
        assertTrue(bulkScanInfo.isMonitored());
    }

    @Test
    void testConstructorWithDifferentValues() {
        BulkScan anotherBulkScan =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "AnotherTestScan",
                        testScanConfig,
                        System.currentTimeMillis(),
                        false,
                        null);
        anotherBulkScan.set_id("another-scan-456");

        BulkScanInfo anotherInfo = new BulkScanInfo(anotherBulkScan);

        assertEquals("another-scan-456", anotherInfo.getBulkScanId());
        assertEquals(testScanConfig, anotherInfo.getScanConfig());
        assertFalse(anotherInfo.isMonitored());
    }

    @Test
    void testGetBulkScanId() {
        assertEquals("bulk-scan-123", bulkScanInfo.getBulkScanId());
    }

    @Test
    void testGetScanConfig() {
        assertEquals(testScanConfig, bulkScanInfo.getScanConfig());
    }

    @Test
    void testGetScanConfigWithClass() {
        TestScanConfig testConfig = new TestScanConfig("test-value");
        BulkScan bulkScanWithTestConfig =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "TestScan",
                        testConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScanWithTestConfig.set_id("test-scan-id");

        BulkScanInfo infoWithTestConfig = new BulkScanInfo(bulkScanWithTestConfig);

        // Test type-safe getter
        TestScanConfig retrievedConfig = infoWithTestConfig.getScanConfig(TestScanConfig.class);
        assertNotNull(retrievedConfig);
        assertEquals(testConfig, retrievedConfig);
        assertEquals("test-value", retrievedConfig.getTestField());
    }

    @Test
    void testGetScanConfigWithWrongClass() {
        TestScanConfig testConfig = new TestScanConfig("test-value");
        BulkScan bulkScanWithTestConfig =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "TestScan",
                        testConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScanWithTestConfig.set_id("test-scan-id");

        BulkScanInfo infoWithTestConfig = new BulkScanInfo(bulkScanWithTestConfig);

        // Test ClassCastException when casting to wrong type
        assertThrows(
                ClassCastException.class,
                () -> {
                    infoWithTestConfig.getScanConfig(AnotherTestScanConfig.class);
                });
    }

    @Test
    void testIsMonitored() {
        assertTrue(bulkScanInfo.isMonitored());
    }

    @Test
    void testIsMonitoredFalse() {
        BulkScan unmonitoredBulkScan =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "UnmonitoredScan",
                        testScanConfig,
                        System.currentTimeMillis(),
                        false,
                        null);
        unmonitoredBulkScan.set_id("unmonitored-scan-id");

        BulkScanInfo unmonitoredInfo = new BulkScanInfo(unmonitoredBulkScan);
        assertFalse(unmonitoredInfo.isMonitored());
    }

    @Test
    void testSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(bulkScanInfo.getClass()));
    }

    @Test
    void testFieldsAreFinal() throws NoSuchFieldException {
        // Verify fields are final (immutable)
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        BulkScanInfo.class.getDeclaredField("bulkScanId").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        BulkScanInfo.class.getDeclaredField("scanConfig").getModifiers()));
        assertTrue(
                java.lang.reflect.Modifier.isFinal(
                        BulkScanInfo.class.getDeclaredField("isMonitored").getModifiers()));
    }

    @Test
    void testNullBulkScanId() {
        BulkScan bulkScanWithNullId =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "NullIdScan",
                        testScanConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        // Don't set ID, leaving it null

        BulkScanInfo infoWithNullId = new BulkScanInfo(bulkScanWithNullId);
        assertNull(infoWithNullId.getBulkScanId());
    }

    @Test
    void testNullScanConfig() {
        BulkScan bulkScanWithNullConfig =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "NullConfigScan",
                        null,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScanWithNullConfig.set_id("null-config-scan-id");

        BulkScanInfo infoWithNullConfig = new BulkScanInfo(bulkScanWithNullConfig);
        assertNull(infoWithNullConfig.getScanConfig());
    }

    @Test
    void testGetScanConfigWithClassOnNull() {
        BulkScan bulkScanWithNullConfig =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "NullConfigScan",
                        null,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScanWithNullConfig.set_id("null-config-scan-id");

        BulkScanInfo infoWithNullConfig = new BulkScanInfo(bulkScanWithNullConfig);

        // The cast method will return null when called on null
        TestScanConfig result = infoWithNullConfig.getScanConfig(TestScanConfig.class);
        assertNull(result);
    }

    @Test
    void testMultipleCallsReturnSameValues() {
        // Test that the values don't change (immutability)
        String id1 = bulkScanInfo.getBulkScanId();
        String id2 = bulkScanInfo.getBulkScanId();
        ScanConfig config1 = bulkScanInfo.getScanConfig();
        ScanConfig config2 = bulkScanInfo.getScanConfig();
        boolean monitored1 = bulkScanInfo.isMonitored();
        boolean monitored2 = bulkScanInfo.isMonitored();

        assertEquals(id1, id2);
        assertSame(config1, config2);
        assertEquals(monitored1, monitored2);
    }

    @Test
    void testEmptyBulkScanId() {
        BulkScan bulkScanWithEmptyId =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "EmptyIdScan",
                        testScanConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScanWithEmptyId.set_id("");

        BulkScanInfo infoWithEmptyId = new BulkScanInfo(bulkScanWithEmptyId);
        assertEquals("", infoWithEmptyId.getBulkScanId());
    }

    @Test
    void testGetScanConfigGenericCast() {
        // Test that the generic method properly preserves type
        TestScanConfig testConfig = new TestScanConfig("generic-test");
        BulkScan bulkScanWithTestConfig =
                new BulkScan(
                        BulkScanInfoTest.class,
                        BulkScanInfoTest.class,
                        "GenericTestScan",
                        testConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScanWithTestConfig.set_id("generic-test-scan-id");

        BulkScanInfo info = new BulkScanInfo(bulkScanWithTestConfig);

        // This should compile and work without explicit cast
        TestScanConfig retrieved = info.getScanConfig(TestScanConfig.class);
        assertEquals("generic-test", retrieved.getTestField());
    }
}
