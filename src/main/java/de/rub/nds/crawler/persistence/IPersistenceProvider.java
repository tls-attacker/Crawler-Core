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
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import java.util.List;

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
    void insertScanResult(ScanResult scanResult, ScanJobDescription job);

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

    /**
     * Retrieve scan results for a specific target hostname or IP.
     *
     * @param dbName The database name where the scan results are stored.
     * @param collectionName The collection name where the scan results are stored.
     * @param target The hostname or IP address to search for.
     * @return A list of scan results matching the target.
     */
    List<ScanResult> getScanResultsByTarget(String dbName, String collectionName, String target);

    /**
     * Retrieve a specific scan result by its ID.
     *
     * @param dbName The database name where the scan result is stored.
     * @param collectionName The collection name where the scan result is stored.
     * @param id The ID of the scan result to retrieve.
     * @return The scan result, or null if not found.
     */
    ScanResult getScanResultById(String dbName, String collectionName, String id);

    /**
     * Retrieve the most recent scan result by its scan job description ID. If multiple results
     * exist for the same scan job description ID, returns the one with the latest timestamp.
     *
     * @param dbName The database name where the scan result is stored.
     * @param collectionName The collection name where the scan result is stored.
     * @param scanJobDescriptionId The scan job description ID to search for.
     * @return The most recent scan result, or null if not found.
     */
    ScanResult getScanResultByScanJobDescriptionId(
            String dbName, String collectionName, String scanJobDescriptionId);
}
