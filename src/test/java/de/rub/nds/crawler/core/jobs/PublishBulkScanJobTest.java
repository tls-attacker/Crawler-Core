/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.core.ProgressMonitor;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.targetlist.ITargetListProvider;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

class PublishBulkScanJobTest {

    @Mock private JobExecutionContext jobExecutionContext;

    @Mock private JobDataMap jobDataMap;

    @Mock private ControllerCommandConfig config;

    @Mock private ITargetListProvider targetListProvider;

    @Mock private IDenylistProvider denylistProvider;

    @Mock private IOrchestrationProvider orchestrationProvider;

    @Mock private IPersistenceProvider persistenceProvider;

    @Mock private ProgressMonitor progressMonitor;

    @Mock private Scheduler scheduler;

    private PublishBulkScanJob publishBulkScanJob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publishBulkScanJob = new PublishBulkScanJob();

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
        when(jobDataMap.get("config")).thenReturn(config);
        when(jobDataMap.get("orchestrationProvider")).thenReturn(orchestrationProvider);
        when(jobDataMap.get("persistenceProvider")).thenReturn(persistenceProvider);
        when(jobDataMap.get("denylistProvider")).thenReturn(denylistProvider);
        when(jobDataMap.get("progressMonitor")).thenReturn(progressMonitor);
        when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
    }

    @Test
    void testExecuteSuccess() throws JobExecutionException {
        // Setup
        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        List<String> targets = Arrays.asList("example.com", "test.org:8443");
        when(targetListProvider.getTargets()).thenReturn(targets.stream());

        when(denylistProvider.isDenied(anyString())).thenReturn(false);

        // Execute
        publishBulkScanJob.execute(jobExecutionContext);

        // Verify
        ArgumentCaptor<BulkScan> bulkScanCaptor = ArgumentCaptor.forClass(BulkScan.class);
        verify(persistenceProvider).saveBulkScan(bulkScanCaptor.capture());

        BulkScan savedBulkScan = bulkScanCaptor.getValue();
        assertNotNull(savedBulkScan);
        assertEquals(scanConfig, savedBulkScan.getScanConfig());
        assertEquals(2, savedBulkScan.getJobTotal());

        // Verify jobs were submitted
        verify(orchestrationProvider, times(2)).submitJob(any(ScanJobDescription.class));

        // Verify progress monitor was started
        verify(progressMonitor).startMonitoringBulkScanProgress(savedBulkScan);
    }

    @Test
    void testExecuteWithDenylistedHost() throws JobExecutionException {
        // Setup
        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        List<String> targets = Arrays.asList("example.com", "denied.com");
        when(targetListProvider.getTargets()).thenReturn(targets.stream());

        when(denylistProvider.isDenied("example.com")).thenReturn(false);
        when(denylistProvider.isDenied("denied.com")).thenReturn(true);

        // Execute
        publishBulkScanJob.execute(jobExecutionContext);

        // Verify only one job was submitted
        verify(orchestrationProvider, times(1)).submitJob(any(ScanJobDescription.class));

        ArgumentCaptor<BulkScan> bulkScanCaptor = ArgumentCaptor.forClass(BulkScan.class);
        verify(persistenceProvider, times(2)).saveBulkScan(bulkScanCaptor.capture());

        BulkScan finalBulkScan = bulkScanCaptor.getAllValues().get(1);
        assertEquals(1, finalBulkScan.getJobTotal());
    }

    @Test
    void testExecuteWithUnresolvableHost() throws JobExecutionException {
        // Setup
        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        List<String> targets =
                Arrays.asList("example.com", "this-host-does-not-exist-12345.invalid");
        when(targetListProvider.getTargets()).thenReturn(targets.stream());

        when(denylistProvider.isDenied(anyString())).thenReturn(false);

        // Execute
        publishBulkScanJob.execute(jobExecutionContext);

        // Verify - one should succeed, one should fail resolution
        verify(orchestrationProvider, atLeast(1)).submitJob(any(ScanJobDescription.class));
        verify(orchestrationProvider, atMost(2)).submitJob(any(ScanJobDescription.class));
    }

    @Test
    void testExecuteWithInvalidTargetFormat() throws JobExecutionException {
        // Setup
        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        List<String> targets =
                Arrays.asList("example.com", "invalid:port:format", "test.org:notanumber");
        when(targetListProvider.getTargets()).thenReturn(targets.stream());

        when(denylistProvider.isDenied(anyString())).thenReturn(false);

        // Execute
        publishBulkScanJob.execute(jobExecutionContext);

        // Verify - only valid targets should be submitted
        verify(orchestrationProvider, atLeast(1)).submitJob(any(ScanJobDescription.class));
    }

    @Test
    void testExecuteWithProgressMonitorNull() throws JobExecutionException {
        // Setup
        when(jobDataMap.get("progressMonitor")).thenReturn(null);

        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        List<String> targets = Arrays.asList("example.com");
        when(targetListProvider.getTargets()).thenReturn(targets.stream());

        when(denylistProvider.isDenied(anyString())).thenReturn(false);

        // Execute - should not throw exception
        assertDoesNotThrow(() -> publishBulkScanJob.execute(jobExecutionContext));

        // Verify job was still submitted
        verify(orchestrationProvider).submitJob(any(ScanJobDescription.class));
    }

    @Test
    void testExecuteEmptyTargetList() throws JobExecutionException {
        // Setup
        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        when(targetListProvider.getTargets()).thenReturn(Stream.empty());

        // Execute
        publishBulkScanJob.execute(jobExecutionContext);

        // Verify no jobs were submitted
        verify(orchestrationProvider, never()).submitJob(any(ScanJobDescription.class));

        // Verify bulk scan was still saved
        ArgumentCaptor<BulkScan> bulkScanCaptor = ArgumentCaptor.forClass(BulkScan.class);
        verify(persistenceProvider).saveBulkScan(bulkScanCaptor.capture());

        BulkScan savedBulkScan = bulkScanCaptor.getValue();
        assertEquals(0, savedBulkScan.getJobTotal());
    }

    @Test
    void testJobSubmitterParallelExecution() throws JobExecutionException {
        // Setup
        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        // Large number of targets to test parallel processing
        Stream<String> targets =
                Stream.generate(() -> "example" + Math.random() + ".com").limit(100);
        when(targetListProvider.getTargets()).thenReturn(targets);

        when(denylistProvider.isDenied(anyString())).thenReturn(false);

        // Execute
        publishBulkScanJob.execute(jobExecutionContext);

        // Verify all jobs were submitted
        verify(orchestrationProvider, times(100)).submitJob(any(ScanJobDescription.class));
    }

    @Test
    void testSchedulerShutdownOnException() throws JobExecutionException, SchedulerException {
        // Setup to throw exception during execution
        when(config.getScanConfig()).thenThrow(new RuntimeException("Test exception"));

        // Execute
        assertThrows(
                JobExecutionException.class, () -> publishBulkScanJob.execute(jobExecutionContext));

        // Verify scheduler was shutdown
        verify(scheduler).shutdown();
    }

    @Test
    void testHostWithExplicitPort() throws JobExecutionException {
        // Setup
        ScanConfig scanConfig = new ScanConfig(ScannerDetail.NORMAL, 1, 1);
        when(config.getScanConfig()).thenReturn(scanConfig);
        when(config.getTargetListProvider()).thenReturn(targetListProvider);

        List<String> targets = Arrays.asList("example.com:8443", "test.org:443");
        when(targetListProvider.getTargets()).thenReturn(targets.stream());

        when(denylistProvider.isDenied(anyString())).thenReturn(false);

        // Execute
        publishBulkScanJob.execute(jobExecutionContext);

        // Verify correct ports were used
        ArgumentCaptor<ScanJobDescription> jobCaptor =
                ArgumentCaptor.forClass(ScanJobDescription.class);
        verify(orchestrationProvider, times(2)).submitJob(jobCaptor.capture());

        List<ScanJobDescription> submittedJobs = jobCaptor.getAllValues();
        assertTrue(
                submittedJobs.stream()
                        .anyMatch(
                                job ->
                                        job.getScanTarget().getPort() == 8443
                                                && job.getScanTarget()
                                                        .getHostName()
                                                        .equals("example.com")));
        assertTrue(
                submittedJobs.stream()
                        .anyMatch(
                                job ->
                                        job.getScanTarget().getPort() == 443
                                                && job.getScanTarget()
                                                        .getHostName()
                                                        .equals("test.org")));
    }
}
