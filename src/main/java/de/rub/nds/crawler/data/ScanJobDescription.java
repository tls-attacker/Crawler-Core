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

/**
 * Data transfer object representing a single TLS scan job in the distributed scanning architecture.
 *
 * <p>The ScanJobDescription serves as the primary communication unit between the controller and
 * worker nodes in the TLS-Crawler system. It encapsulates all information necessary for a worker to
 * execute a TLS scan and store the results, including the scan target, execution status, database
 * storage location, and message queue metadata.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Job Definition</strong> - Specifies what should be scanned (target host/port)
 *   <li><strong>Status Tracking</strong> - Maintains current execution status throughout lifecycle
 *   <li><strong>Storage Configuration</strong> - Defines where results should be persisted
 *   <li><strong>Message Queue Integration</strong> - Handles RabbitMQ delivery tags for
 *       acknowledgment
 *   <li><strong>Bulk Scan Coordination</strong> - Links individual jobs to their parent bulk scan
 * </ul>
 *
 * <p><strong>Lifecycle Management:</strong>
 *
 * <ul>
 *   <li><strong>Creation</strong> - Controller creates jobs with TO_BE_EXECUTED status
 *   <li><strong>Distribution</strong> - Jobs are serialized and sent via message queue
 *   <li><strong>Processing</strong> - Workers receive, execute, and update status
 *   <li><strong>Completion</strong> - Final status and results are persisted
 * </ul>
 *
 * <p><strong>Message Queue Integration:</strong>
 *
 * <ul>
 *   <li><strong>Delivery Tag</strong> - RabbitMQ message identifier for acknowledgment
 *   <li><strong>Transient Field</strong> - Delivery tag is not serialized (transport-specific)
 *   <li><strong>Single Assignment</strong> - Delivery tag can only be set once per job
 *   <li><strong>Deserialization Handling</strong> - Custom readObject() ensures proper
 *       initialization
 * </ul>
 *
 * <p><strong>Database Storage:</strong>
 *
 * <ul>
 *   <li><strong>Database Name</strong> - Target database for result storage
 *   <li><strong>Collection Name</strong> - Specific collection/table for this scan type
 *   <li><strong>Bulk Scan Traceability</strong> - Links results back to originating bulk scan
 * </ul>
 *
 * <p><strong>Immutability:</strong> Most fields are final to ensure job definitions remain
 * consistent throughout processing, with only the status field being mutable to track execution
 * progress.
 *
 * <p><strong>Serialization:</strong> The class supports Java serialization for message queue
 * transport while handling the transient delivery tag appropriately during deserialization.
 *
 * @see ScanTarget
 * @see BulkScanInfo
 * @see BulkScan
 * @see JobStatus
 */
public class ScanJobDescription implements Serializable {

    /** Target specification containing hostname, IP address, and port information. */
    private final ScanTarget scanTarget;

    // Metadata
    private transient Optional<Long> deliveryTag = Optional.empty();

    /** Current execution status of this scan job (pending, success, error, etc.). */
    private JobStatus status;

    /** Metadata about the parent bulk scan operation this job belongs to. */
    private final BulkScanInfo bulkScanInfo;

    // data to write back results

    /** Database name where scan results should be stored. */
    private final String dbName;

    /** Collection name within the database for result storage. */
    private final String collectionName;

    /**
     * Creates a new scan job description with explicit database storage configuration.
     *
     * <p>This constructor allows precise control over where scan results will be stored by
     * specifying the database name and collection name directly. It's primarily used for advanced
     * scenarios where custom storage locations are needed.
     *
     * @param scanTarget the target host and port to scan
     * @param bulkScanInfo metadata about the parent bulk scan operation
     * @param dbName the database name where results should be stored
     * @param collectionName the collection/table name for result storage
     * @param status the initial job status (typically TO_BE_EXECUTED)
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
     * Creates a new scan job description from a bulk scan configuration.
     *
     * <p>This convenience constructor extracts storage configuration from the bulk scan object,
     * using the bulk scan name as the database name and the bulk scan's collection name for result
     * storage. This is the most common way to create scan jobs.
     *
     * @param scanTarget the target host and port to scan
     * @param bulkScan the parent bulk scan containing storage and configuration details
     * @param status the initial job status (typically TO_BE_EXECUTED)
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
     * Custom deserialization method to properly initialize transient fields.
     *
     * <p>This method ensures that the transient deliveryTag field is properly initialized to an
     * empty Optional after deserialization. The delivery tag is transport-specific and should not
     * be serialized across message boundaries.
     *
     * @param in the object input stream for deserialization
     * @throws IOException if an I/O error occurs during deserialization
     * @throws ClassNotFoundException if a class cannot be found during deserialization
     */
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // handle deserialization, cf. https://stackoverflow.com/a/3960558
        in.defaultReadObject();
        deliveryTag = Optional.empty();
    }

    /**
     * Gets the scan target containing the host and port to be scanned.
     *
     * @return the scan target specifying what should be scanned
     */
    public ScanTarget getScanTarget() {
        return scanTarget;
    }

    /**
     * Gets the database name where scan results should be stored.
     *
     * @return the target database name for result persistence
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Gets the collection/table name where scan results should be stored.
     *
     * @return the target collection name for result persistence
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * Gets the current execution status of this scan job.
     *
     * <p>The status tracks the job's progress through its lifecycle from initial creation
     * (TO_BE_EXECUTED) through completion (SUCCESS, ERROR, etc.).
     *
     * @return the current job execution status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * Updates the execution status of this scan job.
     *
     * <p>This method is used to track the job's progress as it moves through the execution
     * pipeline, from queued to running to completed states.
     *
     * @param status the new job execution status
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Gets the RabbitMQ delivery tag for message acknowledgment.
     *
     * <p>The delivery tag is used by workers to acknowledge message processing back to the RabbitMQ
     * broker. This ensures reliable message delivery in the distributed system.
     *
     * @return the RabbitMQ delivery tag
     * @throws java.util.NoSuchElementException if no delivery tag has been set
     */
    public long getDeliveryTag() {
        return deliveryTag.get();
    }

    /**
     * Sets the RabbitMQ delivery tag for this job message.
     *
     * <p>This method is called by the orchestration provider when a job message is received from
     * the queue. The delivery tag can only be set once to prevent accidental overwrites that could
     * break message acknowledgment.
     *
     * @param deliveryTag the RabbitMQ delivery tag for message acknowledgment
     * @throws IllegalStateException if a delivery tag has already been set
     */
    public void setDeliveryTag(Long deliveryTag) {
        if (this.deliveryTag.isPresent()) {
            throw new IllegalStateException("Delivery tag already set");
        }
        this.deliveryTag = Optional.of(deliveryTag);
    }

    /**
     * Gets the bulk scan metadata for this individual job.
     *
     * <p>The bulk scan info provides traceability back to the parent bulk scan operation and
     * contains configuration details needed for job execution.
     *
     * @return the bulk scan information object
     */
    public BulkScanInfo getBulkScanInfo() {
        return bulkScanInfo;
    }
}
