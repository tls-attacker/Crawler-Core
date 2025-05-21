/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.rub.nds.crawler.constant.JobStatus;
import java.io.Serializable;
import java.util.UUID;
import org.bson.Document;

/**
 * Represents the result of a completed scan. Contains information about the scan target, status,
 * and the actual scan results. This class is used to store scan results in the database and for
 * notifications.
 */
public class ScanResult implements Serializable {

    /** Unique identifier for this scan result. */
    private String id;

    /** Reference to the bulk scan this result belongs to. */
    private final String bulkScan;

    /** The target that was scanned. */
    private final ScanTarget scanTarget;

    /** The status of the scan job. */
    private final JobStatus jobStatus;

    /** The actual scan results as a MongoDB document. */
    private final Document result;

    /**
     * Private constructor for creating a scan result.
     *
     * @param bulkScan The bulk scan ID this result belongs to
     * @param scanTarget The target that was scanned
     * @param jobStatus The status of the scan job
     * @param result The actual scan results
     */
    private ScanResult(
            String bulkScan, ScanTarget scanTarget, JobStatus jobStatus, Document result) {
        this.id = UUID.randomUUID().toString();
        this.bulkScan = bulkScan;
        this.scanTarget = scanTarget;
        this.jobStatus = jobStatus;
        this.result = result;
    }

    /**
     * Creates a scan result from a scan job description and result document.
     *
     * @param scanJobDescription The completed scan job description
     * @param result The scan results as a document
     * @throws IllegalArgumentException If the job status is TO_BE_EXECUTED
     */
    public ScanResult(ScanJobDescription scanJobDescription, Document result) {
        this(
                scanJobDescription.getBulkScanInfo().getBulkScanId(),
                scanJobDescription.getScanTarget(),
                scanJobDescription.getStatus(),
                result);
        if (scanJobDescription.getStatus() == JobStatus.TO_BE_EXECUTED) {
            throw new IllegalArgumentException(
                    "ScanJobDescription must not be in TO_BE_EXECUTED state");
        }
    }

    /**
     * Creates a scan result from a scan job description and an exception. Used when a scan fails
     * with an exception.
     *
     * @param scanJobDescription The scan job description that encountered an error
     * @param e The exception that occurred
     * @return A new ScanResult containing the exception information
     * @throws IllegalArgumentException If the job status is not an error state
     */
    public static ScanResult fromException(ScanJobDescription scanJobDescription, Exception e) {
        if (!scanJobDescription.getStatus().isError()) {
            throw new IllegalArgumentException("ScanJobDescription must be in an error state");
        }
        Document errorDocument = new Document();
        errorDocument.put("exception", e);
        return new ScanResult(scanJobDescription, errorDocument);
    }

    /**
     * Gets the unique identifier for this scan result.
     *
     * @return The scan result ID
     */
    @JsonProperty("_id")
    public String getId() {
        return this.id;
    }

    /**
     * Sets the unique identifier for this scan result. Used by MongoDB for document IDs.
     *
     * @param id The scan result ID
     */
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the bulk scan ID this result belongs to.
     *
     * @return The bulk scan ID
     */
    public String getBulkScan() {
        return this.bulkScan;
    }

    /**
     * Gets the target that was scanned.
     *
     * @return The scan target
     */
    public ScanTarget getScanTarget() {
        return this.scanTarget;
    }

    /**
     * Gets the actual scan results.
     *
     * @return The scan results as a MongoDB document
     */
    public Document getResult() {
        return this.result;
    }

    /**
     * Gets the status of the scan job.
     *
     * @return The job status
     */
    public JobStatus getResultStatus() {
        return jobStatus;
    }
}
