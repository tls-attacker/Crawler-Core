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
     * Creates a new BulkScanInfo from the given bulk scan.
     *
     * @param bulkScan the bulk scan to extract information from
     */
    public BulkScanInfo(BulkScan bulkScan) {
        this.bulkScanId = bulkScan.get_id();
        this.scanConfig = bulkScan.getScanConfig();
        this.isMonitored = bulkScan.isMonitored();
    }

    /**
     * Gets the bulk scan ID.
     *
     * @return the bulk scan ID
     */
    public String getBulkScanId() {
        return bulkScanId;
    }

    /**
     * Gets the scan configuration.
     *
     * @return the scan configuration
     */
    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    /**
     * Gets the scan configuration cast to the specified type.
     *
     * @param clazz the class to cast the scan configuration to
     * @param <T> the type to cast to, must extend ScanConfig
     * @return the scan configuration cast to the specified type
     */
    public <T extends ScanConfig> T getScanConfig(Class<T> clazz) {
        return clazz.cast(scanConfig);
    }

    /**
     * Checks if the bulk scan is monitored.
     *
     * @return true if monitored, false otherwise
     */
    public boolean isMonitored() {
        return isMonitored;
    }
}
