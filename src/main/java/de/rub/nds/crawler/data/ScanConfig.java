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

    /**
     * Gets the scanner detail configuration.
     *
     * @return the scanner detail
     */
    public ScannerDetail getScannerDetail() {
        return this.scannerDetail;
    }

    /**
     * Gets the number of re-executions configured.
     *
     * @return the number of re-executions
     */
    public int getReexecutions() {
        return this.reexecutions;
    }

    /**
     * Gets the timeout value in seconds.
     *
     * @return the timeout value
     */
    public int getTimeout() {
        return this.timeout;
    }

    /**
     * Sets the scanner detail configuration.
     *
     * @param scannerDetail the scanner detail to set
     */
    public void setScannerDetail(ScannerDetail scannerDetail) {
        this.scannerDetail = scannerDetail;
    }

    /**
     * Sets the number of re-executions.
     *
     * @param reexecutions the number of re-executions to set
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the timeout value in seconds.
     *
     * @param timeout the timeout value to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Creates a worker for bulk scanning with the specified configuration.
     *
     * @param bulkScanID the bulk scan ID
     * @param parallelConnectionThreads the number of parallel connection threads
     * @param parallelScanThreads the number of parallel scan threads
     * @return a new bulk scan worker
     */
    public abstract BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads);
}
