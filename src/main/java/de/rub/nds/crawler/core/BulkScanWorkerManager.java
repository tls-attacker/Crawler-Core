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
 * Manager class for bulk scan workers that ensures worker instances are reused and properly
 * managed. Acts as a singleton factory and manager for BulkScanWorker instances.
 */
public class BulkScanWorkerManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile BulkScanWorkerManager instance;

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
                    instance = new BulkScanWorkerManager();
                }
            }
        }
        return instance;
    }

    /**
     * Static convenience method to handle a scan job. Creates or retrieves the appropriate worker
     * and submits the scan target for processing.
     *
     * @param scanJobDescription The scan job to handle
     * @param parallelConnectionThreads The number of parallel connection threads to use
     * @param parallelScanThreads The number of parallel scan threads to use
     * @return A future that will complete when the scan is done
     */
    public static Future<Document> handleStatic(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        BulkScanWorkerManager manager = getInstance();
        return manager.handle(scanJobDescription, parallelConnectionThreads, parallelScanThreads);
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
     * Gets or creates a bulk scan worker for the specified bulk scan. Workers are cached and reused
     * to avoid excessive resource allocation.
     *
     * @param bulkScanId The ID of the bulk scan
     * @param scanConfig The scan configuration to use
     * @param parallelConnectionThreads The number of parallel connection threads to use
     * @param parallelScanThreads The number of parallel scan threads to use
     * @return A bulk scan worker for the specified bulk scan
     * @throws UncheckedException If a worker cannot be created
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
     * Handles a scan job by creating or retrieving the appropriate worker and submitting the scan
     * target for processing.
     *
     * @param scanJobDescription The scan job to handle
     * @param parallelConnectionThreads The number of parallel connection threads to use
     * @param parallelScanThreads The number of parallel scan threads to use
     * @return A future that will complete when the scan is done
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
