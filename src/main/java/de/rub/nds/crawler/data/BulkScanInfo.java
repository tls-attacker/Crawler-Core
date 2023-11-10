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

    public BulkScanInfo(BulkScan bulkScan) {
        this.bulkScanId = bulkScan.get_id();
        this.scanConfig = bulkScan.getScanConfig();
        this.isMonitored = bulkScan.isMonitored();
    }

    public String getBulkScanId() {
        return bulkScanId;
    }

    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    public <T extends ScanConfig> T getScanConfig(Class<T> clazz) {
        return clazz.cast(scanConfig);
    }

    public boolean isMonitored() {
        return isMonitored;
    }
}
