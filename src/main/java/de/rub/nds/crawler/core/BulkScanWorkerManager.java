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
     * Gets the singleton instance of BulkScanWorkerManager.
     *
     * @return the singleton instance
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
     * Static method to handle a scan job using the singleton instance.
     *
     * @param scanJobDescription the scan job description
     * @param parallelConnectionThreads number of parallel connection threads
     * @param parallelScanThreads number of parallel scan threads
     * @return a Future containing the scan result document
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
     * Gets or creates a BulkScanWorker for the given bulk scan ID.
     *
     * @param bulkScanId the unique identifier for the bulk scan
     * @param scanConfig the scan configuration
     * @param parallelConnectionThreads number of parallel connection threads
     * @param parallelScanThreads number of parallel scan threads
     * @return the BulkScanWorker instance
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
     * Handles a scan job by delegating to the appropriate BulkScanWorker.
     *
     * @param scanJobDescription the scan job description
     * @param parallelConnectionThreads number of parallel connection threads
     * @param parallelScanThreads number of parallel scan threads
     * @return a Future containing the scan result document
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
