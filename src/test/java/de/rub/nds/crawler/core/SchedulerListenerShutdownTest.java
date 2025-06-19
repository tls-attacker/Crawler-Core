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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

class SchedulerListenerShutdownTest {

    @Mock private Controller controller;

    @Mock private JobDetail jobDetail;

    @Mock private JobKey jobKey;

    @Mock private Trigger trigger;

    @Mock private TriggerKey triggerKey;

    private SchedulerListenerShutdown listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new SchedulerListenerShutdown(controller);
    }

    @Test
    void testJobScheduled() {
        listener.jobScheduled(trigger);
        verify(controller).shutdownSchedulerIfAllTriggersFinalized();
    }

    @Test
    void testJobUnscheduled() {
        listener.jobUnscheduled(triggerKey);
        verify(controller).shutdownSchedulerIfAllTriggersFinalized();
    }

    @Test
    void testTriggerFinalized() {
        listener.triggerFinalized(trigger);
        verify(controller).shutdownSchedulerIfAllTriggersFinalized();
    }

    @Test
    void testJobDeleted() {
        listener.jobDeleted(jobKey);
        verifyNoInteractions(controller);
    }

    @Test
    void testJobAdded() {
        listener.jobAdded(jobDetail);
        verifyNoInteractions(controller);
    }

    @Test
    void testJobPaused() {
        listener.jobPaused(jobKey);
        verifyNoInteractions(controller);
    }

    @Test
    void testJobResumed() {
        listener.jobResumed(jobKey);
        verifyNoInteractions(controller);
    }

    @Test
    void testJobsPaused() {
        listener.jobsPaused("group");
        verifyNoInteractions(controller);
    }

    @Test
    void testJobsResumed() {
        listener.jobsResumed("group");
        verifyNoInteractions(controller);
    }

    @Test
    void testSchedulerError() {
        SchedulerException exception = new SchedulerException("Test error");
        listener.schedulerError("Test error", exception);
        verifyNoInteractions(controller);
    }

    @Test
    void testSchedulerInStandbyMode() {
        listener.schedulerInStandbyMode();
        verifyNoInteractions(controller);
    }

    @Test
    void testSchedulerStarted() {
        listener.schedulerStarted();
        verifyNoInteractions(controller);
    }

    @Test
    void testSchedulerStarting() {
        listener.schedulerStarting();
        verifyNoInteractions(controller);
    }

    @Test
    void testSchedulerShutdown() {
        listener.schedulerShutdown();
        verifyNoInteractions(controller);
    }

    @Test
    void testSchedulerShuttingdown() {
        listener.schedulerShuttingdown();
        verifyNoInteractions(controller);
    }

    @Test
    void testSchedulingDataCleared() {
        listener.schedulingDataCleared();
        verifyNoInteractions(controller);
    }

    @Test
    void testTriggerPaused() {
        listener.triggerPaused(triggerKey);
        verifyNoInteractions(controller);
    }

    @Test
    void testTriggerResumed() {
        listener.triggerResumed(triggerKey);
        verifyNoInteractions(controller);
    }

    @Test
    void testTriggersPaused() {
        listener.triggersPaused("group");
        verifyNoInteractions(controller);
    }

    @Test
    void testTriggersResumed() {
        listener.triggersResumed("group");
        verifyNoInteractions(controller);
    }
}
