/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.BulkScanJobCounters;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.orchestration.DoneNotificationConsumer;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Real-time progress monitoring system for TLS-Crawler bulk scanning operations.
 *
 * <p>The ProgressMonitor provides comprehensive tracking and reporting of bulk scan progress by
 * consuming completion notifications from worker instances. It maintains detailed statistics,
 * calculates performance metrics, and provides estimated completion times for running scans.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li><strong>Progress Tracking</strong> - Real-time monitoring of scan job completion
 *   <li><strong>Performance Metrics</strong> - Global and moving average completion times
 *   <li><strong>Status Categorization</strong> - Detailed breakdown by job completion status
 *   <li><strong>ETA Calculation</strong> - Estimated time to completion based on current rates
 *   <li><strong>Completion Notifications</strong> - HTTP callbacks when scans finish
 *   <li><strong>Automatic Cleanup</strong> - Resource management and scheduler shutdown
 * </ul>
 *
 * <p><strong>Monitoring Architecture:</strong>
 *
 * <ul>
 *   <li>Registers consumers for bulk scan completion notifications via orchestration provider
 *   <li>Maintains per-scan job counters and statistics in memory
 *   <li>Updates persistence layer with final scan results and metadata
 *   <li>Integrates with Quartz scheduler for automatic controller shutdown
 * </ul>
 *
 * <p><strong>Performance Analysis:</strong>
 *
 * <ul>
 *   <li><strong>Global Average</strong> - Overall time per scan job since scan start
 *   <li><strong>Moving Average</strong> - Exponential moving average for recent performance
 *   <li><strong>Adaptive Alpha</strong> - Dynamic smoothing factor based on sample size
 *   <li><strong>ETA Prediction</strong> - Remaining time estimate using moving average
 * </ul>
 *
 * <p><strong>Status Categories:</strong> Tracks completion status including SUCCESS, EMPTY,
 * TIMEOUT, ERROR, SERIALIZATION_ERROR, and INTERNAL_ERROR for detailed failure analysis.
 *
 * <p><strong>Notification Integration:</strong> Supports HTTP POST notifications with
 * JSON-serialized BulkScan objects for external system integration and workflow automation.
 *
 * @see BulkScanJobCounters
 * @see IOrchestrationProvider
 * @see IPersistenceProvider
 * @see DoneNotificationConsumer
 * @see JobStatus
 */
public class ProgressMonitor {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, BulkScanJobCounters> scanJobDetailsById;

    private final IOrchestrationProvider orchestrationProvider;

    private final IPersistenceProvider persistenceProvider;

    private final Scheduler scheduler;

    private boolean listenerRegistered;

    /**
     * Creates a new progress monitor with required dependencies for scan tracking.
     *
     * <p>This constructor initializes the progress monitoring system with the necessary components
     * for tracking bulk scan progress, managing job counters, and coordinating with the distributed
     * scanning infrastructure.
     *
     * <p><strong>Component Responsibilities:</strong>
     *
     * <ul>
     *   <li><strong>Orchestration Provider</strong> - Receives completion notifications from
     *       workers
     *   <li><strong>Persistence Provider</strong> - Updates scan metadata and final results
     *   <li><strong>Scheduler</strong> - Manages controller lifecycle and automatic shutdown
     * </ul>
     *
     * <p><strong>Initialization:</strong> Sets up the internal job counter map and prepares the
     * monitor for tracking multiple concurrent bulk scan operations.
     *
     * @param orchestrationProvider the provider for worker communication and notifications
     * @param persistenceProvider the provider for database operations and result storage
     * @param scheduler the Quartz scheduler for controller lifecycle management
     */
    public ProgressMonitor(
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider,
            Scheduler scheduler) {
        this.scanJobDetailsById = new HashMap<>();
        this.orchestrationProvider = orchestrationProvider;
        this.persistenceProvider = persistenceProvider;
        this.scheduler = scheduler;
    }

    /**
     * Inner class that implements completion notification consumption for individual bulk scans.
     *
     * <p>This class handles the real-time processing of scan job completion notifications,
     * maintaining performance metrics, calculating ETAs, and providing detailed progress logging
     * for a specific bulk scan operation.
     *
     * <p><strong>Performance Tracking:</strong>
     *
     * <ul>
     *   <li><strong>Global Average</strong> - Total time divided by completed jobs
     *   <li><strong>Moving Average</strong> - Exponential smoothing of recent completion times
     *   <li><strong>Adaptive Alpha</strong> - Dynamic smoothing factor (0.1 after 20 jobs, adaptive
     *       before)
     *   <li><strong>ETA Calculation</strong> - Estimated completion time based on moving average
     * </ul>
     *
     * <p><strong>Logging Features:</strong> Provides comprehensive progress logging including
     * completion counts, performance metrics, status breakdowns, and estimated completion times.
     *
     * @see DoneNotificationConsumer
     * @see BulkScan
     * @see BulkScanJobCounters
     */
    private class BulkscanMonitor implements DoneNotificationConsumer {
        private final BulkScan bulkScan;
        private final BulkScanJobCounters counters;
        private final String bulkScanId;
        private double movingAverageDuration = -1;
        private long lastTime = System.currentTimeMillis();

        /**
         * Creates a new bulk scan monitor for the specified scan and counters.
         *
         * @param bulkScan the bulk scan to monitor
         * @param counters the job counters for tracking completion statistics
         */
        public BulkscanMonitor(BulkScan bulkScan, BulkScanJobCounters counters) {
            this.bulkScan = bulkScan;
            this.counters = counters;
            this.bulkScanId = bulkScan.get_id();
        }

        /**
         * Formats a time duration in milliseconds into a human-readable string.
         *
         * <p>This method provides adaptive time formatting that automatically selects the most
         * appropriate time unit based on the magnitude of the duration.
         *
         * <p><strong>Format Rules:</strong>
         *
         * <ul>
         *   <li>&lt; 1 second: "XXX ms"
         *   <li>&lt; 100 seconds: "XX.XX s"
         *   <li>&lt; 100 minutes: "XX m XX s"
         *   <li>&lt; 48 hours: "XX h XX m"
         *   <li>&gt;= 48 hours: "XX.X d"
         * </ul>
         *
         * @param millis the duration in milliseconds to format
         * @return formatted time string with appropriate units
         */
        private String formatTime(double millis) {
            if (millis < 1000) {
                return String.format("%4.0f ms", millis);
            }
            double seconds = millis / 1000;
            if (seconds < 100) {
                return String.format("%5.2f s", seconds);
            }

            double minutes = seconds / 60;
            seconds = seconds % 60;
            if (minutes < 100) {
                return String.format("%2.0f m %2.0f s", minutes, seconds);
            }
            double hours = minutes / 60;
            minutes = minutes % 60;
            if (hours < 48) {
                return String.format("%2.0f h %2.0f m", hours, minutes);
            }
            double days = hours / 24;
            return String.format("%.1f d", days);
        }

        /**
         * Processes a scan job completion notification and updates progress metrics.
         *
         * <p>This method implements the core progress tracking logic, updating job counters,
         * calculating performance metrics, logging progress information, and determining when the
         * bulk scan is complete.
         *
         * <p><strong>Processing Steps:</strong>
         *
         * <ol>
         *   <li>Updates job status counters and gets total completion count
         *   <li>Calculates global average duration since scan start
         *   <li>Updates exponential moving average with adaptive alpha
         *   <li>Computes estimated time to completion (ETA)
         *   <li>Logs comprehensive progress information
         *   <li>Triggers bulk scan finalization if all jobs complete
         * </ol>
         *
         * <p><strong>Performance Metrics:</strong>
         *
         * <ul>
         *   <li><strong>Alpha Calculation</strong> - 2/(totalDone+1) for first 20 jobs, 0.1 after
         *   <li><strong>Moving Average</strong> - α × current_duration + (1-α) × previous_average
         *   <li><strong>ETA</strong> - (remaining_jobs × moving_average_duration)
         * </ul>
         *
         * @param consumerTag the RabbitMQ consumer tag for this notification
         * @param scanJob the completed scan job description
         */
        @Override
        public void consumeDoneNotification(String consumerTag, ScanJobDescription scanJob) {
            try {
                long totalDone = counters.increaseJobStatusCount(scanJob.getStatus());
                long expectedTotal =
                        bulkScan.getScanJobsPublished() != 0
                                ? bulkScan.getScanJobsPublished()
                                : bulkScan.getTargetsGiven();
                long now = System.currentTimeMillis();
                // global average
                double globalAverageDuration = (now - bulkScan.getStartTime()) / (double) totalDone;
                // exponential moving average
                // start with a large alpha to not over-emphasize the first results
                double alpha = totalDone > 20 ? 0.1 : 2 / (double) (totalDone + 1);
                long duration = now - lastTime;
                lastTime = now;
                movingAverageDuration = alpha * duration + (1 - alpha) * movingAverageDuration;

                double eta = (expectedTotal - totalDone) * movingAverageDuration;
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "BulkScan '{}' - {} of {} scan jobs done | Global Average {}/report | Moving Average {}/report | ETA: {}",
                            bulkScanId,
                            totalDone,
                            expectedTotal,
                            formatTime(globalAverageDuration),
                            formatTime(movingAverageDuration),
                            formatTime(eta));
                    LOGGER.info(
                            "BulkScan '{}' - Successful: {} | Empty: {} | Timeout: {} | Error: {} | Serialization Error: {} | Internal Error: {}",
                            bulkScanId,
                            counters.getJobStatusCount(JobStatus.SUCCESS),
                            counters.getJobStatusCount(JobStatus.EMPTY),
                            counters.getJobStatusCount(JobStatus.CANCELLED),
                            counters.getJobStatusCount(JobStatus.ERROR),
                            counters.getJobStatusCount(JobStatus.SERIALIZATION_ERROR),
                            counters.getJobStatusCount(JobStatus.INTERNAL_ERROR));
                }
                if (totalDone == expectedTotal) {
                    stopMonitoringAndFinalizeBulkScan(scanJob.getBulkScanInfo().getBulkScanId());
                }
            } catch (Exception e) {
                LOGGER.error("Exception in done notification consumer:", e);
            }
        }
    }

    /**
     * Initiates progress monitoring for a bulk scan operation.
     *
     * <p>This method sets up real-time progress tracking for the specified bulk scan by creating
     * job counters, registering notification consumers, and preparing the monitoring infrastructure
     * for scan job completion notifications.
     *
     * <p><strong>Setup Process:</strong>
     *
     * <ol>
     *   <li>Creates BulkScanJobCounters for the scan
     *   <li>Registers the scan in the internal tracking map
     *   <li>Sets up BulkscanMonitor as notification consumer
     *   <li>Registers with orchestration provider for completion notifications
     * </ol>
     *
     * <p><strong>Monitoring Features:</strong>
     *
     * <ul>
     *   <li>Real-time job completion counting by status
     *   <li>Performance metric calculation and ETA estimation
     *   <li>Comprehensive progress logging
     *   <li>Automatic scan finalization when complete
     * </ul>
     *
     * <p><strong>Note:</strong> The listener registration is performed only once per
     * ProgressMonitor instance to avoid duplicate registrations.
     *
     * @param bulkScan the bulk scan operation to monitor for progress
     * @see BulkScanJobCounters
     * @see BulkscanMonitor
     * @see IOrchestrationProvider#registerDoneNotificationConsumer(BulkScan,
     *     DoneNotificationConsumer)
     */
    public void startMonitoringBulkScanProgress(BulkScan bulkScan) {
        final BulkScanJobCounters counters = new BulkScanJobCounters(bulkScan);
        scanJobDetailsById.put(bulkScan.get_id(), counters);

        if (!listenerRegistered) {
            orchestrationProvider.registerDoneNotificationConsumer(
                    bulkScan, new BulkscanMonitor(bulkScan, counters));
            listenerRegistered = true;
        }
    }

    /**
     * Finalizes a completed bulk scan and performs cleanup operations.
     *
     * <p>This method handles the complete finalization workflow when a bulk scan reaches
     * completion, including database updates, notification delivery, resource cleanup, and
     * controller shutdown coordination.
     *
     * <p><strong>Finalization Workflow:</strong>
     *
     * <ol>
     *   <li><strong>Status Update</strong> - Marks scan as finished with end timestamp
     *   <li><strong>Statistics Collection</strong> - Updates final job status counters
     *   <li><strong>Database Persistence</strong> - Saves updated BulkScan to database
     *   <li><strong>Memory Cleanup</strong> - Removes scan from active monitoring map
     *   <li><strong>HTTP Notification</strong> - Sends completion callback if configured
     *   <li><strong>Controller Shutdown</strong> - Initiates shutdown if all scans complete
     * </ol>
     *
     * <p><strong>Notification Handling:</strong>
     *
     * <ul>
     *   <li>HTTP POST with JSON-serialized BulkScan object
     *   <li>Comprehensive error handling and logging
     *   <li>Thread interruption handling for graceful shutdown
     * </ul>
     *
     * <p><strong>Automatic Shutdown:</strong> When all monitored bulk scans complete and the
     * scheduler is shut down, automatically closes orchestration provider connections for clean
     * termination.
     *
     * @param bulkScanId the unique identifier of the bulk scan to finalize
     * @see #notify(BulkScan)
     * @see IPersistenceProvider#updateBulkScan(BulkScan)
     * @see IOrchestrationProvider#closeConnection()
     */
    public void stopMonitoringAndFinalizeBulkScan(String bulkScanId) {
        LOGGER.info("BulkScan '{}' is finished", bulkScanId);
        BulkScanJobCounters bulkScanJobCounters = scanJobDetailsById.get(bulkScanId);
        BulkScan scan = bulkScanJobCounters.getBulkScan();
        scan.setFinished(true);
        scan.setEndTime(System.currentTimeMillis());
        scan.setSuccessfulScans(bulkScanJobCounters.getJobStatusCount(JobStatus.SUCCESS));
        scan.setJobStatusCounters(bulkScanJobCounters.getJobStatusCountersCopy());
        persistenceProvider.updateBulkScan(scan);
        LOGGER.info("Persisted updated BulkScan with id: {}", scan.get_id());

        scanJobDetailsById.remove(bulkScanId);

        if (scan.getNotifyUrl() != null
                && !scan.getNotifyUrl().isEmpty()
                && !scan.getNotifyUrl().isBlank()) {
            try {
                String response = notify(scan);
                LOGGER.info(
                        "BulkScan {}(id={}): sent notification to '{}' got response: '{}'",
                        scan.getName(),
                        scan.get_id(),
                        scan.getNotifyUrl(),
                        response);
            } catch (IOException e) {
                LOGGER.error(
                        "Could not send notification for bulkScan '{}' because: ", bulkScanId, e);
            } catch (InterruptedException e) {
                LOGGER.error(
                        "Could not send notification for bulkScan '{}' because we were interrupted: ",
                        bulkScanId,
                        e);
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (scanJobDetailsById.isEmpty() && scheduler.isShutdown()) {
                LOGGER.info("All bulkScans are finished. Closing rabbitMq connection.");
                orchestrationProvider.closeConnection();
            }
        } catch (SchedulerException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Sends an HTTP POST notification with bulk scan completion data.
     *
     * <p>This method implements the HTTP notification feature for external system integration. It
     * serializes the completed BulkScan object as JSON and sends it via HTTP POST to the configured
     * notification URL.
     *
     * <p><strong>Request Configuration:</strong>
     *
     * <ul>
     *   <li><strong>Method</strong> - HTTP POST
     *   <li><strong>Content-Type</strong> - application/json
     *   <li><strong>Body</strong> - Pretty-printed JSON representation of BulkScan
     *   <li><strong>URL</strong> - Taken from BulkScan.getNotifyUrl()
     * </ul>
     *
     * <p><strong>JSON Serialization:</strong> Uses Jackson ObjectMapper with default
     * pretty-printing to create a comprehensive JSON representation including all scan metadata,
     * statistics, and results.
     *
     * <p><strong>HTTP Client:</strong> Uses Java 11+ HttpClient for modern, efficient HTTP
     * communication with automatic connection management.
     *
     * @param bulkScan the completed bulk scan to send notification for
     * @return the HTTP response body as a string
     * @throws IOException if network communication fails
     * @throws InterruptedException if the HTTP request is interrupted
     * @see ObjectMapper
     * @see HttpClient
     * @see HttpRequest
     */
    private static String notify(BulkScan bulkScan) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody =
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bulkScan);

        HttpRequest request =
                HttpRequest.newBuilder(URI.create(bulkScan.getNotifyUrl()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString())
                .body();
    }
}
