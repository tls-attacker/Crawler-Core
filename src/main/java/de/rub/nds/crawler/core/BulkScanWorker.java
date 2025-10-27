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

/**
 * Abstract worker for performing bulk scanning operations. Implements thread management and
 * lifecycle operations for scan workers.
 *
 * @param <T> The specific ScanConfig type used by this worker
 */
public abstract class BulkScanWorker<T extends ScanConfig> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shouldCleanupSelf = new AtomicBoolean(false);
    private final Object initializationLock = new Object();
    protected final String bulkScanId;

    /** The scan configuration for this worker */
    protected final T scanConfig;

    /**
     * Calls the inner scan function and may handle cleanup. This is needed to wrap the scanner into
     * a future object such that we can handle timeouts properly.
     */
    private final ThreadPoolExecutor timeoutExecutor;

    /**
     * Creates a new bulk scan worker.
     *
     * @param bulkScanId The ID of the bulk scan this worker is associated with
     * @param scanConfig The scan configuration for this worker
     * @param parallelScanThreads The number of parallel scan threads to use
     */
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
     * Handles a scan target by submitting it to the executor. If this is the first call, it will
     * initialize the worker first. When the last job completes, it will clean up the worker if
     * needed.
     *
     * @param scanTarget The target to scan
     * @return A future that will complete when the scan is done
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
     * Scans a target and returns the result as a Document. This is the core scanning functionality
     * that must be implemented by subclasses.
     *
     * @param scanTarget The target to scan
     * @return The scan result as a Document
     */
    public abstract Document scan(ScanTarget scanTarget);

    /**
     * Initializes this worker if it hasn't been initialized yet. This method is thread-safe and
     * will only initialize once.
     *
     * @return True if this call performed the initialization, false if already initialized
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
     * Cleans up this worker if it has been initialized and has no active jobs. This method is
     * thread-safe and will only clean up once.
     *
     * @return True if this call performed the cleanup, false otherwise
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

    /**
     * Performs the actual initialization of this worker. This method is called exactly once by
     * {@link #init()} when initialization is needed. Subclasses must implement this method to
     * initialize their specific resources.
     */
    protected abstract void initInternal();

    /**
     * Performs the actual cleanup of this worker. This method is called exactly once by {@link
     * #cleanup()} when cleanup is needed. Subclasses must implement this method to clean up their
     * specific resources.
     */
    protected abstract void cleanupInternal();
}
