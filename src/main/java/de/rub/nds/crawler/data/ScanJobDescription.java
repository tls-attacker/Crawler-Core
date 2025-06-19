/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import de.rub.nds.crawler.constant.JobStatus;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

public class ScanJobDescription implements Serializable {

    private final ScanTarget scanTarget;

    // Metadata
    private transient Optional<Long> deliveryTag = Optional.empty();

    private JobStatus status;

    private final BulkScanInfo bulkScanInfo;

    // data to write back results

    private final String dbName;

    private final String collectionName;

    /**
     * Creates a new scan job description with the specified parameters.
     *
     * @param scanTarget the target to scan
     * @param bulkScanInfo the bulk scan information
     * @param dbName the database name for storing results
     * @param collectionName the collection name for storing results
     * @param status the initial job status
     */
    public ScanJobDescription(
            ScanTarget scanTarget,
            BulkScanInfo bulkScanInfo,
            String dbName,
            String collectionName,
            JobStatus status) {
        this.scanTarget = scanTarget;
        this.bulkScanInfo = bulkScanInfo;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.status = status;
    }

    /**
     * Creates a new scan job description from a bulk scan.
     *
     * @param scanTarget the target to scan
     * @param bulkScan the bulk scan to extract information from
     * @param status the initial job status
     */
    public ScanJobDescription(ScanTarget scanTarget, BulkScan bulkScan, JobStatus status) {
        this(
                scanTarget,
                new BulkScanInfo(bulkScan),
                bulkScan.getName(),
                bulkScan.getCollectionName(),
                status);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // handle deserialization, cf. https://stackoverflow.com/a/3960558
        in.defaultReadObject();
        deliveryTag = Optional.empty();
    }

    /**
     * Gets the scan target.
     *
     * @return the scan target
     */
    public ScanTarget getScanTarget() {
        return scanTarget;
    }

    /**
     * Gets the database name for storing results.
     *
     * @return the database name
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Gets the collection name for storing results.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Gets the current job status.
     *
     * @return the job status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * Sets the job status.
     *
     * @param status the new job status
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Gets the delivery tag for message acknowledgement.
     *
     * @return the delivery tag
     */
    public long getDeliveryTag() {
        return deliveryTag.get();
    }

    /**
     * Sets the delivery tag for message acknowledgement.
     *
     * @param deliveryTag the delivery tag to set
     * @throws IllegalStateException if the delivery tag was already set
     */
    public void setDeliveryTag(Long deliveryTag) {
        if (this.deliveryTag.isPresent()) {
            throw new IllegalStateException("Delivery tag already set");
        }
        this.deliveryTag = Optional.of(deliveryTag);
    }

    /**
     * Gets the bulk scan information.
     *
     * @return the bulk scan information
     */
    public BulkScanInfo getBulkScanInfo() {
        return bulkScanInfo;
    }
}
