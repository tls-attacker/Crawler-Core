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

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import de.rub.nds.crawler.dummy.DummyOrchestrationProvider;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.targetlist.ITargetListProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

class ControllerTest {

    @Mock private ControllerCommandConfig mockConfig;
    @Mock private IOrchestrationProvider mockOrchestrationProvider;
    @Mock private IPersistenceProvider mockPersistenceProvider;
    @Mock private ITargetListProvider mockTargetListProvider;
    @Mock private Scheduler mockScheduler;
    @Mock private ListenerManager mockListenerManager;
    @Mock private Trigger mockTrigger;

    private Controller controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

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
    void testConstructorWithoutDenylist() {
        when(mockConfig.getDenylistFile()).thenReturn(null);

        controller = new Controller(mockConfig, mockOrchestrationProvider, mockPersistenceProvider);

        assertNotNull(controller);
        verify(mockConfig).getDenylistFile();
    }

    @Test
    void testConstructorWithDenylist() throws Exception {
        File denylistFile = File.createTempFile("denylist", ".txt");
        denylistFile.deleteOnExit();

        when(mockConfig.getDenylistFile()).thenReturn(denylistFile.getAbsolutePath());

        controller = new Controller(mockConfig, mockOrchestrationProvider, mockPersistenceProvider);

        // Verify denylist provider was created via reflection
        Field denylistField = Controller.class.getDeclaredField("denylistProvider");
        denylistField.setAccessible(true);
        IDenylistProvider denylistProvider = (IDenylistProvider) denylistField.get(controller);

        assertNotNull(denylistProvider);
    }

    @Test
    void testStartWithCronSchedule() throws SchedulerException {
        // Setup
        when(mockConfig.getTargetListProvider()).thenReturn(mockTargetListProvider);
        when(mockConfig.getScanCronInterval()).thenReturn("0 0 12 * * ?");
        when(mockConfig.isMonitored()).thenReturn(false);
        when(mockConfig.getDenylistFile()).thenReturn(null);

        // Use spy to intercept scheduler creation
        Controller controllerSpy =
                spy(new Controller(mockConfig, mockOrchestrationProvider, mockPersistenceProvider));

        // We can't easily mock the StdSchedulerFactory, so let's test what we can
        assertDoesNotThrow(() -> controllerSpy.start());

        verify(mockConfig).getTargetListProvider();
        verify(mockConfig, atLeastOnce()).getScanCronInterval();
        verify(mockConfig).isMonitored();
    }

    @Test
    void testStartWithSimpleSchedule() {
        // Setup
        when(mockConfig.getTargetListProvider()).thenReturn(mockTargetListProvider);
        when(mockConfig.getScanCronInterval()).thenReturn(null);
        when(mockConfig.isMonitored()).thenReturn(false);
        when(mockConfig.getDenylistFile()).thenReturn(null);

        Controller controller =
                new Controller(mockConfig, mockOrchestrationProvider, mockPersistenceProvider);

        assertDoesNotThrow(() -> controller.start());

        verify(mockConfig).getScanCronInterval();
    }

    @Test
    void testStartWithMonitoring() {
        // Setup
        when(mockConfig.getTargetListProvider()).thenReturn(mockTargetListProvider);
        when(mockConfig.getScanCronInterval()).thenReturn(null);
        when(mockConfig.isMonitored()).thenReturn(true);
        when(mockConfig.getDenylistFile()).thenReturn(null);

        Controller controller =
                new Controller(mockConfig, mockOrchestrationProvider, mockPersistenceProvider);

        assertDoesNotThrow(() -> controller.start());

        verify(mockConfig).isMonitored();
    }

    @Test
    void testShutdownSchedulerIfAllTriggersFinalized_NoTriggers() throws SchedulerException {
        // Setup
        Set<TriggerKey> triggerKeys = new HashSet<>();
        when(mockScheduler.getTriggerKeys(any(GroupMatcher.class))).thenReturn(triggerKeys);

        // When
        Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler);

        // Then
        verify(mockScheduler).shutdown();
    }

    @Test
    void testShutdownSchedulerIfAllTriggersFinalized_WithActiveTrigger() throws SchedulerException {
        // Setup
        TriggerKey triggerKey = new TriggerKey("trigger1", "group1");
        Set<TriggerKey> triggerKeys = new HashSet<>();
        triggerKeys.add(triggerKey);

        when(mockScheduler.getTriggerKeys(any(GroupMatcher.class))).thenReturn(triggerKeys);
        when(mockScheduler.getTrigger(triggerKey)).thenReturn(mockTrigger);
        when(mockTrigger.mayFireAgain()).thenReturn(true);

        // When
        Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler);

        // Then
        verify(mockScheduler, never()).shutdown();
    }

    @Test
    void testShutdownSchedulerIfAllTriggersFinalized_WithInactiveTrigger()
            throws SchedulerException {
        // Setup
        TriggerKey triggerKey = new TriggerKey("trigger1", "group1");
        Set<TriggerKey> triggerKeys = new HashSet<>();
        triggerKeys.add(triggerKey);

        when(mockScheduler.getTriggerKeys(any(GroupMatcher.class))).thenReturn(triggerKeys);
        when(mockScheduler.getTrigger(triggerKey)).thenReturn(mockTrigger);
        when(mockTrigger.mayFireAgain()).thenReturn(false);

        // When
        Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler);

        // Then
        verify(mockScheduler).shutdown();
    }

    @Test
    void testShutdownSchedulerIfAllTriggersFinalized_SchedulerException()
            throws SchedulerException {
        // Setup
        when(mockScheduler.getTriggerKeys(any(GroupMatcher.class)))
                .thenThrow(new SchedulerException("Test exception"));

        // When/Then - should not throw
        assertDoesNotThrow(() -> Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler));
    }

    @Test
    void testShutdownSchedulerIfAllTriggersFinalized_TriggerReadException()
            throws SchedulerException {
        // Setup
        TriggerKey triggerKey = new TriggerKey("trigger1", "group1");
        Set<TriggerKey> triggerKeys = new HashSet<>();
        triggerKeys.add(triggerKey);

        when(mockScheduler.getTriggerKeys(any(GroupMatcher.class))).thenReturn(triggerKeys);
        when(mockScheduler.getTrigger(triggerKey))
                .thenThrow(new SchedulerException("Cannot read trigger"));

        // When
        Controller.shutdownSchedulerIfAllTriggersFinalized(mockScheduler);

        // Then - should not shutdown due to exception (treated as still running)
        verify(mockScheduler, never()).shutdown();
    }
}
