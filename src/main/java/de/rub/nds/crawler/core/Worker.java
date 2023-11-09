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
import de.rub.nds.crawler.data.ScanJob;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.scans.Scan;
import java.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Worker that subscribe to scan job queue, initializes thread pool and submits received scan jobs
 * to thread pool.
 */
public class Worker {
    private static final Logger LOGGER = LogManager.getLogger();

    private final IOrchestrationProvider orchestrationProvider;
    private final IPersistenceProvider persistenceProvider;

    private final int maxThreadCount;
    private final int parallelProbeThreads;
    private final int scanTimeout;

    private final ThreadPoolExecutor executor;
    private final ThreadPoolExecutor timeoutExecutor;

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
        this.maxThreadCount = commandConfig.getNumberOfThreads();
        this.parallelProbeThreads = commandConfig.getParallelProbeThreads();
        this.scanTimeout = commandConfig.getScanTimeout();

        executor =
                new ThreadPoolExecutor(
                        maxThreadCount,
                        maxThreadCount,
                        5,
                        TimeUnit.MINUTES,
                        new LinkedBlockingDeque<>());
        timeoutExecutor =
                new ThreadPoolExecutor(
                        maxThreadCount,
                        maxThreadCount,
                        5,
                        TimeUnit.MINUTES,
                        new LinkedBlockingDeque<>());
    }

    public void start() {
        this.orchestrationProvider.registerScanJobConsumer(
                this::handleScanJob, this.maxThreadCount);
    }

    private void handleScanJob(ScanJob scanJob, long deliveryTag) {
        scanJob.setDeliveryTag(deliveryTag);
        submitWithTimeout(
                scanJob.createRunnable(
                        orchestrationProvider, persistenceProvider, parallelProbeThreads));
    }

    private void submitWithTimeout(Scan scan) {
        timeoutExecutor.submit(
                () -> {
                    Future<?> future = null;
                    try {
                        future = executor.submit(scan);
                        future.get(this.scanTimeout, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Could not submit a scan to the worker thread with error ", e);
                    } catch (TimeoutException e) {
                        LOGGER.info(
                                "Trying to shutdown scan of '{}' because timeout reached",
                                scan.getScanJob().getScanTarget());
                        scan.cancel();
                        future.cancel(true);
                    }
                });
    }
}
