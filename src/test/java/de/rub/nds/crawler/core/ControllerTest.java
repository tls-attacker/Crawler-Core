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

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import de.rub.nds.crawler.dummy.DummyOrchestrationProvider;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import de.rub.nds.crawler.targetlist.ITargetListProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

class ControllerTest {

    @Test
    void submitting() throws IOException, InterruptedException {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        ControllerCommandConfig config = new DummyControllerCommandConfig();

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("example.com\nexample.org:8000");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        Thread.sleep(1000);

        Assertions.assertEquals(2, orchestrationProvider.jobQueue.size());
        Assertions.assertEquals(0, orchestrationProvider.unackedJobs.size());
    }

    @Test
    void testStartWithCronSchedule() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();
        config.setCronExpression("0 0 * * * ?"); // Every hour

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("example.com");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        // Scheduler should be running
        assertNotNull(controller.scheduler);
        assertTrue(controller.scheduler.isStarted());

        // Shutdown for cleanup
        controller.scheduler.shutdown();
    }

    @Test
    void testStartWithSimpleSchedule() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();
        config.setDelay(1000); // 1 second delay

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("example.com");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        // Wait for job to execute
        Thread.sleep(1500);

        // Check that job was executed
        assertTrue(orchestrationProvider.jobQueue.size() > 0);

        // Shutdown for cleanup
        controller.scheduler.shutdown();
    }

    @Test
    void testStartWithProgressMonitor() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();
        config.setMonitor(true);

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("example.com");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        Thread.sleep(1000);

        // Progress monitor should have been created
        assertNotNull(controller.progressMonitor);

        // Shutdown for cleanup
        controller.scheduler.shutdown();
    }

    @Test
    void testStartWithDenylistProvider() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();

        IDenylistProvider mockDenylistProvider = mock(IDenylistProvider.class);
        config.setDenylistProvider(mockDenylistProvider);

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("example.com");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        Thread.sleep(1000);

        // Denylist provider should have been used
        verify(mockDenylistProvider, atLeastOnce()).isDenied(anyString());

        // Shutdown for cleanup
        controller.scheduler.shutdown();
    }

    @Test
    void testShutdownSchedulerIfAllTriggersFinalized() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.scheduler = mock(Scheduler.class);

        // Test when scheduler is not started
        when(controller.scheduler.isStarted()).thenReturn(false);
        controller.shutdownSchedulerIfAllTriggersFinalized();
        verify(controller.scheduler, never()).shutdown();

        // Test when scheduler is started but has triggers
        when(controller.scheduler.isStarted()).thenReturn(true);
        when(controller.scheduler.getTriggerKeys(any())).thenReturn(Set.of(new TriggerKey("test")));
        controller.shutdownSchedulerIfAllTriggersFinalized();
        verify(controller.scheduler, never()).shutdown();

        // Test when scheduler is started and has no triggers
        when(controller.scheduler.getTriggerKeys(any())).thenReturn(Set.of());
        controller.shutdownSchedulerIfAllTriggersFinalized();
        verify(controller.scheduler).shutdown();
    }

    @Test
    void testShutdownSchedulerWithException() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.scheduler = mock(Scheduler.class);

        when(controller.scheduler.isStarted()).thenReturn(true);
        when(controller.scheduler.getTriggerKeys(any()))
                .thenThrow(new SchedulerException("Test error"));

        // Should not throw exception
        assertDoesNotThrow(() -> controller.shutdownSchedulerIfAllTriggersFinalized());
    }

    @Test
    void testGetScanScheduleWithInvalidCron() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();
        config.setCronExpression("invalid cron expression");

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);

        // Should throw exception for invalid cron
        assertThrows(RuntimeException.class, () -> controller.getScanSchedule());
    }

    // Test configuration class that allows setting all parameters
    private static class TestControllerCommandConfig extends DummyControllerCommandConfig {
        private String cronExpression;
        private int delay = 0;
        private boolean monitor = false;
        private IDenylistProvider denylistProvider;

        @Override
        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }

        @Override
        public int getDelay() {
            return delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }

        @Override
        public boolean isMonitor() {
            return monitor;
        }

        public void setMonitor(boolean monitor) {
            this.monitor = monitor;
        }

        @Override
        public IDenylistProvider getDenylistProvider() {
            return denylistProvider;
        }

        public void setDenylistProvider(IDenylistProvider denylistProvider) {
            this.denylistProvider = denylistProvider;
        }

        @Override
        public ITargetListProvider getTargetListProvider() {
            return new ITargetListProvider() {
                @Override
                public Stream<String> getTargets() {
                    return Stream.of("example.com");
                }
            };
        }
    }
}
