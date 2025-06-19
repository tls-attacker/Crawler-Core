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

public class ScanResult implements Serializable {

    private String id;

    private final String bulkScan;

    private final ScanTarget scanTarget;

    private final JobStatus jobStatus;

    private final Document result;

    private ScanResult(
            String bulkScan, ScanTarget scanTarget, JobStatus jobStatus, Document result) {
        this.id = UUID.randomUUID().toString();
        this.bulkScan = bulkScan;
        this.scanTarget = scanTarget;
        this.jobStatus = jobStatus;
        this.result = result;
    }

    /**
     * Constructs a new ScanResult from a completed scan job.
     *
     * @param scanJobDescription the scan job description containing target and status information
     * @param result the scan result document (may be null for error states)
     * @throws IllegalArgumentException if the scan job is still in TO_BE_EXECUTED state
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
     * Creates a ScanResult from an exception that occurred during scanning.
     *
     * @param scanJobDescription the scan job description with an error status
     * @param e the exception that occurred
     * @return a new ScanResult containing the exception information
     * @throws IllegalArgumentException if the scan job is not in an error state
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
     * @return the unique result identifier
     */
    @JsonProperty("_id")
    public String getId() {
        return this.id;
    }

    /**
     * Sets the unique identifier for this scan result.
     *
     * @param id the unique result identifier to set
     */
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the bulk scan identifier this result belongs to.
     *
     * @return the parent bulk scan ID
     */
    public String getBulkScan() {
        return this.bulkScan;
    }

    /**
     * Gets the scan target information for this result.
     *
     * @return the scan target containing host and port information
     */
    public ScanTarget getScanTarget() {
        return this.scanTarget;
    }

    /**
     * Gets the scan result document containing the actual scan data or error information.
     *
     * @return the result document, may be null for certain error states
     */
    public Document getResult() {
        return this.result;
    }

    /**
     * Gets the job status indicating the outcome of the scan.
     *
     * @return the job status (SUCCESS, ERROR, TIMEOUT, etc.)
     */
    public JobStatus getResultStatus() {
        return jobStatus;
    }
}
