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
     * Constructs a Controller with the specified configuration and providers.
     *
     * @param config the controller configuration
     * @param orchestrationProvider the provider for job orchestration
     * @param persistenceProvider the provider for data persistence
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
     * Starts the controller and schedules bulk scan publishing jobs.
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
     * Creates a schedule builder based on the configured scan interval.
     *
     * @return a cron schedule builder if cron interval is set, otherwise a simple schedule
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
     * Shuts down the scheduler if all triggers have been finalized.
     *
     * @param scheduler the scheduler to check and potentially shut down
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
