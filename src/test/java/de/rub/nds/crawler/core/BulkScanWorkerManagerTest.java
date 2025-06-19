/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.data.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import org.apache.commons.lang3.exception.UncheckedException;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.mockito.*;

class BulkScanWorkerManagerTest {

    private BulkScanWorker<?> mockBulkScanWorker;
    @Mock private ScanConfig mockScanConfig;
    @Mock private Future<Document> mockFuture;

    private BulkScanWorkerManager manager;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockBulkScanWorker = mock(BulkScanWorker.class);

        // Reset singleton instance
        Field instanceField = BulkScanWorkerManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        manager = BulkScanWorkerManager.getInstance();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Reset singleton instance after each test
        Field instanceField = BulkScanWorkerManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testGetInstance() {
        BulkScanWorkerManager instance1 = BulkScanWorkerManager.getInstance();
        BulkScanWorkerManager instance2 = BulkScanWorkerManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testGetBulkScanWorkerCreatesNewWorker() {
        // Given
        String bulkScanId = "test-scan-id";
        int parallelConnectionThreads = 10;
        int parallelScanThreads = 2;

        // Use doReturn to avoid generic issues
        mockBulkScanWorker = mock(BulkScanWorker.class);
        doReturn(mockBulkScanWorker)
                .when(mockScanConfig)
                .createWorker(bulkScanId, parallelConnectionThreads, parallelScanThreads);

        // When
        BulkScanWorker<?> worker =
                manager.getBulkScanWorker(
                        bulkScanId, mockScanConfig, parallelConnectionThreads, parallelScanThreads);

        // Then
        assertSame(mockBulkScanWorker, worker);
        verify(mockBulkScanWorker).init();
        verify(mockScanConfig)
                .createWorker(bulkScanId, parallelConnectionThreads, parallelScanThreads);
    }

    @Test
    void testGetBulkScanWorkerReturnsCachedWorker() {
        // Given
        String bulkScanId = "test-scan-id";
        int parallelConnectionThreads = 10;
        int parallelScanThreads = 2;

        // Use doReturn to avoid generic issues
        mockBulkScanWorker = mock(BulkScanWorker.class);
        doReturn(mockBulkScanWorker)
                .when(mockScanConfig)
                .createWorker(bulkScanId, parallelConnectionThreads, parallelScanThreads);

        // When - get worker twice
        BulkScanWorker<?> worker1 =
                manager.getBulkScanWorker(
                        bulkScanId, mockScanConfig, parallelConnectionThreads, parallelScanThreads);
        BulkScanWorker<?> worker2 =
                manager.getBulkScanWorker(
                        bulkScanId, mockScanConfig, parallelConnectionThreads, parallelScanThreads);

        // Then
        assertSame(worker1, worker2);
        assertSame(mockBulkScanWorker, worker1);
        // Should only create and init once
        verify(mockScanConfig, times(1))
                .createWorker(bulkScanId, parallelConnectionThreads, parallelScanThreads);
        verify(mockBulkScanWorker, times(1)).init();
    }

    @Test
    void testGetBulkScanWorkerThrowsExceptionOnCreationFailure() {
        // Given
        String bulkScanId = "test-scan-id";
        int parallelConnectionThreads = 10;
        int parallelScanThreads = 2;

        when(mockScanConfig.createWorker(
                        bulkScanId, parallelConnectionThreads, parallelScanThreads))
                .thenThrow(new RuntimeException("Creation failed"));

        // When/Then
        assertThrows(
                UncheckedException.class,
                () ->
                        manager.getBulkScanWorker(
                                bulkScanId,
                                mockScanConfig,
                                parallelConnectionThreads,
                                parallelScanThreads));
    }

    @Test
    void testHandle() {
        // Given
        String bulkScanId = "test-scan-id";
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScanInfo bulkScanInfo = mock(BulkScanInfo.class);
        ScanJobDescription scanJobDescription = mock(ScanJobDescription.class);

        when(scanJobDescription.getBulkScanInfo()).thenReturn(bulkScanInfo);
        when(scanJobDescription.getScanTarget()).thenReturn(scanTarget);
        when(bulkScanInfo.getBulkScanId()).thenReturn(bulkScanId);
        when(bulkScanInfo.getScanConfig()).thenReturn(mockScanConfig);
        mockBulkScanWorker = mock(BulkScanWorker.class);
        doReturn(mockBulkScanWorker)
                .when(mockScanConfig)
                .createWorker(anyString(), anyInt(), anyInt());
        when(mockBulkScanWorker.handle(scanTarget)).thenReturn(mockFuture);

        // When
        Future<Document> result = manager.handle(scanJobDescription, 10, 2);

        // Then
        assertSame(mockFuture, result);
        verify(mockBulkScanWorker).handle(scanTarget);
    }

    @Test
    void testHandleStatic() {
        // Given
        String bulkScanId = "test-scan-id";
        ScanTarget scanTarget = new ScanTarget();
        scanTarget.setHostname("example.com");
        scanTarget.setPort(443);
        scanTarget.setIp("192.0.2.1");
        BulkScanInfo bulkScanInfo = mock(BulkScanInfo.class);
        ScanJobDescription scanJobDescription = mock(ScanJobDescription.class);

        when(scanJobDescription.getBulkScanInfo()).thenReturn(bulkScanInfo);
        when(scanJobDescription.getScanTarget()).thenReturn(scanTarget);
        when(bulkScanInfo.getBulkScanId()).thenReturn(bulkScanId);
        when(bulkScanInfo.getScanConfig()).thenReturn(mockScanConfig);
        mockBulkScanWorker = mock(BulkScanWorker.class);
        doReturn(mockBulkScanWorker)
                .when(mockScanConfig)
                .createWorker(anyString(), anyInt(), anyInt());
        when(mockBulkScanWorker.handle(scanTarget)).thenReturn(mockFuture);

        // When
        Future<Document> result = BulkScanWorkerManager.handleStatic(scanJobDescription, 10, 2);

        // Then
        assertSame(mockFuture, result);
        verify(mockBulkScanWorker).handle(scanTarget);
    }

    @Test
    void testCacheEvictionCallsCleanup() throws Exception {
        // Given
        String bulkScanId = "test-scan-id";

        mockBulkScanWorker = mock(BulkScanWorker.class);
        doReturn(mockBulkScanWorker)
                .when(mockScanConfig)
                .createWorker(anyString(), anyInt(), anyInt());

        // Get the cache field via reflection
        Field cacheField = BulkScanWorkerManager.class.getDeclaredField("bulkScanWorkers");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        com.google.common.cache.Cache<String, BulkScanWorker<?>> cache =
                (com.google.common.cache.Cache<String, BulkScanWorker<?>>) cacheField.get(manager);

        // When - add worker and then invalidate
        manager.getBulkScanWorker(bulkScanId, mockScanConfig, 10, 2);
        cache.invalidate(bulkScanId);

        // Give some time for async removal listener
        Thread.sleep(100);

        // Then
        verify(mockBulkScanWorker).cleanup();
    }

    @Test
    void testMultipleBulkScansHaveSeparateWorkers() {
        // Given
        String bulkScanId1 = "scan-1";
        String bulkScanId2 = "scan-2";

        // Create separate mocks for each worker
        BulkScanWorker<?> worker1 = mock(BulkScanWorker.class);
        BulkScanWorker<?> worker2 = mock(BulkScanWorker.class);

        doReturn(worker1).when(mockScanConfig).createWorker(eq(bulkScanId1), anyInt(), anyInt());
        doReturn(worker2).when(mockScanConfig).createWorker(eq(bulkScanId2), anyInt(), anyInt());

        // When
        BulkScanWorker<?> result1 = manager.getBulkScanWorker(bulkScanId1, mockScanConfig, 10, 2);
        BulkScanWorker<?> result2 = manager.getBulkScanWorker(bulkScanId2, mockScanConfig, 10, 2);

        // Then
        assertNotSame(result1, result2);
        assertSame(worker1, result1);
        assertSame(worker2, result2);
        verify(worker1).init();
        verify(worker2).init();
    }
}
