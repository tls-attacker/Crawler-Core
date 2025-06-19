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
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.orchestration.ScanJobConsumer;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.constants.ScannerDetail;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WorkerTest {

    @Mock private WorkerCommandConfig config;

    @Mock private IOrchestrationProvider orchestrationProvider;

    @Mock private IPersistenceProvider persistenceProvider;

    @Mock private BulkScanWorkerManager bulkScanWorkerManager;

    private Worker worker;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        worker = new Worker(config, orchestrationProvider, persistenceProvider);
        worker.bulkScanWorkerManager = bulkScanWorkerManager;
    }

    @Test
    void testStart() throws Exception {
        ArgumentCaptor<ScanJobConsumer> consumerCaptor =
                ArgumentCaptor.forClass(ScanJobConsumer.class);

        worker.start();

        verify(orchestrationProvider).registerJobConsumer(consumerCaptor.capture());

        ScanJobConsumer registeredConsumer = consumerCaptor.getValue();
        assertNotNull(registeredConsumer);
    }

    @Test
    void testHandleScanJobSuccess() throws Exception {
        ScanJobDescription job = createTestScanJobDescription();
        ScanResult expectedResult = createSuccessfulScanResult(job.getScanTarget());
        CompletableFuture<ScanResult> future = CompletableFuture.completedFuture(expectedResult);

        when(bulkScanWorkerManager.handle(any(), eq(job))).thenReturn(future);

        String jobJson = objectMapper.writeValueAsString(job);

        worker.handleScanJob("delivery-tag-123", jobJson);

        // Allow async processing to complete
        Thread.sleep(100);

        verify(persistenceProvider).saveScanResult(expectedResult);
        verify(orchestrationProvider).ackJob("delivery-tag-123");
        verify(orchestrationProvider).sendDoneNotification(job.getBulkScan(), expectedResult);
    }

    @Test
    void testHandleScanJobTimeout() throws Exception {
        ScanJobDescription job = createTestScanJobDescription();
        CompletableFuture<ScanResult> future = new CompletableFuture<>();

        when(bulkScanWorkerManager.handle(any(), eq(job))).thenReturn(future);
        when(config.getScanTimeout()).thenReturn(100); // 100ms timeout

        String jobJson = objectMapper.writeValueAsString(job);

        worker.handleScanJob("delivery-tag-123", jobJson);

        // Wait for timeout
        Thread.sleep(200);

        ArgumentCaptor<ScanResult> resultCaptor = ArgumentCaptor.forClass(ScanResult.class);
        verify(persistenceProvider).saveScanResult(resultCaptor.capture());

        ScanResult savedResult = resultCaptor.getValue();
        assertEquals(JobStatus.TIMEOUT, savedResult.getStatus());

        verify(orchestrationProvider).ackJob("delivery-tag-123");
        verify(orchestrationProvider)
                .sendDoneNotification(eq(job.getBulkScan()), any(ScanResult.class));
    }

    @Test
    void testHandleScanJobExecutionException() throws Exception {
        ScanJobDescription job = createTestScanJobDescription();
        CompletableFuture<ScanResult> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Scan failed"));

        when(bulkScanWorkerManager.handle(any(), eq(job))).thenReturn(future);

        String jobJson = objectMapper.writeValueAsString(job);

        worker.handleScanJob("delivery-tag-123", jobJson);

        // Allow async processing to complete
        Thread.sleep(100);

        ArgumentCaptor<ScanResult> resultCaptor = ArgumentCaptor.forClass(ScanResult.class);
        verify(persistenceProvider).saveScanResult(resultCaptor.capture());

        ScanResult savedResult = resultCaptor.getValue();
        assertEquals(JobStatus.ERROR, savedResult.getStatus());
        assertTrue(
                savedResult
                        .getScanDetails()
                        .get("errorMessage")
                        .toString()
                        .contains("Scan failed"));

        verify(orchestrationProvider).ackJob("delivery-tag-123");
        verify(orchestrationProvider)
                .sendDoneNotification(eq(job.getBulkScan()), any(ScanResult.class));
    }

    @Test
    void testHandleScanJobInterruption() throws Exception {
        ScanJobDescription job = createTestScanJobDescription();
        CompletableFuture<ScanResult> future = new CompletableFuture<>();

        when(bulkScanWorkerManager.handle(any(), eq(job))).thenReturn(future);
        when(config.getScanTimeout()).thenReturn(1000);

        String jobJson = objectMapper.writeValueAsString(job);

        // Interrupt the thread after starting the job
        Thread workerThread =
                new Thread(
                        () -> {
                            try {
                                worker.handleScanJob("delivery-tag-123", jobJson);
                            } catch (Exception e) {
                                // Expected
                            }
                        });

        workerThread.start();
        Thread.sleep(50);
        workerThread.interrupt();
        workerThread.join();

        // Verify job was acknowledged even after interruption
        verify(orchestrationProvider).ackJob("delivery-tag-123");
    }

    @Test
    void testHandleScanJobInvalidJson() throws Exception {
        String invalidJson = "{ invalid json }";

        worker.handleScanJob("delivery-tag-123", invalidJson);

        // Should still acknowledge the job even if JSON parsing fails
        verify(orchestrationProvider).ackJob("delivery-tag-123");

        // Should not attempt to save result or send notification
        verify(persistenceProvider, never()).saveScanResult(any());
        verify(orchestrationProvider, never()).sendDoneNotification(any(), any());
    }

    @Test
    void testPersistResultWithException() throws Exception {
        ScanJobDescription job = createTestScanJobDescription();
        ScanResult result = createSuccessfulScanResult(job.getScanTarget());
        CompletableFuture<ScanResult> future = CompletableFuture.completedFuture(result);

        when(bulkScanWorkerManager.handle(any(), eq(job))).thenReturn(future);
        doThrow(new RuntimeException("Database error"))
                .when(persistenceProvider)
                .saveScanResult(any());

        String jobJson = objectMapper.writeValueAsString(job);

        worker.handleScanJob("delivery-tag-123", jobJson);

        // Allow async processing to complete
        Thread.sleep(100);

        // Should still acknowledge and send notification even if persistence fails
        verify(orchestrationProvider).ackJob("delivery-tag-123");
        verify(orchestrationProvider).sendDoneNotification(job.getBulkScan(), result);
    }

    @Test
    void testWaitForScanResultCancellation() throws Exception {
        ScanJobDescription job = createTestScanJobDescription();
        CompletableFuture<ScanResult> future = new CompletableFuture<>();

        when(bulkScanWorkerManager.handle(any(), eq(job))).thenReturn(future);
        when(config.getScanTimeout()).thenReturn(100);

        String jobJson = objectMapper.writeValueAsString(job);

        worker.handleScanJob("delivery-tag-123", jobJson);

        // Wait for timeout and cancellation
        Thread.sleep(200);

        // Future should be cancelled
        assertTrue(future.isCancelled());
    }

    private ScanJobDescription createTestScanJobDescription() {
        BulkScan bulkScan = new BulkScan();
        bulkScan.setId("test-bulk-scan");
        bulkScan.setScanConfig(new ScanConfig(ScannerDetail.NORMAL, 1, 1));
        bulkScan.setStartTime(ZonedDateTime.now());

        ScanTarget target = new ScanTarget("example.com", 443);

        return new ScanJobDescription(bulkScan, target);
    }

    private ScanResult createSuccessfulScanResult(ScanTarget target) {
        Map<String, Object> details = new HashMap<>();
        details.put("test", true);
        details.put("scanSuccessful", true);

        return new ScanResult(target, ZonedDateTime.now(), JobStatus.SUCCESS, details);
    }
}
