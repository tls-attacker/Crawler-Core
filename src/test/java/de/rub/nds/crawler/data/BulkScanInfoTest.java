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

import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.Serializable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkScanInfoTest {

    @Mock private BulkScan mockBulkScan;

    @Mock private ScanConfig mockScanConfig;

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

    @BeforeEach
    void setUp() {
        when(mockBulkScan.get_id()).thenReturn("bulk-scan-123");
        when(mockBulkScan.getScanConfig()).thenReturn(mockScanConfig);
        when(mockBulkScan.isMonitored()).thenReturn(true);

        bulkScanInfo = new BulkScanInfo(mockBulkScan);
    }

    @Test
    void testConstructor() {
        assertNotNull(bulkScanInfo);
        assertEquals("bulk-scan-123", bulkScanInfo.getBulkScanId());
        assertEquals(mockScanConfig, bulkScanInfo.getScanConfig());
        assertTrue(bulkScanInfo.isMonitored());

        // Verify mock interactions
        verify(mockBulkScan).get_id();
        verify(mockBulkScan).getScanConfig();
        verify(mockBulkScan).isMonitored();
    }

    @Test
    void testConstructorWithDifferentValues() {
        when(mockBulkScan.get_id()).thenReturn("another-scan-456");
        when(mockBulkScan.isMonitored()).thenReturn(false);

        BulkScanInfo anotherInfo = new BulkScanInfo(mockBulkScan);

        assertEquals("another-scan-456", anotherInfo.getBulkScanId());
        assertEquals(mockScanConfig, anotherInfo.getScanConfig());
        assertFalse(anotherInfo.isMonitored());
    }

    @Test
    void testGetBulkScanId() {
        assertEquals("bulk-scan-123", bulkScanInfo.getBulkScanId());
    }

    @Test
    void testGetScanConfig() {
        assertEquals(mockScanConfig, bulkScanInfo.getScanConfig());
    }

    @Test
    void testGetScanConfigWithClass() {
        TestScanConfig testConfig = new TestScanConfig("test-value");
        when(mockBulkScan.getScanConfig()).thenReturn(testConfig);

        BulkScanInfo infoWithTestConfig = new BulkScanInfo(mockBulkScan);

        // Test type-safe getter
        TestScanConfig retrievedConfig = infoWithTestConfig.getScanConfig(TestScanConfig.class);
        assertNotNull(retrievedConfig);
        assertEquals(testConfig, retrievedConfig);
        assertEquals("test-value", retrievedConfig.getTestField());
    }

    @Test
    void testGetScanConfigWithWrongClass() {
        TestScanConfig testConfig = new TestScanConfig("test-value");
        when(mockBulkScan.getScanConfig()).thenReturn(testConfig);

        BulkScanInfo infoWithTestConfig = new BulkScanInfo(mockBulkScan);

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
        when(mockBulkScan.isMonitored()).thenReturn(false);
        BulkScanInfo unmonitoredInfo = new BulkScanInfo(mockBulkScan);
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
        when(mockBulkScan.get_id()).thenReturn(null);
        BulkScanInfo infoWithNullId = new BulkScanInfo(mockBulkScan);
        assertNull(infoWithNullId.getBulkScanId());
    }

    @Test
    void testNullScanConfig() {
        when(mockBulkScan.getScanConfig()).thenReturn(null);
        BulkScanInfo infoWithNullConfig = new BulkScanInfo(mockBulkScan);
        assertNull(infoWithNullConfig.getScanConfig());
    }

    @Test
    void testGetScanConfigWithClassOnNull() {
        when(mockBulkScan.getScanConfig()).thenReturn(null);
        BulkScanInfo infoWithNullConfig = new BulkScanInfo(mockBulkScan);

        assertThrows(
                NullPointerException.class,
                () -> {
                    infoWithNullConfig.getScanConfig(TestScanConfig.class);
                });
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
        when(mockBulkScan.get_id()).thenReturn("");
        BulkScanInfo infoWithEmptyId = new BulkScanInfo(mockBulkScan);
        assertEquals("", infoWithEmptyId.getBulkScanId());
    }

    @Test
    void testGetScanConfigGenericCast() {
        // Test that the generic method properly preserves type
        TestScanConfig testConfig = new TestScanConfig("generic-test");
        when(mockBulkScan.getScanConfig()).thenReturn(testConfig);

        BulkScanInfo info = new BulkScanInfo(mockBulkScan);

        // This should compile and work without explicit cast
        TestScanConfig retrieved = info.getScanConfig(TestScanConfig.class);
        assertEquals("generic-test", retrieved.getTestField());
    }
}
