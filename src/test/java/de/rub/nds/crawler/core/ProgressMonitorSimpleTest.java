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

import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.config.ScannerDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.Scheduler;

class ProgressMonitorSimpleTest {

    @Mock private IOrchestrationProvider orchestrationProvider;

    @Mock private IPersistenceProvider persistenceProvider;

    @Mock private Scheduler scheduler;

    private ProgressMonitor progressMonitor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        progressMonitor =
                new ProgressMonitor(orchestrationProvider, persistenceProvider, scheduler);
    }

    @Test
    void testStartMonitoringBulkScanProgress() {
        BulkScan bulkScan = createTestBulkScan();
        bulkScan.setScanJobsPublished(100);

        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        // The monitor should be tracking this bulk scan
        // We can't directly verify internal state, but we can verify no exceptions are thrown
        assertDoesNotThrow(() -> progressMonitor.startMonitoringBulkScanProgress(bulkScan));
    }

    @Test
    void testStopMonitoringAndFinalizeBulkScan() {
        BulkScan bulkScan = createTestBulkScan();
        progressMonitor.startMonitoringBulkScanProgress(bulkScan);

        progressMonitor.stopMonitoringAndFinalizeBulkScan(bulkScan.get_id());

        // Verify bulk scan was updated
        verify(persistenceProvider).updateBulkScan(any(BulkScan.class));
    }

    @Test
    void testStopMonitoringWithNullId() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> progressMonitor.stopMonitoringAndFinalizeBulkScan(null));
    }

    @Test
    void testMultipleBulkScans() {
        BulkScan bulkScan1 = createTestBulkScan();
        bulkScan1.set_id("scan1");
        BulkScan bulkScan2 = createTestBulkScan();
        bulkScan2.set_id("scan2");

        progressMonitor.startMonitoringBulkScanProgress(bulkScan1);
        progressMonitor.startMonitoringBulkScanProgress(bulkScan2);

        progressMonitor.stopMonitoringAndFinalizeBulkScan("scan1");

        // Only one bulk scan should be updated
        verify(persistenceProvider, times(1)).updateBulkScan(any(BulkScan.class));
    }

    private BulkScan createTestBulkScan() {
        BulkScan bulkScan =
                new BulkScan(
                        getClass(),
                        getClass(),
                        "test-scan",
                        new ScanConfig(ScannerDetail.NORMAL, 1, 1),
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScan.set_id("test-bulk-scan-" + System.currentTimeMillis());
        return bulkScan;
    }
}
