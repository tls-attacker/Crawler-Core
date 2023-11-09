/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.scans;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.ScanJob;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

/** Interface to be implemented by scans. */
public abstract class Scan implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger();

    protected final ScanJob scanJob;

    private final IOrchestrationProvider orchestrationProvider;

    private final IPersistenceProvider persistenceProvider;
    protected final AtomicBoolean cancelled = new AtomicBoolean(false);
    protected final AtomicBoolean cleantUp = new AtomicBoolean(false);

    protected Scan(
            ScanJob scanJob,
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider) {
        this.scanJob = scanJob;
        this.orchestrationProvider = orchestrationProvider;
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public final void run() {
        ScanResult scanResult = null;
        try {
            Document resultDocument = executeScan();

            JobStatus jobStatus;
            if (resultDocument == null) {
                jobStatus = JobStatus.EMPTY;
            } else {
                jobStatus = cancelled.get() ? JobStatus.CANCELLED : JobStatus.SUCCESS;
            }
            scanJob.setStatus(jobStatus);
            scanResult = new ScanResult(scanJob, resultDocument);
        } catch (Exception e) {
            LOGGER.error(
                    "Scanning of {} had to be aborted because of an exception: ",
                    scanJob.getScanTarget(),
                    e);

            scanResult = ScanResult.fromException(scanJob, e);
        } finally {
            try {
                persistResult(scanResult);
            } catch (Exception e) {
                LOGGER.error("Could not persist result for {}", scanJob.getScanTarget(), e);
                scanJob.setStatus(JobStatus.ERROR);
            }
            this.cleanup();
        }
    }

    private void persistResult(ScanResult scanResult) {
        if (scanResult != null) {
            LOGGER.info(
                    "Writing {} result for {}",
                    scanResult.getResultStatus(),
                    scanJob.getScanTarget());
            scanJob.setStatus(scanResult.getResultStatus());
            persistenceProvider.insertScanResult(scanResult, scanJob);
        } else {
            LOGGER.error("ScanResult was null, this should not happen.");
            scanJob.setStatus(JobStatus.INTERNAL_ERROR);
        }
    }

    protected abstract Document executeScan();

    /** Cancels the scan. This is idempotent. */
    public final void cancel() {
        if (!cancelled.getAndSet(true)) {
            scanJob.setStatus(JobStatus.CANCELLED);
            cleanup();
        }
    }

    /**
     * Cleans up the scan. Calls {@link #onCleanup(boolean)} if not already called. This is
     * idempotent.
     */
    public final void cleanup() {
        if (!cleantUp.getAndSet(true)) {
            orchestrationProvider.notifyOfDoneScanJob(scanJob);
            onCleanup(cancelled.get());
        }
    }

    /**
     * Hook to free further resources. Do not call directly, use {@link #cleanup()} instead. If the
     * scan is still running (i.e. cancelled is true), this has to cause the scan to exit.
     *
     * @param cancelled Whether the scan was cancelled or finished normally.
     */
    protected abstract void onCleanup(boolean cancelled);

    public ScanJob getScanJob() {
        return this.scanJob;
    }
}
