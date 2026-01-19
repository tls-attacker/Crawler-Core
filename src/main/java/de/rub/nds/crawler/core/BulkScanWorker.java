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
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.util.CanceallableThreadPoolExecutor;
import de.rub.nds.scanner.core.execution.NamedThreadFactory;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/**
 * A worker that scans all targets for a single scan ID. Instances are managed using the {@link
 * BulkScanWorkerManager}.
 *
 * @param <T> The specific ScanConfig type used by this worker
 */
public abstract class BulkScanWorker<T extends ScanConfig> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shouldCleanupSelf = new AtomicBoolean(false);
    private final Object initializationLock = new Object();

    /** The bulk scan ID for this worker. This is unique across all workers. */
    protected final String bulkScanId;

    /** The scan configuration for this worker */
    protected final T scanConfig;

    /** The persistence provider for writing partial results */
    protected final IPersistenceProvider persistenceProvider;

    /**
     * Calls the inner scan function and may handle cleanup. This is needed to wrap the scanner into
     * a future object such that we can handle timeouts properly.
     */
    private final ThreadPoolExecutor timeoutExecutor;

    /**
     * Creates a new bulk scan worker. This should only be called by the {@link
     * BulkScanWorkerManager}.
     *
     * @param bulkScanId The ID of the bulk scan this worker is associated with
     * @param scanConfig The scan configuration for this worker
     * @param parallelScanThreads The number of parallel scan threads to use, i.e., how many {@link
     *     ScanTarget}s to handle in parallel.
     * @param persistenceProvider The persistence provider for writing partial results
     */
    protected BulkScanWorker(
            String bulkScanId,
            T scanConfig,
            int parallelScanThreads,
            IPersistenceProvider persistenceProvider) {
        this.bulkScanId = bulkScanId;
        this.scanConfig = scanConfig;
        this.persistenceProvider = persistenceProvider;

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
     * Handles a scan target by submitting it to the executor. If init was not called, it will
     * initialize itself. In this case it will also clean up itself if all jobs are done.
     *
     * <p>Returns a {@link ProgressableFuture} that represents the entire scan lifecycle, allowing
     * callers to:
     *
     * <ul>
     *   <li>Get partial results as the scan progresses via {@link
     *       ProgressableFuture#getCurrentResult()}
     *   <li>Wait for the final result via {@link ProgressableFuture#get()}
     * </ul>
     *
     * @param jobDescription The job description for this scan.
     * @return A ProgressableFuture representing the scan lifecycle
     */
    public ProgressableFuture<Document> handle(ScanJobDescription jobDescription) {
        // if we initialized ourself, we also clean up ourself
        shouldCleanupSelf.weakCompareAndSetAcquire(false, init());
        activeJobs.incrementAndGet();

        ProgressableFuture<Document> progressableFuture = new ProgressableFuture<>();

        // Compose a consumer that both updates the future and persists partial results
        Consumer<Document> progressConsumer =
                partialResult -> {
                    progressableFuture.updateResult(partialResult);
                    try {
                        persistPartialResult(jobDescription, partialResult);
                    } catch (Exception e) {
                        LOGGER.warn("Failed to persist partial result, continuing scan", e);
                    }
                };

        timeoutExecutor.submit(
                () -> {
                    try {
                        Document result = scan(jobDescription, progressConsumer);
                        progressableFuture.complete(result);
                    } catch (Exception e) {
                        progressableFuture.completeExceptionally(e);
                    } finally {
                        if (activeJobs.decrementAndGet() == 0 && shouldCleanupSelf.get()) {
                            cleanup();
                        }
                    }
                });

        return progressableFuture;
    }

    /**
     * Scans a target and returns the result as a Document. This is the core scanning functionality
     * that must be implemented by subclasses.
     *
     * @param jobDescription The job description containing target and metadata
     * @param progressConsumer Consumer to call with partial results during scanning
     * @return The scan result as a Document
     */
    public abstract Document scan(
            ScanJobDescription jobDescription, Consumer<Document> progressConsumer);

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
     * thread-safe and will only clean up once. If there are still active jobs, it will enqueue the
     * cleanup for later.
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

    /**
     * Persists a partial scan result. This method can be called by subclasses during scanning to
     * save intermediate results.
     *
     * @param jobDescription The job description for the scan
     * @param partialResult The partial result document to persist
     */
    protected void persistPartialResult(ScanJobDescription jobDescription, Document partialResult) {
        persistenceProvider.upsertPartialResult(jobDescription, partialResult);
    }
}
