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
    protected final String bulkScanId;
    protected final T scanConfig;

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
                        new NamedThreadFactory("crawler: scan executor"));
    }

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

    public abstract Document scan(ScanTarget scanTarget);

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

    protected abstract void initInternal();

    protected abstract void cleanupInternal();
}
