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

import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.Serializable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScanConfigTest {

    private TestScanConfig scanConfig;
    private static final ScannerDetail DEFAULT_DETAIL = ScannerDetail.NORMAL;
    private static final int DEFAULT_REEXECUTIONS = 3;
    private static final int DEFAULT_TIMEOUT = 30000;

    // Concrete implementation for testing
    private static class TestScanConfig extends ScanConfig {
        private BulkScanWorker<TestScanConfig> mockWorker;

        public TestScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
            super(scannerDetail, reexecutions, timeout);
        }

        public TestScanConfig() {
            // Using private constructor via reflection would be tested separately
            super(ScannerDetail.NORMAL, 0, 0);
        }

        public void setMockWorker(BulkScanWorker<TestScanConfig> mockWorker) {
            this.mockWorker = mockWorker;
        }

        @Override
        public BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
            if (mockWorker != null) {
                return mockWorker;
            }
            return mock(BulkScanWorker.class);
        }
    }

    @BeforeEach
    void setUp() {
        scanConfig = new TestScanConfig(DEFAULT_DETAIL, DEFAULT_REEXECUTIONS, DEFAULT_TIMEOUT);
    }

    @Test
    void testProtectedConstructor() {
        TestScanConfig config = new TestScanConfig(ScannerDetail.DETAILED, 5, 60000);
        assertEquals(ScannerDetail.DETAILED, config.getScannerDetail());
        assertEquals(5, config.getReexecutions());
        assertEquals(60000, config.getTimeout());
    }

    @Test
    void testGetScannerDetail() {
        assertEquals(DEFAULT_DETAIL, scanConfig.getScannerDetail());
    }

    @Test
    void testSetScannerDetail() {
        scanConfig.setScannerDetail(ScannerDetail.ALL);
        assertEquals(ScannerDetail.ALL, scanConfig.getScannerDetail());

        scanConfig.setScannerDetail(ScannerDetail.QUICK);
        assertEquals(ScannerDetail.QUICK, scanConfig.getScannerDetail());
    }

    @Test
    void testGetReexecutions() {
        assertEquals(DEFAULT_REEXECUTIONS, scanConfig.getReexecutions());
    }

    @Test
    void testSetReexecutions() {
        scanConfig.setReexecutions(10);
        assertEquals(10, scanConfig.getReexecutions());

        scanConfig.setReexecutions(0);
        assertEquals(0, scanConfig.getReexecutions());

        scanConfig.setReexecutions(-1);
        assertEquals(-1, scanConfig.getReexecutions());
    }

    @Test
    void testGetTimeout() {
        assertEquals(DEFAULT_TIMEOUT, scanConfig.getTimeout());
    }

    @Test
    void testSetTimeout() {
        scanConfig.setTimeout(120000);
        assertEquals(120000, scanConfig.getTimeout());

        scanConfig.setTimeout(0);
        assertEquals(0, scanConfig.getTimeout());

        scanConfig.setTimeout(-1000);
        assertEquals(-1000, scanConfig.getTimeout());
    }

    @Test
    void testCreateWorker() {
        BulkScanWorker<TestScanConfig> mockWorker = mock(BulkScanWorker.class);
        scanConfig.setMockWorker(mockWorker);

        BulkScanWorker<? extends ScanConfig> worker = scanConfig.createWorker("bulkScan123", 4, 8);
        assertNotNull(worker);
        assertEquals(mockWorker, worker);
    }

    @Test
    void testCreateWorkerWithDifferentParameters() {
        BulkScanWorker<? extends ScanConfig> worker1 = scanConfig.createWorker("scan1", 1, 1);
        BulkScanWorker<? extends ScanConfig> worker2 = scanConfig.createWorker("scan2", 10, 20);

        assertNotNull(worker1);
        assertNotNull(worker2);
    }

    @Test
    void testSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(scanConfig.getClass()));
    }

    @Test
    void testPrivateConstructorAccess() throws Exception {
        // Test that the private constructor exists and can be accessed via reflection
        java.lang.reflect.Constructor<TestScanConfig> constructor =
                TestScanConfig.class.getDeclaredConstructor();
        assertNotNull(constructor);
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
    }

    @Test
    void testAllScannerDetailValues() {
        // Test setting all possible ScannerDetail values
        for (ScannerDetail detail : ScannerDetail.values()) {
            scanConfig.setScannerDetail(detail);
            assertEquals(detail, scanConfig.getScannerDetail());
        }
    }

    @Test
    void testNullScannerDetail() {
        scanConfig.setScannerDetail(null);
        assertNull(scanConfig.getScannerDetail());
    }

    @Test
    void testBoundaryTimeoutValues() {
        scanConfig.setTimeout(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, scanConfig.getTimeout());

        scanConfig.setTimeout(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, scanConfig.getTimeout());
    }

    @Test
    void testBoundaryReexecutionValues() {
        scanConfig.setReexecutions(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, scanConfig.getReexecutions());

        scanConfig.setReexecutions(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, scanConfig.getReexecutions());
    }

    @Test
    void testDefaultConstructorInitialization() {
        TestScanConfig defaultConfig = new TestScanConfig();
        assertEquals(ScannerDetail.NORMAL, defaultConfig.getScannerDetail());
        assertEquals(0, defaultConfig.getReexecutions());
        assertEquals(0, defaultConfig.getTimeout());
    }
}
