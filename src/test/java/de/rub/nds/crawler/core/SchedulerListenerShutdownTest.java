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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

class SchedulerListenerShutdownTest {

    private SchedulerListenerShutdown listener;
    private Scheduler scheduler;

    @BeforeEach
    void setUp() throws SchedulerException {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        listener = new SchedulerListenerShutdown(scheduler);
    }

    @Test
    void testConstructor() {
        assertNotNull(listener);
    }

    @Test
    void testJobScheduled() throws SchedulerException {
        // Create a mock trigger
        JobDetail jobDetail =
                JobBuilder.newJob(TestJob.class).withIdentity("testJob", "testGroup").build();

        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .withIdentity("testTrigger", "testGroup")
                        .startNow()
                        .build();

        // This should not throw any exceptions
        listener.jobScheduled(trigger);
    }

    @Test
    void testJobUnscheduled() {
        TriggerKey triggerKey = new TriggerKey("testTrigger", "testGroup");
        // This should not throw any exceptions
        listener.jobUnscheduled(triggerKey);
    }

    @Test
    void testTriggerFinalized() {
        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .withIdentity("testTrigger", "testGroup")
                        .startNow()
                        .build();

        // This should not throw any exceptions
        listener.triggerFinalized(trigger);
    }

    @Test
    void testOtherListenerMethods() {
        // Test all the empty methods to ensure they don't throw exceptions
        TriggerKey triggerKey = new TriggerKey("testTrigger", "testGroup");
        JobKey jobKey = new JobKey("testJob", "testGroup");
        JobDetail jobDetail = JobBuilder.newJob(TestJob.class).withIdentity(jobKey).build();

        // None of these should throw exceptions
        listener.triggerPaused(triggerKey);
        listener.triggersPaused("testGroup");
        listener.triggerResumed(triggerKey);
        listener.triggersResumed("testGroup");
        listener.jobAdded(jobDetail);
        listener.jobDeleted(jobKey);
        listener.jobPaused(jobKey);
        listener.jobsPaused("testGroup");
        listener.jobResumed(jobKey);
        listener.jobsResumed("testGroup");
        listener.schedulerError("Test error", new SchedulerException());
        listener.schedulerInStandbyMode();
        listener.schedulerStarted();
        listener.schedulerStarting();
        listener.schedulerShutdown();
        listener.schedulerShuttingdown();
        listener.schedulingDataCleared();
    }

    // Test job for Quartz
    public static class TestJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            // Empty test job
        }
    }
}
