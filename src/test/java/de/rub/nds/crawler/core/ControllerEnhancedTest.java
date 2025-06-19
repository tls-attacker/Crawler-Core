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

import de.rub.nds.crawler.denylist.IDenylistProvider;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import de.rub.nds.crawler.dummy.DummyOrchestrationProvider;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import de.rub.nds.crawler.targetlist.ITargetListProvider;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

class ControllerEnhancedTest {

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

        // Start the controller which will create and start a scheduler internally
        controller.start();

        // Wait a bit to ensure scheduler is started
        Thread.sleep(500);

        // We can't access the scheduler directly, but we can verify the job was scheduled
        // by checking if any jobs were queued (this would happen if the schedule triggered)
        // For a cron expression that runs every hour, it won't trigger immediately
        assertEquals(0, orchestrationProvider.jobQueue.size());
    }

    @Test
    void testStartWithSimpleSchedule() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();
        config.setDelay(100); // 100ms delay

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
        Thread.sleep(500);

        // Check that job was executed
        assertTrue(orchestrationProvider.jobQueue.size() > 0);
    }

    @Test
    void testStartWithProgressMonitor() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();
        config.setMonitor(true);
        config.setDelay(100); // Add delay to trigger job execution

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("example.com");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        // Wait for job execution
        Thread.sleep(500);

        // Verify job was executed with monitoring enabled
        assertTrue(orchestrationProvider.jobQueue.size() > 0);
    }

    @Test
    void testStartWithDenylistProvider() throws Exception {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        TestControllerCommandConfig config = new TestControllerCommandConfig();
        config.setDelay(100); // Add delay to trigger job execution

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

        Thread.sleep(500);

        // Denylist provider should have been used during job execution
        verify(mockDenylistProvider, atLeastOnce()).isDenied(anyString());
    }

    @Test
    void testStaticShutdownSchedulerIfAllTriggersFinalized() throws Exception {
        Scheduler mockScheduler = mock(Scheduler.class);

        // Test when scheduler is not started
        when(mockScheduler.isStarted()).thenReturn(false);
        Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler);
        verify(mockScheduler, never()).shutdown();

        // Test when scheduler is started but has triggers
        when(mockScheduler.isStarted()).thenReturn(true);
        when(mockScheduler.getTriggerKeys(any())).thenReturn(Set.of(new TriggerKey("test")));
        Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler);
        verify(mockScheduler, never()).shutdown();

        // Test when scheduler is started and has no triggers
        when(mockScheduler.getTriggerKeys(any())).thenReturn(Set.of());
        Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler);
        verify(mockScheduler).shutdown();
    }

    @Test
    void testStaticShutdownSchedulerWithException() throws Exception {
        Scheduler mockScheduler = mock(Scheduler.class);

        when(mockScheduler.isStarted()).thenReturn(true);
        when(mockScheduler.getTriggerKeys(any())).thenThrow(new SchedulerException("Test error"));

        // Should not throw exception
        assertDoesNotThrow(() -> Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler));
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

                @Override
                public List<String> getTargetList() {
                    return List.of("example.com");
                }
            };
        }
    }
}
