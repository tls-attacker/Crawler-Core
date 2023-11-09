/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.persistence;

import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJob;
import de.rub.nds.crawler.data.ScanResult;

/**
 * Persistence provider interface. Exposes methods to write out the different stages of a task to a
 * file/database/api.
 */
public interface IPersistenceProvider {

    /**
     * Insert a scan result into the database.
     *
     * @param scanResult The scan result to insert.
     * @param job The job that was used to create the scan result.
     */
    void insertScanResult(ScanResult scanResult, ScanJob job);

    /**
     * Insert a bulk scan into the database. This is used to store metadata about the bulk scan.
     * This adds an ID to the bulk scan.
     *
     * @param bulkScan The bulk scan to insert.
     */
    void insertBulkScan(BulkScan bulkScan);

    /**
     * Update a bulk scan in the database. This updated the whole bulk scan.
     *
     * @param bulkScan The bulk scan to update.
     */
    void updateBulkScan(BulkScan bulkScan);
}
