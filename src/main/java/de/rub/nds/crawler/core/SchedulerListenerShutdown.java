/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import static de.rub.nds.crawler.core.Controller.shutdownSchedulerIfAllTriggersFinalized;

import org.quartz.*;

/**
 * Listener which shuts scheduler down when all triggers are finalized and thereby prevents
 * application from running forever if all schedules are finished.
 */
class SchedulerListenerShutdown implements SchedulerListener {

    private final Scheduler scheduler;

    SchedulerListenerShutdown(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Called when a job is scheduled with a trigger. Checks if all triggers are finalized and shuts
     * down the scheduler if so.
     *
     * @param trigger the trigger that was scheduled
     */
    @Override
    public void jobScheduled(Trigger trigger) {
        shutdownSchedulerIfAllTriggersFinalized(scheduler);
    }

    /**
     * Called when a job is unscheduled. Checks if all triggers are finalized and shuts down the
     * scheduler if so.
     *
     * @param triggerKey the key of the trigger that was unscheduled
     */
    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
        shutdownSchedulerIfAllTriggersFinalized(scheduler);
    }

    /**
     * Called when a trigger reaches its final fire time and will not fire again. Checks if all
     * triggers are finalized and shuts down the scheduler if so.
     *
     * @param trigger the trigger that was finalized
     */
    @Override
    public void triggerFinalized(Trigger trigger) {
        shutdownSchedulerIfAllTriggersFinalized(scheduler);
    }

    /**
     * Called when a trigger is paused. No action is taken by this implementation.
     *
     * @param triggerKey the key of the trigger that was paused
     */
    @Override
    public void triggerPaused(TriggerKey triggerKey) {}

    /**
     * Called when a group of triggers is paused. No action is taken by this implementation.
     *
     * @param triggerGroup the name of the trigger group that was paused
     */
    @Override
    public void triggersPaused(String triggerGroup) {}

    /**
     * Called when a trigger is resumed from pause. No action is taken by this implementation.
     *
     * @param triggerKey the key of the trigger that was resumed
     */
    @Override
    public void triggerResumed(TriggerKey triggerKey) {}

    /**
     * Called when a group of triggers is resumed from pause. No action is taken by this
     * implementation.
     *
     * @param triggerGroup the name of the trigger group that was resumed
     */
    @Override
    public void triggersResumed(String triggerGroup) {}

    /**
     * Called when a job is added to the scheduler. No action is taken by this implementation.
     *
     * @param jobDetail the details of the job that was added
     */
    @Override
    public void jobAdded(JobDetail jobDetail) {}

    /**
     * Called when a job is deleted from the scheduler. No action is taken by this implementation.
     *
     * @param jobKey the key of the job that was deleted
     */
    @Override
    public void jobDeleted(JobKey jobKey) {}

    /**
     * Called when a job is paused. No action is taken by this implementation.
     *
     * @param jobKey the key of the job that was paused
     */
    @Override
    public void jobPaused(JobKey jobKey) {}

    /**
     * Called when a group of jobs is paused. No action is taken by this implementation.
     *
     * @param jobGroup the name of the job group that was paused
     */
    @Override
    public void jobsPaused(String jobGroup) {}

    /**
     * Called when a job is resumed from pause. No action is taken by this implementation.
     *
     * @param jobKey the key of the job that was resumed
     */
    @Override
    public void jobResumed(JobKey jobKey) {}

    /**
     * Called when a group of jobs is resumed from pause. No action is taken by this implementation.
     *
     * @param jobGroup the name of the job group that was resumed
     */
    @Override
    public void jobsResumed(String jobGroup) {}

    /**
     * Called when a serious error occurs during scheduling. No action is taken by this
     * implementation.
     *
     * @param msg the error message
     * @param cause the exception that caused the error
     */
    @Override
    public void schedulerError(String msg, SchedulerException cause) {}

    /** Called when the scheduler enters standby mode. No action is taken by this implementation. */
    @Override
    public void schedulerInStandbyMode() {}

    /** Called when the scheduler has been started. No action is taken by this implementation. */
    @Override
    public void schedulerStarted() {}

    /** Called when the scheduler is starting. No action is taken by this implementation. */
    @Override
    public void schedulerStarting() {}

    /** Called when the scheduler has been shut down. No action is taken by this implementation. */
    @Override
    public void schedulerShutdown() {}

    /** Called when the scheduler is shutting down. No action is taken by this implementation. */
    @Override
    public void schedulerShuttingdown() {}

    /**
     * Called when all scheduling data has been cleared. No action is taken by this implementation.
     */
    @Override
    public void schedulingDataCleared() {}
}
