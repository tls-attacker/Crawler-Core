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

/**
 * Quartz job implementation responsible for initializing and publishing bulk scan operations.
 *
 * <p>The PublishBulkScanJob serves as the main orchestration component that transforms a bulk scan
 * configuration into individual scan jobs distributed to worker instances. It handles the complete
 * job creation workflow including target list processing, filtering, validation, and submission to
 * the message queue infrastructure.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Bulk Scan Initialization</strong> - Creates and persists BulkScan metadata
 *   <li><strong>Target Processing</strong> - Processes target lists with filtering and validation
 *   <li><strong>Job Creation</strong> - Converts targets into individual ScanJobDescription objects
 *   <li><strong>Quality Control</strong> - Filters denylisted and unresolvable targets
 *   <li><strong>Progress Monitoring</strong> - Initializes monitoring for tracked scans
 *   <li><strong>Statistics Collection</strong> - Tracks submission statistics and error counts
 * </ul>
 *
 * <p><strong>Execution Workflow:</strong>
 *
 * <ol>
 *   <li><strong>Configuration Extraction</strong> - Retrieves all required providers from
 *       JobDataMap
 *   <li><strong>BulkScan Creation</strong> - Creates and persists the parent bulk scan object
 *   <li><strong>Target List Retrieval</strong> - Fetches targets from the configured provider
 *   <li><strong>Monitoring Setup</strong> - Initializes progress tracking if enabled
 *   <li><strong>Parallel Processing</strong> - Processes targets concurrently using parallel
 *       streams
 *   <li><strong>Job Submission</strong> - Submits valid jobs to orchestration provider
 *   <li><strong>Statistics Update</strong> - Updates bulk scan with final submission counts
 * </ol>
 *
 * <p><strong>Target Filtering Pipeline:</strong>
 *
 * <ul>
 *   <li><strong>Target Parsing</strong> - Converts string targets to ScanTarget objects
 *   <li><strong>DNS Resolution</strong> - Validates that hostnames can be resolved
 *   <li><strong>Denylist Checking</strong> - Filters out prohibited targets
 *   <li><strong>Error Handling</strong> - Categorizes and persists processing errors
 * </ul>
 *
 * <p><strong>Error Handling:</strong> The job implements comprehensive error handling that
 * categorizes failures into specific JobStatus types (UNRESOLVABLE, DENYLISTED, RESOLUTION_ERROR)
 * and persists error results for analysis while continuing processing of valid targets.
 *
 * <p><strong>Parallel Processing:</strong> Uses Java parallel streams for efficient processing of
 * large target lists, with the JobSubmitter functional interface handling individual target
 * processing and submission.
 *
 * <p><strong>Monitoring Integration:</strong> For monitored scans, sets up ProgressMonitor tracking
 * and handles the special case where no jobs are submitted (immediate completion).
 *
 * @see Job
 * @see ControllerCommandConfig
 * @see BulkScan
 * @see ScanJobDescription
 * @see ProgressMonitor
 * @see IOrchestrationProvider
 * @see ITargetListProvider
 */
public class PublishBulkScanJob implements Job {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a new bulk scan job publisher instance.
     *
     * <p>Default constructor required by the Quartz scheduler framework. The job execution context
     * provides all necessary configuration and dependencies at execution time.
     */
    public PublishBulkScanJob() {
        // Default constructor for Quartz scheduler instantiation
    }

    /**
     * Executes the bulk scan job creation and publication process.
     *
     * <p>This method implements the Quartz Job interface and performs the complete workflow for
     * transforming a bulk scan configuration into individual scan jobs distributed to workers. It
     * handles all aspects of job creation including filtering, validation, and submission while
     * providing comprehensive error handling and statistics collection.
     *
     * <p><strong>Required JobDataMap Entries:</strong>
     *
     * <ul>
     *   <li><strong>config</strong> - ControllerCommandConfig with scan parameters
     *   <li><strong>orchestrationProvider</strong> - IOrchestrationProvider for job submission
     *   <li><strong>persistenceProvider</strong> - IPersistenceProvider for data storage
     *   <li><strong>targetListProvider</strong> - ITargetListProvider for target acquisition
     *   <li><strong>denylistProvider</strong> - IDenylistProvider for target filtering
     *   <li><strong>progressMonitor</strong> - ProgressMonitor for tracking (if enabled)
     * </ul>
     *
     * <p><strong>Execution Steps:</strong>
     *
     * <ol>
     *   <li>Extract configuration and providers from JobDataMap
     *   <li>Create and persist BulkScan object with metadata
     *   <li>Retrieve target list from configured provider
     *   <li>Initialize progress monitoring if enabled
     *   <li>Process targets in parallel using JobSubmitter
     *   <li>Collect statistics and update BulkScan
     *   <li>Handle edge case of zero submitted jobs
     * </ol>
     *
     * <p><strong>Error Handling:</strong> Any exception during execution is caught, logged, and
     * converted to a JobExecutionException with unscheduleAllTriggers=true to prevent retry
     * attempts that would likely fail with the same error.
     *
     * @param context the Quartz job execution context containing configuration and providers
     * @throws JobExecutionException if any error occurs during job execution
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

    /**
     * Functional interface implementation for processing individual target strings into scan jobs.
     *
     * <p>The JobSubmitter class implements the Function interface to enable parallel processing of
     * target lists using Java streams. Each instance processes target strings by parsing,
     * validating, filtering, and either submitting valid jobs or persisting error results.
     *
     * <p><strong>Processing Pipeline:</strong>
     *
     * <ol>
     *   <li><strong>Target Parsing</strong> - Converts string to ScanTarget with DNS resolution
     *   <li><strong>Denylist Checking</strong> - Validates target against configured denylist
     *   <li><strong>Job Creation</strong> - Creates ScanJobDescription with appropriate status
     *   <li><strong>Submission/Persistence</strong> - Submits valid jobs or persists error results
     * </ol>
     *
     * <p><strong>Status Determination:</strong>
     *
     * <ul>
     *   <li><strong>TO_BE_EXECUTED</strong> - Valid target, submitted to orchestration provider
     *   <li><strong>DENYLISTED</strong> - Target blocked by denylist configuration
     *   <li><strong>UNRESOLVABLE</strong> - DNS resolution failed for hostname
     *   <li><strong>RESOLUTION_ERROR</strong> - Unexpected error during target processing
     * </ul>
     *
     * <p><strong>Error Persistence:</strong> All error cases result in ScanResult objects being
     * persisted to maintain complete audit trails and enable analysis of filtering effectiveness
     * and target list quality.
     */
    private static class JobSubmitter implements Function<String, JobStatus> {
        private final IOrchestrationProvider orchestrationProvider;
        private final IPersistenceProvider persistenceProvider;
        private final IDenylistProvider denylistProvider;
        private final BulkScan bulkScan;
        private final int defaultPort;

        /**
         * Creates a new JobSubmitter with the required dependencies for target processing.
         *
         * @param orchestrationProvider provider for submitting valid scan jobs
         * @param persistenceProvider provider for storing error results
         * @param denylistProvider provider for target filtering
         * @param bulkScan the parent bulk scan for job association
         * @param defaultPort the default port to use when not specified in target strings
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
         * Processes a single target string and returns the resulting job status.
         *
         * <p>This method implements the core target processing logic, handling parsing, validation,
         * filtering, and job submission or error persistence. It uses the
         * ScanTarget.fromTargetString method for DNS resolution and denylist checking.
         *
         * <p><strong>Processing Flow:</strong>
         *
         * <ol>
         *   <li>Parse target string using ScanTarget.fromTargetString
         *   <li>Create ScanJobDescription with parsed target and determined status
         *   <li>For valid targets (TO_BE_EXECUTED): submit to orchestration provider
         *   <li>For invalid targets: create and persist ScanResult with error details
         * </ol>
         *
         * <p><strong>Error Handling:</strong> Exceptions during target parsing are caught and
         * result in RESOLUTION_ERROR status with the exception persisted in the ScanResult for
         * debugging purposes.
         *
         * @param targetString the target string to process (e.g., "example.com:443")
         * @return the JobStatus indicating how the target was processed
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
