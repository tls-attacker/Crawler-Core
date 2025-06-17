/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.core.jobs.PublishBulkScanJob;
import de.rub.nds.crawler.denylist.DenylistFileProvider;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.targetlist.ITargetListProvider;
import java.util.TimeZone;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Controller that orchestrates and schedules bulk scanning operations.
 *
 * <p>Central coordination component managing TLS scanning campaigns. Uses Quartz scheduler for
 * timing, integrates with orchestration providers for job distribution, and supports progress
 * monitoring.
 *
 * @see ControllerCommandConfig
 * @see PublishBulkScanJob
 * @see IOrchestrationProvider
 * @see IPersistenceProvider
 */
public class Controller {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Provider for distributing scan jobs to worker instances. */
    private final IOrchestrationProvider orchestrationProvider;

    /** Provider for scan result storage and retrieval. */
    private final IPersistenceProvider persistenceProvider;

    /** Configuration containing controller parameters and scheduling options. */
    private final ControllerCommandConfig config;

    /** Optional provider for filtering prohibited scan targets. */
    private IDenylistProvider denylistProvider;

    /**
     * Creates a new Controller with the specified configuration and providers.
     *
     * <p>This constructor initializes the controller with all necessary dependencies for
     * orchestrating bulk scanning operations. If a denylist file is specified in the configuration,
     * a denylist provider is automatically created.
     *
     * @param config the controller configuration containing scheduling and scan parameters
     * @param orchestrationProvider the provider for distributing scan jobs to workers
     * @param persistenceProvider the provider for storing and retrieving scan results
     */
    public Controller(
            ControllerCommandConfig config,
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider) {
        this.orchestrationProvider = orchestrationProvider;
        this.persistenceProvider = persistenceProvider;
        this.config = config;
        if (config.getDenylistFile() != null) {
            this.denylistProvider = new DenylistFileProvider(config.getDenylistFile());
        }
    }

    /**
     * Starts the controller and begins scheduling bulk scan operations.
     *
     * <p>This method performs the complete initialization and startup sequence:
     *
     * <ol>
     *   <li>Obtains the target list provider from configuration
     *   <li>Initializes the Quartz scheduler with appropriate listeners
     *   <li>Creates progress monitoring if enabled in configuration
     *   <li>Prepares job data map with all necessary providers and configuration
     *   <li>Schedules the bulk scan publishing job according to configuration
     *   <li>Starts the scheduler to begin processing
     * </ol>
     *
     * <p><strong>Progress Monitoring:</strong> If monitoring is enabled in the configuration, a
     * {@link ProgressMonitor} is created to track scan progress and send notifications.
     *
     * <p><strong>Automatic Shutdown:</strong> The scheduler is configured to automatically shut
     * down when all scheduled jobs complete execution.
     *
     * @throws RuntimeException if scheduler initialization or startup fails
     * @see ControllerCommandConfig#isMonitored()
     * @see PublishBulkScanJob
     * @see ProgressMonitor
     */
    public void start() {
        ITargetListProvider targetListProvider = config.getTargetListProvider();

        ProgressMonitor progressMonitor = null;

        SchedulerFactory sf = new StdSchedulerFactory();
        try {
            Scheduler scheduler = sf.getScheduler();
            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(getScanSchedule()).build();
            scheduler
                    .getListenerManager()
                    .addSchedulerListener(new SchedulerListenerShutdown(scheduler));

            if (config.isMonitored()) {
                progressMonitor =
                        new ProgressMonitor(orchestrationProvider, persistenceProvider, scheduler);
            }

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("config", config);
            jobDataMap.put("orchestrationProvider", orchestrationProvider);
            jobDataMap.put("persistenceProvider", persistenceProvider);
            jobDataMap.put("targetListProvider", targetListProvider);
            jobDataMap.put("denylistProvider", denylistProvider);
            jobDataMap.put("progressMonitor", progressMonitor);

            // schedule job publishing according to specified cmd parameters
            scheduler.scheduleJob(
                    JobBuilder.newJob(PublishBulkScanJob.class).usingJobData(jobDataMap).build(),
                    trigger);

            scheduler.start();
        } catch (SchedulerException e) {
            LOGGER.error("Scheduler exception with message ", e);
        }
    }

    /**
     * Creates the appropriate schedule builder based on configuration.
     *
     * <p>This method determines the scheduling strategy:
     *
     * <ul>
     *   <li><strong>Cron-based:</strong> If a cron interval is specified, creates a cron schedule
     *       using the system default timezone
     *   <li><strong>Simple:</strong> If no cron interval is specified, creates a simple schedule
     *       for immediate one-time execution
     * </ul>
     *
     * @return the appropriate ScheduleBuilder for the configured scheduling strategy
     * @see ControllerCommandConfig#getScanCronInterval()
     */
    private ScheduleBuilder<?> getScanSchedule() {
        if (config.getScanCronInterval() != null) {
            return CronScheduleBuilder.cronSchedule(config.getScanCronInterval())
                    .inTimeZone(TimeZone.getDefault());
        } else {
            return SimpleScheduleBuilder.simpleSchedule();
        }
    }

    /**
     * Conditionally shuts down the scheduler if all triggers have completed.
     *
     * <p>This utility method provides graceful scheduler shutdown by checking the state of all
     * registered triggers. The scheduler is shut down only when no triggers are capable of firing
     * again, indicating that all scheduled work is complete.
     *
     * <p><strong>Trigger State Checking:</strong>
     *
     * <ul>
     *   <li>Examines all triggers across all groups
     *   <li>Checks if each trigger can fire again using {@code mayFireAgain()}
     *   <li>Handles scheduler exceptions by assuming triggers are still active
     *   <li>Only shuts down when all triggers are finalized
     * </ul>
     *
     * <p><strong>Error Handling:</strong> If trigger state cannot be determined due to scheduler
     * exceptions, the trigger is conservatively treated as still active to prevent premature
     * shutdown.
     *
     * @param scheduler the Quartz scheduler to potentially shut down
     * @see Scheduler#shutdown()
     * @see Trigger#mayFireAgain()
     */
    public static void shutdownSchedulerIfAllTriggersFinalized(Scheduler scheduler) {
        try {
            boolean allTriggersFinalized =
                    scheduler.getTriggerKeys(GroupMatcher.anyGroup()).stream()
                            .map(
                                    k -> {
                                        try {
                                            return scheduler.getTrigger(k).mayFireAgain();
                                        } catch (SchedulerException e) {
                                            LOGGER.warn(
                                                    "Could not read trigger state in scheduler. Treating as still running.");
                                            return false;
                                        }
                                    })
                            .noneMatch(Predicate.isEqual(true));

            if (allTriggersFinalized) {
                LOGGER.info("All scheduled Jobs published. Shutting down scheduler.");
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            LOGGER.error("Scheduler exception with message ", e);
        }
    }
}
