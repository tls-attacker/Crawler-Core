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

/**
 * Abstract base class for scan configurations used by the crawler. Provides common configuration
 * parameters for bulk scanning operations.
 */
public abstract class ScanConfig implements Serializable {

    private ScannerDetail scannerDetail;

    private int reexecutions;

    private int timeout;

    @SuppressWarnings("unused")
    private ScanConfig() {}

    /**
     * Creates a new ScanConfig with the specified parameters.
     *
     * @param scannerDetail the level of detail for the scan
     * @param reexecutions the number of times to retry failed scans
     * @param timeout the timeout in milliseconds for each scan
     */
    protected ScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
        this.scannerDetail = scannerDetail;
        this.reexecutions = reexecutions;
        this.timeout = timeout;
    }

    /**
     * Gets the scanner detail level.
     *
     * @return the scanner detail level
     */
    public ScannerDetail getScannerDetail() {
        return this.scannerDetail;
    }

    /**
     * Gets the number of reexecutions for failed scans.
     *
     * @return the number of reexecutions
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
     * Sets the scanner detail level.
     *
     * @param scannerDetail the scanner detail level to set
     */
    public void setScannerDetail(ScannerDetail scannerDetail) {
        this.scannerDetail = scannerDetail;
    }

    /**
     * Sets the number of reexecutions for failed scans.
     *
     * @param reexecutions the number of reexecutions to set
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
     * @param bulkScanID the unique identifier for the bulk scan
     * @param parallelConnectionThreads the number of parallel connection threads
     * @param parallelScanThreads the number of parallel scan threads
     * @return a new bulk scan worker configured with this scan configuration
     */
    public abstract BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads);
}
