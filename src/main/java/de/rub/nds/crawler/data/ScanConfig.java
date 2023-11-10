/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.Serializable;

public abstract class ScanConfig implements Serializable {

    private ScannerDetail scannerDetail;

    private int reexecutions;

    private int timeout;

    @SuppressWarnings("unused")
    private ScanConfig() {}

    protected ScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
        this.scannerDetail = scannerDetail;
        this.reexecutions = reexecutions;
        this.timeout = timeout;
    }

    public ScannerDetail getScannerDetail() {
        return this.scannerDetail;
    }

    public int getReexecutions() {
        return this.reexecutions;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setScannerDetail(ScannerDetail scannerDetail) {
        this.scannerDetail = scannerDetail;
    }

    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public abstract BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads);
}
