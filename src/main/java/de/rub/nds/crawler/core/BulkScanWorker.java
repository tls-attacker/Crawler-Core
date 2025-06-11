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
 * Abstract base class for bulk scanning workers that execute TLS scans on individual targets.
 *
 * <p>This class provides the framework for implementing specific scanner workers that can process
 * multiple scan targets concurrently. It handles the lifecycle management, thread pool
 * coordination, and resource cleanup for scanning operations.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Concurrency Management</strong> - Manages a thread pool for parallel scanning
 *   <li><strong>Lifecycle Control</strong> - Handles initialization and cleanup of scanner
 *       resources
 *   <li><strong>Job Tracking</strong> - Tracks active scanning jobs for proper resource management
 *   <li><strong>Thread Safety</strong> - Ensures safe concurrent access to shared resources
 * </ul>
 *
 * <p>Implementations must provide:
 *
 * <ul>
 *   <li>{@link #scan(ScanTarget)} - The actual scanning logic
 *   <li>{@link #initInternal()} - Scanner-specific initialization
 *   <li>{@link #cleanupInternal()} - Scanner-specific cleanup
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is designed to be thread-safe and can handle
 * multiple concurrent scan requests. The initialization and cleanup methods are synchronized to
 * prevent race conditions.
 *
 * <p><strong>Resource Management:</strong> The worker automatically manages its lifecycle,
 * performing initialization on first use and cleanup when no active jobs remain.
 *
 * @param <T> the type of scan configuration used by this worker
 * @see ScanConfig
 * @see ScanTarget
 * @see Worker
 */
public abstract class BulkScanWorker<T extends ScanConfig> {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Counter for currently active scanning jobs. */
    private final AtomicInteger activeJobs = new AtomicInteger(0);

    /** Flag indicating whether the worker has been initialized. */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /** Flag indicating whether the worker should perform self-cleanup when jobs complete. */
    private final AtomicBoolean shouldCleanupSelf = new AtomicBoolean(false);

    /** Identifier of the bulk scan this worker is associated with. */
    protected final String bulkScanId;

    /** Configuration parameters for scanning operations. */
    protected final T scanConfig;

    /**
     * Thread pool executor for handling scan operations with timeout support.
     *
     * <p>This executor wraps scanner functions in Future objects to enable proper timeout handling
     * and concurrent execution of multiple scans.
     */
    private final ThreadPoolExecutor timeoutExecutor;

    /**
     * Creates a new BulkScanWorker with the specified configuration and thread pool size.
     *
     * @param bulkScanId the identifier of the bulk scan this worker belongs to
     * @param scanConfig the scan configuration containing scan parameters
     * @param parallelScanThreads the number of threads to use for parallel scanning
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
     * Handles a scan request for the specified target.
     *
     * <p>This method manages the complete lifecycle of a scan operation:
     *
     * <ul>
     *   <li>Ensures the worker is initialized before scanning
     *   <li>Submits the scan to the thread pool for execution
     *   <li>Tracks active job count for resource management
     *   <li>Handles cleanup when all jobs are complete
     * </ul>
     *
     * @param scanTarget the target to scan
     * @return a Future representing the scan operation result
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
     * Performs the actual scan operation on the specified target.
     *
     * <p>This method must be implemented by concrete worker classes to provide the specific
     * scanning logic for their scanner type.
     *
     * @param scanTarget the target to scan
     * @return a MongoDB document containing the scan results
     */
    public abstract Document scan(ScanTarget scanTarget);

    /**
     * Initializes the worker if not already initialized.
     *
     * <p>This method ensures thread-safe initialization using double-checked locking. Only one
     * thread will perform the actual initialization, while others will wait for completion.
     *
     * @return true if this call performed the initialization, false if already initialized
     */
    public final boolean init() {
        // synchronize such that no thread runs before being initialized
        // but only synchronize if not already initialized
        if (!initialized.get()) {
            synchronized (initialized) {
                if (!initialized.getAndSet(true)) {
                    initInternal();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cleans up the worker resources if no jobs are currently active.
     *
     * <p>This method performs thread-safe cleanup using synchronization to prevent race conditions
     * with initialization and active jobs. If jobs are still running, cleanup is deferred until all
     * jobs complete.
     *
     * @return true if cleanup was performed, false if deferred or already cleaned up
     */
    public final boolean cleanup() {
        // synchronize such that init and cleanup do not run simultaneously
        // but only synchronize if already initialized
        if (initialized.get()) {
            synchronized (initialized) {
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
     * Performs worker-specific initialization.
     *
     * <p>This method is called once during the worker's lifecycle and should set up any resources
     * needed for scanning operations.
     */
    protected abstract void initInternal();

    /**
     * Performs worker-specific cleanup.
     *
     * <p>This method is called when the worker is being shut down and should release any resources
     * allocated during initialization.
     */
    protected abstract void cleanupInternal();
}
