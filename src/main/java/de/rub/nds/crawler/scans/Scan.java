/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.scans;

import de.rub.nds.crawler.constant.Status;
import de.rub.nds.crawler.data.ScanJob;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Interface to be implemented by scans. */
public abstract class Scan implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger();

    protected final ScanJob scanJob;

    private final IOrchestrationProvider orchestrationProvider;

    private final IPersistenceProvider persistenceProvider;
    protected final AtomicBoolean cancelled = new AtomicBoolean(false);

    public Scan(
            ScanJob scanJob,
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider) {
        this.scanJob = scanJob;
        this.orchestrationProvider = orchestrationProvider;
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public final void run() {
        try {
            ScanResult result = executeScan();
            if (result != null) {
                persistenceProvider.insertScanResult(
                        result, scanJob.getDbName(), scanJob.getCollectionName());
                scanJob.setStatus(Status.DoneResultWritten);
            } else {
                scanJob.setStatus(Status.DoneNoResult);
            }
        } catch (Exception e) {
            LOGGER.error(
                    "Scanning of {} had to be aborted because of an exception: ",
                    scanJob.getScanTarget(),
                    e);
            // TODO propagate error to DB or somewhere...
        } finally {
            this.cancel(false);
        }
    }

    protected abstract ScanResult executeScan();

    public final void cancel(boolean timeout) {
        if (!cancelled.getAndSet(true)) {
            if (timeout) {
                scanJob.setStatus(Status.Timeout);
            }
            orchestrationProvider.notifyOfDoneScanJob(scanJob);
            onJobDone(timeout);
        }
    }

    protected abstract void onJobDone(boolean timeout);

    public ScanJob getScanJob() {
        return this.scanJob;
    }
}
