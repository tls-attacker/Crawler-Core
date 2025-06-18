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
     * Creates a scan job description with the specified parameters.
     *
     * @param scanTarget the target to scan
     * @param bulkScanInfo metadata about the bulk scan
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
     * Creates a scan job description from a scan target and bulk scan.
     *
     * @param scanTarget the target to scan
     * @param bulkScan the bulk scan this job belongs to
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
     * Gets the scan target for this job.
     *
     * @return the scan target
     */
    public ScanTarget getScanTarget() {
        return scanTarget;
    }

    /**
     * Gets the database name where results should be stored.
     *
     * @return the database name
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Gets the collection name where results should be stored.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Gets the current status of this scan job.
     *
     * @return the job status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of this scan job.
     *
     * @param status the new job status
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Gets the delivery tag for message queue acknowledgment.
     *
     * @return the delivery tag
     * @throws NoSuchElementException if the delivery tag has not been set
     */
    public long getDeliveryTag() {
        return deliveryTag.get();
    }

    /**
     * Sets the delivery tag for message queue acknowledgment.
     *
     * @param deliveryTag the delivery tag to set
     * @throws IllegalStateException if the delivery tag has already been set
     */
    public void setDeliveryTag(Long deliveryTag) {
        if (this.deliveryTag.isPresent()) {
            throw new IllegalStateException("Delivery tag already set");
        }
        this.deliveryTag = Optional.of(deliveryTag);
    }

    /**
     * Gets the bulk scan information for this job.
     *
     * @return the bulk scan info
     */
    public BulkScanInfo getBulkScanInfo() {
        return bulkScanInfo;
    }
}
