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

public class BulkScanWorkerManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile BulkScanWorkerManager instance;

    /**
     * Returns the singleton instance of BulkScanWorkerManager, creating it if it doesn't exist yet.
     * This method is thread-safe and uses double-checked locking to ensure exactly one instance is
     * created.
     *
     * @return The singleton BulkScanWorkerManager instance
     */
    public static BulkScanWorkerManager getInstance() {
        if (instance == null) {
            synchronized (BulkScanWorkerManager.class) {
                if (instance == null) {
                    instance = new BulkScanWorkerManager();
                }
            }
        }
        return instance;
    }

    /**
     * Static convenience method to handle a scan job using the singleton instance. This method
     * retrieves the appropriate bulk scan worker for the job and delegates the scan to it.
     *
     * @param scanJobDescription The scan job containing bulk scan info and target details
     * @param parallelConnectionThreads Number of parallel threads for connections
     * @param parallelScanThreads Number of parallel threads for scanning
     * @return A Future containing the scan result as a Document
     */
    public static Future<Document> handleStatic(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        BulkScanWorkerManager instance = getInstance();
        return instance.handle(scanJobDescription, parallelConnectionThreads, parallelScanThreads);
    }

    private final Cache<String, BulkScanWorker<?>> bulkScanWorkers;

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
     * Retrieves or creates a BulkScanWorker for the specified bulk scan. Workers are cached and
     * reused for the same bulkScanId to improve performance. Cached workers expire after 30 minutes
     * of inactivity and are automatically cleaned up.
     *
     * @param bulkScanId Unique identifier for the bulk scan
     * @param scanConfig Configuration for the scan type
     * @param parallelConnectionThreads Number of parallel threads for connections
     * @param parallelScanThreads Number of parallel threads for scanning
     * @return The BulkScanWorker instance for this bulk scan
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
     * Handles a scan job by retrieving the appropriate worker and delegating the scan to it. This
     * method extracts the bulk scan information from the job description and uses it to get or
     * create the correct worker.
     *
     * @param scanJobDescription The scan job containing bulk scan info and target details
     * @param parallelConnectionThreads Number of parallel threads for connections
     * @param parallelScanThreads Number of parallel threads for scanning
     * @return A Future containing the scan result as a Document
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
