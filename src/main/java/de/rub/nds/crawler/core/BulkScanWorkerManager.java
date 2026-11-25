/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import de.rub.nds.crawler.data.BulkScanInfo;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.exception.UncheckedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/**
 * Each ScanJob has its own BulkScanWorker. This class manages the BulkScanWorkers and ensures that
 * each BulkScanWorker is only created once and is cleaned up after it is not used anymore.
 *
 * <p>More concretely: If a scan with ID 1 is started, a worker is created. This worker will exist
 * as long as scan targets for scan ID 1 are processed. If the worker does not receive jobs for
 * longer than the idle timeout (default 30 minutes), it is considered stale and removed by a
 * periodic cleanup. If a new ID 1 job arrives afterwards, a new worker is created.
 *
 * <p>This class manages the mechanism above and acts as a singleton factory and manager for
 * BulkScanWorker instances.
 */
public class BulkScanWorkerManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile BulkScanWorkerManager instance;

    public static final long DEFAULT_WORKER_IDLE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);

    private static final long MIN_CLEANUP_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long MAX_CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    private static volatile long workerIdleTimeoutMs = DEFAULT_WORKER_IDLE_TIMEOUT_MS;

    /**
     * Gets the singleton instance of the BulkScanWorkerManager. Creates the instance if it doesn't
     * exist yet.
     *
     * @return The singleton instance
     */
    public static BulkScanWorkerManager getInstance() {
        if (instance == null) {
            synchronized (BulkScanWorkerManager.class) {
                if (instance == null) {
                    instance = new BulkScanWorkerManager(workerIdleTimeoutMs);
                }
            }
        }
        return instance;
    }

    /**
     * Sets the time after which a worker without new jobs is cleaned up. Call before the first job.
     *
     * @param idleTimeoutMs The idle timeout in milliseconds, must be positive
     */
    public static void setWorkerIdleTimeout(long idleTimeoutMs) {
        if (idleTimeoutMs <= 0) {
            throw new IllegalArgumentException(
                    "Bulk scan worker idle timeout must be positive, but was " + idleTimeoutMs);
        }
        synchronized (BulkScanWorkerManager.class) {
            workerIdleTimeoutMs = idleTimeoutMs;
            BulkScanWorkerManager oldInstance = instance;
            if (oldInstance != null && oldInstance.idleTimeoutMs != idleTimeoutMs) {
                LOGGER.warn(
                        "Changing bulk scan worker idle timeout from {} ms to {} ms after workers were created; replacing the worker manager",
                        oldInstance.idleTimeoutMs,
                        idleTimeoutMs);
                instance = new BulkScanWorkerManager(idleTimeoutMs);
                oldInstance.shutdown();
            }
        }
    }

    /**
     * Static convenience method to handle a scan job. See also {@link #handle(ScanJobDescription,
     * int, int, IPersistenceProvider)}.
     *
     * @param scanJobDescription The scan job to handle
     * @param parallelConnectionThreads The number of parallel connection threads to use (used to
     *     create worker if it does not exist)
     * @param parallelScanThreads The number of parallel scan threads to use (used to create worker
     *     if it does not exist)
     * @param persistenceProvider The persistence provider for writing partial results
     * @return A ProgressableFuture representing the scan lifecycle
     */
    public static ProgressableFuture<Document> handleStatic(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads,
            IPersistenceProvider persistenceProvider) {
        BulkScanWorkerManager manager = getInstance();
        return manager.handle(
                scanJobDescription,
                parallelConnectionThreads,
                parallelScanThreads,
                persistenceProvider);
    }

    private final Cache<String, BulkScanWorker<?>> bulkScanWorkers;

    private final long idleTimeoutMs;

    // Guava only evicts expired entries on cache access, so evict periodically
    private final ScheduledExecutorService cleanupScheduler;

    BulkScanWorkerManager(long idleTimeoutMs) {
        this.idleTimeoutMs = idleTimeoutMs;
        bulkScanWorkers =
                CacheBuilder.newBuilder()
                        .expireAfterAccess(idleTimeoutMs, TimeUnit.MILLISECONDS)
                        .removalListener(
                                (RemovalListener<String, BulkScanWorker<?>>)
                                        notification -> {
                                            BulkScanWorker<?> worker = notification.getValue();
                                            if (worker != null) {
                                                worker.cleanup();
                                            }
                                        })
                        .build();

        cleanupScheduler =
                Executors.newSingleThreadScheduledExecutor(
                        runnable -> {
                            Thread thread =
                                    new Thread(
                                            runnable, "crawler-worker: bulk scan worker cleanup");
                            thread.setDaemon(true);
                            return thread;
                        });
        long cleanupIntervalMs =
                Math.min(
                        MAX_CLEANUP_INTERVAL_MS,
                        Math.max(MIN_CLEANUP_INTERVAL_MS, idleTimeoutMs / 2));
        cleanupScheduler.scheduleWithFixedDelay(
                this::evictStaleWorkers,
                cleanupIntervalMs,
                cleanupIntervalMs,
                TimeUnit.MILLISECONDS);
    }

    private void evictStaleWorkers() {
        try {
            bulkScanWorkers.cleanUp();
        } catch (RuntimeException e) {
            // an escaping exception would cancel the scheduled task
            LOGGER.warn("Failed to clean up stale bulk scan workers", e);
        }
    }

    /** Stops the periodic cleanup and cleans up all workers. */
    void shutdown() {
        cleanupScheduler.shutdownNow();
        bulkScanWorkers.invalidateAll();
    }

    /**
     * Gets or creates a bulk scan worker for the specified bulk scan. Workers are cached and reused
     * to ensure thread limits.
     *
     * @param bulkScanId The ID of the bulk scan to get the worker for (used as key in the cache)
     * @param scanConfig The scan configuration (used to create worker if it does not exist)
     * @param parallelConnectionThreads The number of parallel connection threads to use (used to
     *     create worker if it does not exist)
     * @param parallelScanThreads The number of parallel scan threads to use (used to create worker
     *     if it does not exist)
     * @param persistenceProvider The persistence provider for writing partial results
     * @return A bulk scan worker for the specified bulk scan
     * @throws UncheckedException If a worker cannot be created
     */
    public BulkScanWorker<?> getBulkScanWorker(
            String bulkScanId,
            ScanConfig scanConfig,
            int parallelConnectionThreads,
            int parallelScanThreads,
            IPersistenceProvider persistenceProvider) {
        try {
            return bulkScanWorkers.get(
                    bulkScanId,
                    () -> {
                        BulkScanWorker<?> ret =
                                scanConfig.createWorker(
                                        bulkScanId,
                                        parallelConnectionThreads,
                                        parallelScanThreads,
                                        persistenceProvider);
                        ret.init();
                        return ret;
                    });
        } catch (ExecutionException e) {
            LOGGER.error("Could not create bulk scan worker", e);
            throw new UncheckedException(e);
        }
    }

    /**
     * Handles a scan job by creating or retrieving the appropriate worker and submitting the scan
     * target for processing.
     *
     * @param scanJobDescription The scan job to handle
     * @param parallelConnectionThreads The number of parallel connection threads to use (used to
     *     create worker if it does not exist)
     * @param parallelScanThreads The number of parallel scan threads to use (used to create worker
     *     if it does not exist)
     * @param persistenceProvider The persistence provider for writing partial results
     * @return A ProgressableFuture representing the scan lifecycle
     */
    public ProgressableFuture<Document> handle(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads,
            IPersistenceProvider persistenceProvider) {
        BulkScanInfo bulkScanInfo = scanJobDescription.getBulkScanInfo();
        BulkScanWorker<?> worker =
                getBulkScanWorker(
                        bulkScanInfo.getBulkScanId(),
                        bulkScanInfo.getScanConfig(),
                        parallelConnectionThreads,
                        parallelScanThreads,
                        persistenceProvider);
        return worker.handle(scanJobDescription);
    }
}
