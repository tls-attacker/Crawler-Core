/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import java.io.Serializable;

/**
 * Metadata about a bulk scan which is serialized to the workers. This is expected to stay the same
 * for the duration of a bulk scan.
 */
public class BulkScanInfo implements Serializable {
    private final String bulkScanId;

    private final ScanConfig scanConfig;

    private final boolean isMonitored;

    /**
     * Creates a new BulkScanInfo from a BulkScan instance.
     *
     * @param bulkScan the bulk scan to extract information from
     */
    public BulkScanInfo(BulkScan bulkScan) {
        this.bulkScanId = bulkScan.get_id();
        this.scanConfig = bulkScan.getScanConfig();
        this.isMonitored = bulkScan.isMonitored();
    }

    /**
     * Gets the unique identifier for this bulk scan.
     *
     * @return the bulk scan ID
     */
    public String getBulkScanId() {
        return bulkScanId;
    }

    /**
     * Gets the scan configuration for this bulk scan.
     *
     * @return the scan configuration
     */
    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    /**
     * Gets the scan configuration cast to a specific type.
     *
     * @param <T> the type of scan configuration
     * @param clazz the class to cast the configuration to
     * @return the scan configuration cast to the specified type
     * @throws ClassCastException if the configuration cannot be cast to the specified type
     */
    public <T extends ScanConfig> T getScanConfig(Class<T> clazz) {
        return clazz.cast(scanConfig);
    }

    /**
     * Checks if this bulk scan is being monitored.
     *
     * @return true if the bulk scan is monitored, false otherwise
     */
    public boolean isMonitored() {
        return isMonitored;
    }
}
