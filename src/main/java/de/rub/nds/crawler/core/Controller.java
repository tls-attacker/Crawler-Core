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

/** Controller that schedules the publishing of bulk scans. */
public class Controller {

    private static final Logger LOGGER = LogManager.getLogger();

    private final IOrchestrationProvider orchestrationProvider;
    private final IPersistenceProvider persistenceProvider;
    private final ControllerCommandConfig config;
    private IDenylistProvider denylistProvider;

    /**
     * Constructs a new Controller with the specified configuration and providers. Initializes the
     * controller with orchestration and persistence capabilities, and optionally sets up a denylist
     * provider if specified in the configuration.
     *
     * @param config The controller configuration containing scan parameters and settings
     * @param orchestrationProvider Provider for job orchestration and coordination
     * @param persistenceProvider Provider for data persistence operations
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
     * Starts the controller by initializing the scheduler and scheduling bulk scan jobs. Sets up a
     * Quartz scheduler with the configured schedule (either cron-based or simple), registers
     * necessary listeners, and optionally starts a progress monitor. The scheduler will publish
     * bulk scan jobs according to the specified timing.
     *
     * @throws RuntimeException if scheduler initialization fails
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

    private ScheduleBuilder<?> getScanSchedule() {
        if (config.getScanCronInterval() != null) {
            return CronScheduleBuilder.cronSchedule(config.getScanCronInterval())
                    .inTimeZone(TimeZone.getDefault());
        } else {
            return SimpleScheduleBuilder.simpleSchedule();
        }
    }

    /**
     * Shuts down the scheduler if all triggers have completed and will not fire again. This method
     * checks all triggers in the scheduler to determine if any are still active or may fire in the
     * future. If all triggers are finalized, the scheduler is shut down gracefully.
     *
     * @param scheduler The Quartz scheduler to potentially shut down
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
