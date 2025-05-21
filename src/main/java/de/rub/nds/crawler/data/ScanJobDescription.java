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
import java.util.UUID;

/**
 * Description of a scan job to be processed by a worker. Contains all information needed to perform
 * a scan and to store its results.
 */
public class ScanJobDescription implements Serializable {

    private final UUID id = UUID.randomUUID();

    private final ScanTarget scanTarget;

    // Metadata
    private transient Optional<Long> deliveryTag = Optional.empty();

    private JobStatus status;

    private final BulkScanInfo bulkScanInfo;

    // data to write back results

    private final String dbName;

    private final String collectionName;

    /**
     * Creates a new scan job description with the given parameters.
     *
     * @param scanTarget The target to scan
     * @param bulkScanInfo Information about the bulk scan this job is part of
     * @param dbName The database name where results should be stored
     * @param collectionName The collection name where results should be stored
     * @param status The initial status of the job
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
     * Creates a new scan job description as part of a bulk scan. This is a convenience constructor
     * that extracts the necessary information from the bulk scan.
     *
     * @param scanTarget The target to scan
     * @param bulkScan The bulk scan this job is part of
     * @param status The initial status of the job
     */
    public ScanJobDescription(ScanTarget scanTarget, BulkScan bulkScan, JobStatus status) {
        this(
                scanTarget,
                new BulkScanInfo(bulkScan),
                bulkScan.getName(),
                bulkScan.getCollectionName(),
                status);
    }

    /**
     * Gets the unique identifier for this job.
     *
     * @return The job's UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Custom deserialization to properly handle transient fields.
     *
     * @param in The input stream to read from
     * @throws IOException If an I/O error occurs
     * @throws ClassNotFoundException If the class of a serialized object cannot be found
     */
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // handle deserialization, cf. https://stackoverflow.com/a/3960558
        in.defaultReadObject();
        deliveryTag = Optional.empty();
    }

    /**
     * Gets the target to scan.
     *
     * @return The scan target
     */
    public ScanTarget getScanTarget() {
        return scanTarget;
    }

    /**
     * Gets the database name where results should be stored.
     *
     * @return The database name
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Gets the collection name where results should be stored.
     *
     * @return The collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Gets the current status of the job.
     *
     * @return The job status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the job.
     *
     * @param status The new job status
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Gets the delivery tag assigned by the message broker.
     *
     * @return The delivery tag
     * @throws java.util.NoSuchElementException If no delivery tag has been set
     */
    public long getDeliveryTag() {
        return deliveryTag.get();
    }

    /**
     * Sets the delivery tag assigned by the message broker.
     *
     * @param deliveryTag The delivery tag
     * @throws IllegalStateException If a delivery tag has already been set
     */
    public void setDeliveryTag(Long deliveryTag) {
        if (this.deliveryTag.isPresent()) {
            throw new IllegalStateException("Delivery tag already set");
        }
        this.deliveryTag = Optional.of(deliveryTag);
    }

    /**
     * Gets information about the bulk scan this job is part of.
     *
     * @return The bulk scan information
     */
    public BulkScanInfo getBulkScanInfo() {
        return bulkScanInfo;
    }
}
