/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.scanner.core.execution.NamedThreadFactory;
import java.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/**
 * Distributed TLS-Crawler worker instance responsible for consuming scan jobs and executing TLS
 * scans.
 *
 * <p>The Worker forms the core execution unit of the TLS-Crawler distributed scanning architecture.
 * It consumes scan job messages from the orchestration provider (typically RabbitMQ), executes TLS
 * scans using configurable thread pools, and persists results to the database. Each worker instance
 * can handle multiple concurrent scan jobs while providing comprehensive error handling and timeout
 * management.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li><strong>Job Consumption</strong> - Subscribes to scan job queue for continuous processing
 *   <li><strong>Concurrent Execution</strong> - Manages multiple parallel scan threads
 *   <li><strong>Timeout Management</strong> - Enforces scan timeouts with graceful cancellation
 *   <li><strong>Result Persistence</strong> - Stores scan results with comprehensive error handling
 *   <li><strong>Status Reporting</strong> - Notifies orchestration provider of job completion
 *   <li><strong>Resource Management</strong> - Proper cleanup and thread lifecycle management
 * </ul>
 *
 * <p><strong>Threading Architecture:</strong>
 *
 * <ul>
 *   <li><strong>Scan Threads</strong> - Parallel execution of individual TLS scans via
 *       BulkScanWorkerManager
 *   <li><strong>Result Handler Threads</strong> - Dedicated threads for result processing and
 *       persistence
 *   <li><strong>Connection Threads</strong> - Shared thread pool for network connections within
 *       scans
 *   <li><strong>Thread Pools</strong> - Fixed-size pools with graceful shutdown and resource
 *       cleanup
 * </ul>
 *
 * <p><strong>Execution Workflow:</strong>
 *
 * <ol>
 *   <li><strong>Job Reception</strong> - Receives ScanJobDescription from orchestration provider
 *   <li><strong>Scan Execution</strong> - Delegates to BulkScanWorkerManager for actual scanning
 *   <li><strong>Result Waiting</strong> - Waits for scan completion with configurable timeout
 *   <li><strong>Error Handling</strong> - Categorizes failures and creates appropriate ScanResult
 *   <li><strong>Persistence</strong> - Stores results and metadata in persistence provider
 *   <li><strong>Notification</strong> - Sends completion notification for progress tracking
 * </ol>
 *
 * <p><strong>Timeout Management:</strong>
 *
 * <ul>
 *   <li><strong>Primary Timeout</strong> - Configurable scan timeout (default 14 minutes)
 *   <li><strong>Graceful Shutdown</strong> - Attempts to cancel running scans on timeout
 *   <li><strong>Final Timeout</strong> - 10-second deadline for scan termination after cancellation
 *   <li><strong>Status Tracking</strong> - Proper JobStatus assignment for timeout scenarios
 * </ul>
 *
 * <p><strong>Error Categories:</strong>
 *
 * <ul>
 *   <li><strong>SUCCESS</strong> - Scan completed successfully with results
 *   <li><strong>EMPTY</strong> - Scan completed but produced no results
 *   <li><strong>CANCELLED</strong> - Scan timed out and was cancelled
 *   <li><strong>ERROR</strong> - Scanner-level execution exception
 *   <li><strong>CRAWLER_ERROR</strong> - Unexpected worker-level exception
 *   <li><strong>INTERNAL_ERROR</strong> - Worker interruption or persistence failure
 * </ul>
 *
 * <p><strong>Resource Safety:</strong> The worker ensures proper resource cleanup through thread
 * pool management, graceful shutdown handling, and comprehensive exception catching to prevent
 * resource leaks in long-running distributed environments.
 *
 * @see WorkerCommandConfig
 * @see IOrchestrationProvider
 * @see IPersistenceProvider
 * @see BulkScanWorkerManager
 * @see ScanJobDescription
 * @see ScanResult
 * @see JobStatus
 */
public class Worker {
    private static final Logger LOGGER = LogManager.getLogger();

    private final IOrchestrationProvider orchestrationProvider;
    private final IPersistenceProvider persistenceProvider;

    private final int parallelScanThreads;
    private final int parallelConnectionThreads;
    private final int scanTimeout;

    /** Runs a lambda which waits for the scanning result and persists it. */
    private final ThreadPoolExecutor workerExecutor;

    /**
     * Creates a new TLS-Crawler worker with the specified configuration and providers.
     *
     * <p>This constructor initializes the worker with all necessary components for distributed TLS
     * scanning operations. It extracts configuration parameters from the command config and sets up
     * the thread pool executor for result handling.
     *
     * <p><strong>Thread Pool Configuration:</strong>
     *
     * <ul>
     *   <li><strong>Core/Max Threads</strong> - Equal to parallelScanThreads for fixed pool size
     *   <li><strong>Keep-Alive Time</strong> - 5 minutes for idle thread cleanup
     *   <li><strong>Queue</strong> - LinkedBlockingDeque for unlimited task queuing
     *   <li><strong>Thread Factory</strong> - Named threads for debugging ("crawler-worker: result
     *       handler")
     * </ul>
     *
     * <p><strong>Configuration Extraction:</strong> The constructor extracts key parameters from
     * the WorkerCommandConfig including thread counts and timeout values for scan execution.
     *
     * @param commandConfig the worker configuration containing thread counts and timeout settings
     * @param orchestrationProvider the provider for message queue communication and job consumption
     * @param persistenceProvider the provider for database operations and result storage
     * @throws NullPointerException if any parameter is null
     */
    public Worker(
            WorkerCommandConfig commandConfig,
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider) {
        this.orchestrationProvider = orchestrationProvider;
        this.persistenceProvider = persistenceProvider;
        this.parallelScanThreads = commandConfig.getParallelScanThreads();
        this.parallelConnectionThreads = commandConfig.getParallelConnectionThreads();
        this.scanTimeout = commandConfig.getScanTimeout();

        workerExecutor =
                new ThreadPoolExecutor(
                        parallelScanThreads,
                        parallelScanThreads,
                        5,
                        TimeUnit.MINUTES,
                        new LinkedBlockingDeque<>(),
                        new NamedThreadFactory("crawler-worker: result handler"));
    }

    /**
     * Starts the worker by registering for scan job consumption from the orchestration provider.
     *
     * <p>This method initiates the worker's primary function by subscribing to the scan job queue.
     * The orchestration provider will begin delivering scan jobs to this worker's handleScanJob
     * method based on the configured parallel scan thread count.
     *
     * <p><strong>Registration Details:</strong>
     *
     * <ul>
     *   <li><strong>Consumer Method</strong> - Uses method reference to handleScanJob
     *   <li><strong>Concurrency Level</strong> - Registers with parallelScanThreads count
     *   <li><strong>Queue Binding</strong> - Connects to the configured scan job queue
     * </ul>
     *
     * <p><strong>Post-Start Behavior:</strong> After calling this method, the worker will begin
     * receiving and processing scan jobs asynchronously until the application shuts down or the
     * orchestration provider connection is closed.
     */
    public void start() {
        this.orchestrationProvider.registerScanJobConsumer(
                this::handleScanJob, this.parallelScanThreads);
    }

    /**
     * Waits for scan completion and handles timeout scenarios with graceful cancellation.
     *
     * <p>This method implements the core timeout and cancellation logic for scan jobs. It waits for
     * the scan to complete within the configured timeout period, and if the timeout is exceeded, it
     * attempts graceful cancellation before enforcing a final deadline.
     *
     * <p><strong>Timeout Handling Strategy:</strong>
     *
     * <ol>
     *   <li><strong>Primary Wait</strong> - Wait up to scanTimeout for normal completion
     *   <li><strong>Cancellation</strong> - On timeout, cancel the future and log attempt
     *   <li><strong>Grace Period</strong> - Allow 10 seconds for graceful shutdown after
     *       cancellation
     *   <li><strong>Status Assignment</strong> - Set appropriate JobStatus based on outcome
     * </ol>
     *
     * <p><strong>Result Processing:</strong>
     *
     * <ul>
     *   <li><strong>SUCCESS</strong> - Non-null result document indicates successful scan
     *   <li><strong>EMPTY</strong> - Null result document indicates no findings
     *   <li><strong>CANCELLED</strong> - Timeout occurred and scan was interrupted
     * </ul>
     *
     * @param resultFuture the future representing the ongoing scan operation
     * @param scanJobDescription the job description to update with final status
     * @return a ScanResult containing the job description and result document
     * @throws ExecutionException if the scan execution encounters an error
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws TimeoutException if the scan cannot be cancelled within the grace period
     */
    private ScanResult waitForScanResult(
            Future<Document> resultFuture, ScanJobDescription scanJobDescription)
            throws ExecutionException, InterruptedException, TimeoutException {
        Document resultDocument;
        JobStatus jobStatus;
        try {
            resultDocument = resultFuture.get(scanTimeout, TimeUnit.MILLISECONDS);
            jobStatus = resultDocument != null ? JobStatus.SUCCESS : JobStatus.EMPTY;
        } catch (TimeoutException e) {
            LOGGER.info(
                    "Trying to shutdown scan of '{}' because timeout reached",
                    scanJobDescription.getScanTarget());
            resultFuture.cancel(true);
            // after interrupting, the scan should return as soon as possible
            resultDocument = resultFuture.get(10, TimeUnit.SECONDS);
            jobStatus = JobStatus.CANCELLED;
        }
        scanJobDescription.setStatus(jobStatus);
        return new ScanResult(scanJobDescription, resultDocument);
    }

    /**
     * Handles incoming scan job messages by initiating scan execution and result processing.
     *
     * <p>This method serves as the main entry point for scan job processing. It receives scan job
     * descriptions from the orchestration provider, delegates the actual scanning to
     * BulkScanWorkerManager, and submits the result handling to the worker thread pool.
     *
     * <p><strong>Processing Flow:</strong>
     *
     * <ol>
     *   <li><strong>Job Reception</strong> - Log incoming scan job for the target
     *   <li><strong>Scan Delegation</strong> - Submit to BulkScanWorkerManager for execution
     *   <li><strong>Async Processing</strong> - Submit result waiting and persistence to thread
     *       pool
     *   <li><strong>Error Handling</strong> - Comprehensive exception handling with status
     *       categorization
     * </ol>
     *
     * <p><strong>Exception Categories:</strong>
     *
     * <ul>
     *   <li><strong>InterruptedException</strong> - Worker shutdown, sets INTERNAL_ERROR status
     *   <li><strong>ExecutionException</strong> - Scanner failure, sets ERROR status
     *   <li><strong>TimeoutException</strong> - Scan timeout, sets CANCELLED status
     *   <li><strong>General Exception</strong> - Unexpected error, sets CRAWLER_ERROR status
     * </ul>
     *
     * <p><strong>Result Persistence:</strong> All scan results are persisted unless an
     * InterruptedException occurs, indicating the worker is shutting down and persistence should be
     * avoided.
     *
     * @param scanJobDescription the scan job to process, containing target and configuration
     *     details
     */
    private void handleScanJob(ScanJobDescription scanJobDescription) {
        LOGGER.info("Received scan job for {}", scanJobDescription.getScanTarget());
        Future<Document> resultFuture =
                BulkScanWorkerManager.handleStatic(
                        scanJobDescription, parallelConnectionThreads, parallelScanThreads);
        workerExecutor.submit(
                () -> {
                    ScanResult scanResult = null;
                    boolean persist = true;
                    try {
                        scanResult = waitForScanResult(resultFuture, scanJobDescription);
                    } catch (InterruptedException e) {
                        LOGGER.error("Worker was interrupted - not persisting anything", e);
                        scanJobDescription.setStatus(JobStatus.INTERNAL_ERROR);
                        persist = false;
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        LOGGER.error(
                                "Scanning of {} failed because of an exception: ",
                                scanJobDescription.getScanTarget(),
                                e);
                        scanJobDescription.setStatus(JobStatus.ERROR);
                        scanResult = ScanResult.fromException(scanJobDescription, e);
                    } catch (TimeoutException e) {
                        LOGGER.info(
                                "Scan of '{}' did not finish in time and did not cancel gracefully",
                                scanJobDescription.getScanTarget());
                        scanJobDescription.setStatus(JobStatus.CANCELLED);
                        resultFuture.cancel(true);
                        scanResult = ScanResult.fromException(scanJobDescription, e);
                    } catch (Exception e) {
                        LOGGER.error(
                                "Scanning of {} failed because of an unexpected exception: ",
                                scanJobDescription.getScanTarget(),
                                e);
                        scanJobDescription.setStatus(JobStatus.CRAWLER_ERROR);
                        scanResult = ScanResult.fromException(scanJobDescription, e);
                    } finally {
                        if (persist) {
                            persistResult(scanJobDescription, scanResult);
                        }
                    }
                });
    }

    /**
     * Persists scan results to the database and notifies the orchestration provider of completion.
     *
     * <p>This method handles the final phase of scan job processing by storing results in the
     * persistence layer and sending completion notifications to the orchestration provider. It
     * provides comprehensive error handling to ensure completion notifications are always sent,
     * even if persistence fails.
     *
     * <p><strong>Persistence Flow:</strong>
     *
     * <ol>
     *   <li><strong>Null Check</strong> - Validate ScanResult is not null
     *   <li><strong>Status Update</strong> - Sync job description status with result status
     *   <li><strong>Database Insert</strong> - Store result and metadata via persistence provider
     *   <li><strong>Error Handling</strong> - Set INTERNAL_ERROR status on persistence failure
     *   <li><strong>Completion Notification</strong> - Always notify orchestration provider
     * </ol>
     *
     * <p><strong>Error Recovery:</strong>
     *
     * <ul>
     *   <li><strong>Null Result</strong> - Logs error and sets INTERNAL_ERROR status
     *   <li><strong>Persistence Exception</strong> - Logs error, sets INTERNAL_ERROR, continues to
     *       notification
     *   <li><strong>Guaranteed Notification</strong> - Completion notification sent regardless of
     *       persistence outcome
     * </ul>
     *
     * <p><strong>Status Synchronization:</strong> The method ensures the ScanJobDescription status
     * matches the ScanResult status before persistence, maintaining consistency across the system.
     *
     * @param scanJobDescription the job description to update and use for notification
     * @param scanResult the scan result to persist, may be null in error scenarios
     */
    private void persistResult(ScanJobDescription scanJobDescription, ScanResult scanResult) {
        try {
            if (scanResult != null) {
                LOGGER.info(
                        "Writing {} result for {}",
                        scanResult.getResultStatus(),
                        scanJobDescription.getScanTarget());
                scanJobDescription.setStatus(scanResult.getResultStatus());
                persistenceProvider.insertScanResult(scanResult, scanJobDescription);
            } else {
                LOGGER.error("ScanResult was null, this should not happen.");
                scanJobDescription.setStatus(JobStatus.INTERNAL_ERROR);
            }
        } catch (Exception e) {
            LOGGER.error("Could not persist result for {}", scanJobDescription.getScanTarget());
            scanJobDescription.setStatus(JobStatus.INTERNAL_ERROR);
        } finally {
            orchestrationProvider.notifyOfDoneScanJob(scanJobDescription);
        }
    }
}
