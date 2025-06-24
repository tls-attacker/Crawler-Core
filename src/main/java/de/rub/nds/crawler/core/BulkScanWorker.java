/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.util.CanceallableThreadPoolExecutor;
import de.rub.nds.scanner.core.execution.NamedThreadFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

public abstract class BulkScanWorker<T extends ScanConfig> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shouldCleanupSelf = new AtomicBoolean(false);
    private final Object initializationLock = new Object();
    protected final String bulkScanId;
    protected final T scanConfig;

    /**
     * Calls the inner scan function and may handle cleanup. This is needed to wrap the scanner into
     * a future object such that we can handle timeouts properly.
     */
    private final ThreadPoolExecutor timeoutExecutor;

    protected BulkScanWorker(String bulkScanId, T scanConfig, int parallelScanThreads) {
        this.bulkScanId = bulkScanId;
        this.scanConfig = scanConfig;

        timeoutExecutor =
                new CanceallableThreadPoolExecutor(
                        parallelScanThreads,
                        parallelScanThreads,
                        5,
                        TimeUnit.MINUTES,
                        new LinkedBlockingDeque<>(),
                        new NamedThreadFactory("crawler-worker: scan executor"));
    }

    /**
     * Handles a scan request for the given target by submitting it to the executor. Manages
     * initialization and cleanup lifecycle of the worker, ensuring that initialization happens
     * before the first scan and cleanup happens after the last active job completes.
     *
     * @param scanTarget The target to scan, containing connection details
     * @return A Future containing the scan result as a Document
     */
    public Future<Document> handle(ScanTarget scanTarget) {
        // if we initialized ourself, we also clean up ourself
        shouldCleanupSelf.weakCompareAndSetAcquire(false, init());
        activeJobs.incrementAndGet();
        return timeoutExecutor.submit(
                () -> {
                    Document result = scan(scanTarget);
                    if (activeJobs.decrementAndGet() == 0 && shouldCleanupSelf.get()) {
                        cleanup();
                    }
                    return result;
                });
    }

    /**
     * Performs the actual scan of the specified target. This method is called by the executor
     * thread pool and should contain the core scanning logic. Implementations should handle all
     * aspects of connecting to and analyzing the target according to the configured scan
     * parameters.
     *
     * @param scanTarget The target to scan, containing connection details
     * @return A Document containing the scan results, or null if the scan produced no results
     */
    public abstract Document scan(ScanTarget scanTarget);

    /**
     * Initializes the bulk scan worker if it hasn't been initialized yet. This method is
     * thread-safe and ensures that initialization happens exactly once, even when called
     * concurrently from multiple threads. The actual initialization logic is delegated to the
     * abstract initInternal() method.
     *
     * @return true if this call performed the initialization, false if the worker was already
     *     initialized
     */
    public final boolean init() {
        // synchronize such that no thread runs before being initialized
        // but only synchronize if not already initialized
        if (!initialized.get()) {
            synchronized (initializationLock) {
                if (!initialized.getAndSet(true)) {
                    initInternal();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cleans up resources used by the bulk scan worker. This method is thread-safe and ensures
     * cleanup happens exactly once. If there are still active jobs running, cleanup is deferred
     * until all jobs complete. The actual cleanup logic is delegated to the abstract
     * cleanupInternal() method.
     *
     * @return true if cleanup was performed immediately, false if cleanup was deferred due to
     *     active jobs or if already cleaned up
     */
    public final boolean cleanup() {
        // synchronize such that init and cleanup do not run simultaneously
        // but only synchronize if already initialized
        if (initialized.get()) {
            synchronized (initializationLock) {
                if (activeJobs.get() > 0) {
                    shouldCleanupSelf.set(true);
                    LOGGER.warn(
                            "Was told to cleanup while still running; Enqueuing cleanup for later");
                    return false;
                }
                if (initialized.getAndSet(false)) {
                    cleanupInternal();
                    shouldCleanupSelf.set(false);
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract void initInternal();

    protected abstract void cleanupInternal();
}
