/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core.jobs;

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.core.ProgressMonitor;
import de.rub.nds.crawler.data.*;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.targetlist.ITargetListProvider;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class PublishBulkScanJob implements Job {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Executes the bulk scan job which creates scan jobs for a list of targets. This method
     * performs the following operations:
     *
     * <ul>
     *   <li>Creates and persists a BulkScan entity
     *   <li>Filters targets based on denylist and DNS resolution
     *   <li>Submits valid scan jobs to the orchestration provider
     *   <li>Updates bulk scan statistics with job counts
     * </ul>
     *
     * @param context the job execution context containing configuration and provider instances
     * @throws JobExecutionException if an error occurs during job execution
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDataMap data = context.getMergedJobDataMap();

            ControllerCommandConfig controllerConfig = (ControllerCommandConfig) data.get("config");
            IOrchestrationProvider orchestrationProvider =
                    (IOrchestrationProvider) data.get("orchestrationProvider");
            IPersistenceProvider persistenceProvider =
                    (IPersistenceProvider) data.get("persistenceProvider");
            ITargetListProvider targetListProvider =
                    (ITargetListProvider) data.get("targetListProvider");
            IDenylistProvider denylistProvider = (IDenylistProvider) data.get("denylistProvider");
            ProgressMonitor progressMonitor = (ProgressMonitor) data.get("progressMonitor");

            // Create Bulk Scan and write to DB
            LOGGER.info("Initializing BulkScan");
            BulkScan bulkScan = controllerConfig.createBulkScan();

            List<String> targetStringList = targetListProvider.getTargetList();
            bulkScan.setTargetsGiven(targetStringList.size());

            persistenceProvider.insertBulkScan(bulkScan);
            LOGGER.info("Persisted BulkScan with id: {}", bulkScan.get_id());

            if (controllerConfig.isMonitored()) {
                progressMonitor.startMonitoringBulkScanProgress(bulkScan);
            }

            // create and submit scan jobs for valid hosts
            LOGGER.info(
                    "Filtering out denylisted hosts and hosts where the domain can not be resolved.");
            var submitter =
                    new JobSubmitter(
                            orchestrationProvider,
                            persistenceProvider,
                            denylistProvider,
                            bulkScan,
                            controllerConfig.getPort());
            var parsedJobStatuses =
                    targetStringList.parallelStream()
                            .map(submitter)
                            .collect(
                                    Collectors.groupingBy(
                                            Function.identity(), Collectors.counting()));

            long submittedJobs = parsedJobStatuses.getOrDefault(JobStatus.TO_BE_EXECUTED, 0L);
            long unresolvedJobs = parsedJobStatuses.getOrDefault(JobStatus.UNRESOLVABLE, 0L);
            long denylistedJobs = parsedJobStatuses.getOrDefault(JobStatus.DENYLISTED, 0L);
            long resolutionErrorJobs =
                    parsedJobStatuses.getOrDefault(JobStatus.RESOLUTION_ERROR, 0L);
            bulkScan.setScanJobsPublished(submittedJobs);
            bulkScan.setScanJobsResolutionErrors(unresolvedJobs + resolutionErrorJobs);
            bulkScan.setScanJobsDenylisted(denylistedJobs);
            persistenceProvider.updateBulkScan(bulkScan);

            if (controllerConfig.isMonitored() && submittedJobs == 0) {
                progressMonitor.stopMonitoringAndFinalizeBulkScan(bulkScan.get_id());
            }
            LOGGER.info(
                    "Submitted {} scan jobs to RabbitMq (Not submitted: {} Unresolvable, {} Denylisted, {} unhandled Error)",
                    submittedJobs,
                    unresolvedJobs,
                    denylistedJobs,
                    resolutionErrorJobs);
        } catch (Exception e) {
            LOGGER.error("Exception while publishing BulkScan: ", e);
            JobExecutionException e2 = new JobExecutionException(e);
            e2.setUnscheduleAllTriggers(true);
            throw e2;
        }
    }

    private static class JobSubmitter implements Function<String, JobStatus> {
        private final IOrchestrationProvider orchestrationProvider;
        private final IPersistenceProvider persistenceProvider;
        private final IDenylistProvider denylistProvider;
        private final BulkScan bulkScan;
        private final int defaultPort;

        /**
         * Constructs a JobSubmitter for processing and submitting scan jobs.
         *
         * @param orchestrationProvider provider for submitting scan jobs to the message queue
         * @param persistenceProvider provider for persisting scan results
         * @param denylistProvider provider for checking if targets are denylisted
         * @param bulkScan the bulk scan context for which jobs are being created
         * @param defaultPort the default port to use if not specified in target string
         */
        public JobSubmitter(
                IOrchestrationProvider orchestrationProvider,
                IPersistenceProvider persistenceProvider,
                IDenylistProvider denylistProvider,
                BulkScan bulkScan,
                int defaultPort) {
            this.orchestrationProvider = orchestrationProvider;
            this.persistenceProvider = persistenceProvider;
            this.denylistProvider = denylistProvider;
            this.bulkScan = bulkScan;
            this.defaultPort = defaultPort;
        }

        /**
         * Processes a target string to create and submit a scan job. This method performs the
         * following operations:
         *
         * <ul>
         *   <li>Parses the target string and resolves the hostname
         *   <li>Checks if the target is denylisted
         *   <li>Creates a scan job description
         *   <li>Submits valid jobs to the orchestration provider
         *   <li>Persists error results for invalid targets
         * </ul>
         *
         * @param targetString the target to scan (hostname, IP address, or hostname:port)
         * @return the status of the job (TO_BE_EXECUTED, DENYLISTED, UNRESOLVABLE, or
         *     RESOLUTION_ERROR)
         */
        @Override
        public JobStatus apply(String targetString) {
            ScanJobDescription jobDescription;
            ScanResult errorResult = null;
            try {
                var targetInfo =
                        ScanTarget.fromTargetString(targetString, defaultPort, denylistProvider);
                jobDescription =
                        new ScanJobDescription(
                                targetInfo.getLeft(), bulkScan, targetInfo.getRight());
            } catch (Exception e) {
                jobDescription =
                        new ScanJobDescription(
                                new ScanTarget(), bulkScan, JobStatus.RESOLUTION_ERROR);
                errorResult = ScanResult.fromException(jobDescription, e);
                LOGGER.error(
                        "Error while creating ScanJobDescription for target '{}'", targetString, e);
            }

            if (jobDescription.getStatus() == JobStatus.TO_BE_EXECUTED) {
                orchestrationProvider.submitScanJob(jobDescription);
            } else {
                if (errorResult == null) {
                    errorResult = new ScanResult(jobDescription, null);
                }
                persistenceProvider.insertScanResult(errorResult, jobDescription);
            }
            return jobDescription.getStatus();
        }
    }
}
