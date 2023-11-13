/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.data.BulkScanInfo;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import org.bson.Document;

public class BulkScanWorkerManager {
    /**
     * Timeout for a worker to be considered old and be cleaned up. Set to 5 Minutes. Cleanup is
     * only done upon issuing a new job.
     */
    private static final long WORKER_MAX_AGE_MS = 1000 * 60 * 5L;
    /**
     * Time between each cleanup run. This is effectively added to the {@link #WORKER_MAX_AGE_MS}.
     */
    private static final long CLEANUP_COOLDOWN = 1000 * 60L;

    private static BulkScanWorkerManager instance;

    public static BulkScanWorkerManager getInstance() {
        if (instance == null) {
            instance = new BulkScanWorkerManager();
        }
        return instance;
    }

    public static Future<Document> handleStatic(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        BulkScanWorkerManager instance = getInstance();
        return instance.handle(scanJobDescription, parallelConnectionThreads, parallelScanThreads);
    }

    private final Map<String, BulkScanWorker<?>> bulkScanWorkers = new HashMap<>();

    private long lastCleanup = 0;

    private BulkScanWorkerManager() {}

    public BulkScanWorker<?> getBulkScanWorker(
            String bulkScanId,
            ScanConfig scanConfig,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        return bulkScanWorkers.computeIfAbsent(
                bulkScanId,
                id -> {
                    BulkScanWorker<?> ret =
                            scanConfig.createWorker(
                                    bulkScanId, parallelConnectionThreads, parallelScanThreads);
                    ret.init();
                    return ret;
                });
    }

    private void cleanupOldWorkers(String ignoredBulkScanId) {
        // only perform cleanup every once in a while
        if (System.currentTimeMillis() - lastCleanup < CLEANUP_COOLDOWN) {
            return;
        }
        // only one thread should do the cleanup
        synchronized (this) {
            if (System.currentTimeMillis() - lastCleanup < CLEANUP_COOLDOWN) {
                return;
            }
            lastCleanup = System.currentTimeMillis();
        }

        Iterator<Map.Entry<String, BulkScanWorker<?>>> iter = bulkScanWorkers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, BulkScanWorker<?>> entry = iter.next();
            if (entry.getKey().equals(ignoredBulkScanId)) {
                continue;
            }
            if (entry.getValue().getMillisSinceLastJobSubmitted() > WORKER_MAX_AGE_MS) {
                iter.remove();
                entry.getValue().cleanup();
            }
        }
    }

    public Future<Document> handle(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads) {
        BulkScanInfo bulkScanInfo = scanJobDescription.getBulkScanInfo();
        cleanupOldWorkers(bulkScanInfo.getBulkScanId());
        BulkScanWorker<?> worker =
                getBulkScanWorker(
                        bulkScanInfo.getBulkScanId(),
                        bulkScanInfo.getScanConfig(),
                        parallelConnectionThreads,
                        parallelScanThreads);
        return worker.handle(scanJobDescription.getScanTarget());
    }
}
