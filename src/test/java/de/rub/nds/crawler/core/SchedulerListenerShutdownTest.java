/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

class SchedulerListenerShutdownTest {

    @Mock private Scheduler mockScheduler;
    @Mock private Trigger mockTrigger;
    @Mock private TriggerKey mockTriggerKey;
    @Mock private JobDetail mockJobDetail;
    @Mock private JobKey mockJobKey;

    private SchedulerListenerShutdown listener;

    @BeforeEach
    void setUp() throws SchedulerException {
        MockitoAnnotations.openMocks(this);
        listener = new SchedulerListenerShutdown(mockScheduler);

        // Default behavior - no triggers
        Set<TriggerKey> emptySet = new HashSet<>();
        when(mockScheduler.getTriggerKeys(any(GroupMatcher.class))).thenReturn(emptySet);
    }

    @Test
    void testJobScheduled() throws SchedulerException {
        // When
        listener.jobScheduled(mockTrigger);

        // Then - should check if scheduler should shutdown
        verify(mockScheduler).getTriggerKeys(any(GroupMatcher.class));
    }

    @Test
    void testJobUnscheduled() throws SchedulerException {
        // When
        listener.jobUnscheduled(mockTriggerKey);

        // Then - should check if scheduler should shutdown
        verify(mockScheduler).getTriggerKeys(any(GroupMatcher.class));
    }

    @Test
    void testTriggerFinalized() throws SchedulerException {
        // When
        listener.triggerFinalized(mockTrigger);

        // Then - should check if scheduler should shutdown
        verify(mockScheduler).getTriggerKeys(any(GroupMatcher.class));
    }

    @Test
    void testTriggerFinalizedCausesShutdown() throws SchedulerException {
        // Given - no active triggers
        Set<TriggerKey> emptySet = new HashSet<>();
        when(mockScheduler.getTriggerKeys(any(GroupMatcher.class))).thenReturn(emptySet);

        // When
        listener.triggerFinalized(mockTrigger);

        // Then - should shutdown
        verify(mockScheduler).shutdown();
    }

    @Test
    void testEmptyMethodsDontTriggerShutdown() throws SchedulerException {
        // Test all the empty methods
        listener.triggerPaused(mockTriggerKey);
        listener.triggersPaused("group");
        listener.triggerResumed(mockTriggerKey);
        listener.triggersResumed("group");
        listener.jobAdded(mockJobDetail);
        listener.jobDeleted(mockJobKey);
        listener.jobPaused(mockJobKey);
        listener.jobsPaused("group");
        listener.jobResumed(mockJobKey);
        listener.jobsResumed("group");
        listener.schedulerError("error", new SchedulerException());
        listener.schedulerInStandbyMode();
        listener.schedulerStarted();
        listener.schedulerStarting();
        listener.schedulerShutdown();
        listener.schedulerShuttingdown();
        listener.schedulingDataCleared();

        // Then - none of these should trigger shutdown check
        verify(mockScheduler, never()).getTriggerKeys(any(GroupMatcher.class));
        verify(mockScheduler, never()).shutdown();
    }

    @Test
    void testConstructor() throws SchedulerException {
        // Test that constructor properly stores scheduler reference
        SchedulerListenerShutdown newListener = new SchedulerListenerShutdown(mockScheduler);

        // When - trigger an event that uses the scheduler
        newListener.jobScheduled(mockTrigger);

        // Then - verify it uses the provided scheduler
        verify(mockScheduler).getTriggerKeys(any(GroupMatcher.class));
    }
}
