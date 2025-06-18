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

    /**
     * Creates a scan configuration with the specified parameters.
     *
     * @param scannerDetail the level of detail for the scan
     * @param reexecutions the number of times to re-execute failed scans
     * @param timeout the timeout for each scan in milliseconds
     */
    protected ScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
        this.scannerDetail = scannerDetail;
        this.reexecutions = reexecutions;
        this.timeout = timeout;
    }

    /**
     * Gets the scanner detail level for this configuration.
     *
     * @return the scanner detail level
     */
    public ScannerDetail getScannerDetail() {
        return this.scannerDetail;
    }

    /**
     * Gets the number of times to re-execute failed scans.
     *
     * @return the number of re-executions
     */
    public int getReexecutions() {
        return this.reexecutions;
    }

    /**
     * Gets the timeout for each scan.
     *
     * @return the timeout in milliseconds
     */
    public int getTimeout() {
        return this.timeout;
    }

    /**
     * Sets the scanner detail level for this configuration.
     *
     * @param scannerDetail the scanner detail level to set
     */
    public void setScannerDetail(ScannerDetail scannerDetail) {
        this.scannerDetail = scannerDetail;
    }

    /**
     * Sets the number of times to re-execute failed scans.
     *
     * @param reexecutions the number of re-executions to set
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the timeout for each scan.
     *
     * @param timeout the timeout in milliseconds to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Creates a bulk scan worker for this scan configuration.
     *
     * @param bulkScanID the identifier of the bulk scan
     * @param parallelConnectionThreads the number of parallel connection threads
     * @param parallelScanThreads the number of parallel scan threads
     * @return a new bulk scan worker configured for this scan type
     */
    public abstract BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads);
}
