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
 * Abstract base class for scan configurations. Contains common configuration options for all
 * scanner types and defines required factory methods to create workers.
 */
public abstract class ScanConfig implements Serializable {

    private ScannerDetail scannerDetail;

    private int reexecutions;

    private int timeout;

    @SuppressWarnings("unused")
    private ScanConfig() {}

    /**
     * Creates a new scan configuration with the specified parameters.
     *
     * @param scannerDetail The level of detail for the scan
     * @param reexecutions The number of times to retry failed scans
     * @param timeout The timeout for each scan in seconds
     */
    protected ScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
        this.scannerDetail = scannerDetail;
        this.reexecutions = reexecutions;
        this.timeout = timeout;
    }

    /**
     * Gets the scanner detail level.
     *
     * @return The scanner detail level
     */
    public ScannerDetail getScannerDetail() {
        return this.scannerDetail;
    }

    /**
     * Gets the number of reexecutions for failed scans.
     *
     * @return The number of reexecutions
     */
    public int getReexecutions() {
        return this.reexecutions;
    }

    /**
     * Gets the timeout for each scan in seconds.
     *
     * @return The timeout in seconds
     */
    public int getTimeout() {
        return this.timeout;
    }

    /**
     * Sets the scanner detail level.
     *
     * @param scannerDetail The scanner detail level
     */
    public void setScannerDetail(ScannerDetail scannerDetail) {
        this.scannerDetail = scannerDetail;
    }

    /**
     * Sets the number of reexecutions for failed scans.
     *
     * @param reexecutions The number of reexecutions
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the timeout for each scan in seconds.
     *
     * @param timeout The timeout in seconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Creates a worker for this scan configuration. Each implementation must provide a factory
     * method to create the appropriate worker type.
     *
     * @param bulkScanID The ID of the bulk scan this worker is for
     * @param parallelConnectionThreads The number of parallel connection threads to use
     * @param parallelScanThreads The number of parallel scan threads to use
     * @return A worker for this scan configuration
     */
    public abstract BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads);
}
