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
import de.rub.nds.scanner.core.probe.ProbeType;
import java.io.Serializable;
import java.util.List;

public abstract class ScanConfig implements Serializable {

    private ScannerDetail scannerDetail;

    private int reexecutions;

    private int timeout;

    private List<ProbeType> excludedProbes;

    @SuppressWarnings("unused")
    private ScanConfig() {}

    protected ScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
        this(scannerDetail, reexecutions, timeout, null);
    }

    protected ScanConfig(
            ScannerDetail scannerDetail,
            int reexecutions,
            int timeout,
            List<ProbeType> excludedProbes) {
        this.scannerDetail = scannerDetail;
        this.reexecutions = reexecutions;
        this.timeout = timeout;
        this.excludedProbes = excludedProbes;
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

    public List<ProbeType> getExcludedProbes() {
        return this.excludedProbes;
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

    public void setExcludedProbes(List<ProbeType> excludedProbes) {
        this.excludedProbes = excludedProbes;
    }

    public abstract BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads);
}
