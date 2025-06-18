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
 * Worker that subscribe to scan job queue, initializes thread pool and submits received scan jobs
 * to thread pool.
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
     * TLS-Crawler constructor.
     *
     * @param commandConfig The config for this worker.
     * @param orchestrationProvider A non-null orchestration provider.
     * @param persistenceProvider A non-null persistence provider.
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

    /** Starts the worker by registering a scan job consumer with the orchestration provider. */
    public void start() {
        this.orchestrationProvider.registerScanJobConsumer(
                this::handleScanJob, this.parallelScanThreads);
    }

    /**
     * Waits for a scan result with timeout handling.
     *
     * @param resultFuture the future containing the scan result document
     * @param scanJobDescription the scan job description
     * @return the scan result
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException if the wait timed out
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
     * Handles a scan job by executing it and managing the result.
     *
     * @param scanJobDescription the scan job to handle
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
     * Persists the scan result and notifies the orchestration provider.
     *
     * @param scanJobDescription the scan job description
     * @param scanResult the result to persist
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
