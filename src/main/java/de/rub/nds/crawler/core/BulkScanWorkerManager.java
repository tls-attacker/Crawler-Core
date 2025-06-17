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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.exception.UncheckedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/**
 * Singleton manager for bulk scan workers that handles worker lifecycle and caching.
 *
 * <p>This class implements a caching mechanism for {@link BulkScanWorker} instances to optimize
 * resource usage in distributed scanning operations. Workers are cached by bulk scan ID and
 * automatically cleaned up after periods of inactivity.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Worker Lifecycle Management</strong> - Creates, caches, and cleans up worker
 *       instances
 *   <li><strong>Resource Optimization</strong> - Reuses workers for the same bulk scan to avoid
 *       initialization overhead
 *   <li><strong>Memory Management</strong> - Automatically expires unused workers to prevent memory
 *       leaks
 *   <li><strong>Concurrent Access</strong> - Thread-safe worker creation and caching
 * </ul>
 *
 * <p><strong>Caching Strategy:</strong>
 *
 * <ul>
 *   <li>Workers are cached by bulk scan ID for efficient reuse
 *   <li>30-minute expiration after last access to free resources
 *   <li>Automatic cleanup when workers are evicted from cache
 *   <li>Lazy initialization - workers created only when needed
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can handle concurrent worker
 * requests from multiple threads. The underlying Guava cache provides the necessary synchronization
 * guarantees.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>
 * // Static convenience method
 * Future&lt;Document&gt; result = BulkScanWorkerManager.handleStatic(
 *     scanJobDescription, 4, 8);
 *
 * // Instance usage
 * BulkScanWorkerManager manager = BulkScanWorkerManager.getInstance();
 * Future&lt;Document&gt; result = manager.handle(scanJobDescription, 4, 8);
 * </pre>
 *
 * @see BulkScanWorker
 * @see ScanJobDescription
 * @see ScanConfig
 */
public class BulkScanWorkerManager {
    private static final Logger LOGGER = LogManager.getLogger();

    /** Singleton instance of the worker manager. */
    private static BulkScanWorkerManager instance;

    /**
     * Gets the singleton instance of the BulkScanWorkerManager.
     *
     * <p>This method implements lazy initialization of the singleton instance. The instance is
     * created on first access and reused for subsequent calls.
     *
     * @return the singleton BulkScanWorkerManager instance
     */
    public static BulkScanWorkerManager getInstance() {
        if (instance == null) {
            instance = new BulkScanWorkerManager();
        }
        return instance;
    }

    /**
     * Static convenience method for handling scan jobs without explicit instance management.
     *
     * <p>This method provides a simplified interface for processing scan jobs by automatically
     * obtaining the singleton instance and delegating to the instance method.
     *
     * @param scanJobDescription the scan job to execute
     * @param parallelConnectionThreads the number of threads for connection management
     * @param parallelScanThreads the number of threads for parallel scanning
     * @return a Future representing the scan operation result
     * @see #handle(ScanJobDescription, int, int)
     */
    public static Future<Document> handleStatic(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        BulkScanWorkerManager instance = getInstance();
        return instance.handle(scanJobDescription, parallelConnectionThreads, parallelScanThreads);
    }

    /** Cache of bulk scan workers indexed by bulk scan ID. */
    private final Cache<String, BulkScanWorker<?>> bulkScanWorkers;

    /**
     * Private constructor for singleton pattern.
     *
     * <p>Initializes the worker cache with the following configuration:
     *
     * <ul>
     *   <li>30-minute expiration after last access
     *   <li>Automatic cleanup of workers when evicted
     *   <li>Thread-safe concurrent access
     * </ul>
     */
    private BulkScanWorkerManager() {
        bulkScanWorkers =
                CacheBuilder.newBuilder()
                        .expireAfterAccess(30, TimeUnit.MINUTES)
                        .removalListener(
                                (RemovalListener<String, BulkScanWorker<?>>)
                                        notification -> {
                                            BulkScanWorker<?> worker = notification.getValue();
                                            if (worker != null) {
                                                worker.cleanup();
                                            }
                                        })
                        .build();
    }

    /**
     * Gets or creates a bulk scan worker for the specified bulk scan.
     *
     * <p>This method implements the core caching logic for worker management:
     *
     * <ul>
     *   <li>If a worker exists in cache for the bulk scan ID, returns it immediately
     *   <li>If no worker exists, creates a new worker using the scan configuration
     *   <li>Newly created workers are automatically initialized before caching
     *   <li>Workers are cached by bulk scan ID for reuse in subsequent requests
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method is thread-safe and can be called concurrently.
     * The cache handles synchronization of worker creation.
     *
     * @param bulkScanId the unique identifier of the bulk scan
     * @param scanConfig the scan configuration for creating new workers
     * @param parallelConnectionThreads the number of threads for connection management
     * @param parallelScanThreads the number of threads for parallel scanning
     * @return the cached or newly created bulk scan worker
     * @throws UncheckedException if worker creation fails
     */
    public BulkScanWorker<?> getBulkScanWorker(
            String bulkScanId,
            ScanConfig scanConfig,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        try {
            return bulkScanWorkers.get(
                    bulkScanId,
                    () -> {
                        BulkScanWorker<?> ret =
                                scanConfig.createWorker(
                                        bulkScanId, parallelConnectionThreads, parallelScanThreads);
                        ret.init();
                        return ret;
                    });
        } catch (ExecutionException e) {
            LOGGER.error("Could not create bulk scan worker", e);
            throw new UncheckedException(e);
        }
    }

    /**
     * Handles a scan job by obtaining the appropriate worker and executing the scan.
     *
     * <p>This method orchestrates the complete scan job execution:
     *
     * <ol>
     *   <li>Extracts bulk scan information from the job description
     *   <li>Obtains or creates the appropriate worker for the bulk scan
     *   <li>Delegates the actual scanning to the worker
     * </ol>
     *
     * <p>The method leverages worker caching to ensure efficient resource utilization across
     * multiple scan jobs belonging to the same bulk scan operation.
     *
     * @param scanJobDescription the scan job containing target and configuration information
     * @param parallelConnectionThreads the number of threads for connection management
     * @param parallelScanThreads the number of threads for parallel scanning
     * @return a Future representing the scan operation result as a MongoDB document
     * @throws UncheckedException if worker creation or initialization fails
     * @see ScanJobDescription
     * @see BulkScanWorker#handle(de.rub.nds.crawler.data.ScanTarget)
     */
    public Future<Document> handle(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        BulkScanInfo bulkScanInfo = scanJobDescription.getBulkScanInfo();
        BulkScanWorker<?> worker =
                getBulkScanWorker(
                        bulkScanInfo.getBulkScanId(),
                        bulkScanInfo.getScanConfig(),
                        parallelConnectionThreads,
                        parallelScanThreads);
        return worker.handle(scanJobDescription.getScanTarget());
    }
}
