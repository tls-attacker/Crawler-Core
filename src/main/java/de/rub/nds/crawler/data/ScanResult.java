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
     * Creates a scan result from a scan job description and the scan result document.
     *
     * @param scanJobDescription the scan job description containing target and status information
     * @param result the scan result data as a MongoDB document
     * @throws IllegalArgumentException if the scan job status is TO_BE_EXECUTED
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
     * Creates a scan result from a scan job description and an exception that occurred during
     * scanning.
     *
     * @param scanJobDescription the scan job description containing target and error status
     * @param e the exception that occurred during scanning
     * @return a scan result containing the exception information
     * @throws IllegalArgumentException if the scan job status is not an error status
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
     * Gets the unique identifier of this scan result.
     *
     * @return the unique identifier
     */
    @JsonProperty("_id")
    public String getId() {
        return this.id;
    }

    /**
     * Sets the unique identifier of this scan result.
     *
     * @param id the unique identifier to set
     */
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the bulk scan identifier that this result belongs to.
     *
     * @return the bulk scan identifier
     */
    public String getBulkScan() {
        return this.bulkScan;
    }

    /**
     * Gets the scan target that was scanned to produce this result.
     *
     * @return the scan target
     */
    public ScanTarget getScanTarget() {
        return this.scanTarget;
    }

    /**
     * Gets the scan result data as a MongoDB document.
     *
     * @return the result document containing scan data or error information
     */
    public Document getResult() {
        return this.result;
    }

    /**
     * Gets the job status indicating the outcome of the scan.
     *
     * @return the job status
     */
    public JobStatus getResultStatus() {
        return jobStatus;
    }
}
