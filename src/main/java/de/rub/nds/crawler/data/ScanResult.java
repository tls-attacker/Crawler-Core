/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import com.fasterxml.jackson.annotation.JsonCreator;
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

    @JsonCreator
    private ScanResult(
            @JsonProperty("bulkScan") String bulkScan,
            @JsonProperty("scanTarget") ScanTarget scanTarget,
            @JsonProperty("resultStatus") JobStatus jobStatus,
            @JsonProperty("result") Document result) {
        this.id = UUID.randomUUID().toString();
        this.bulkScan = bulkScan;
        this.scanTarget = scanTarget;
        this.jobStatus = jobStatus;
        this.result = result;
    }

    /**
     * Creates a new scan result from a scan job description.
     *
     * @param scanJobDescription the scan job description
     * @param result the scan result document
     * @throws IllegalArgumentException if the scan job description is in TO_BE_EXECUTED state
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
     * Creates a scan result from an exception.
     *
     * @param scanJobDescription the scan job description
     * @param e the exception that occurred
     * @return a new scan result containing the exception
     * @throws IllegalArgumentException if the scan job description is not in an error state
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
     * Gets the unique identifier of the scan result.
     *
     * @return the scan result ID
     */
    @JsonProperty("_id")
    public String getId() {
        return this.id;
    }

    /**
     * Sets the unique identifier of the scan result.
     *
     * @param id the scan result ID
     */
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the bulk scan ID associated with this result.
     *
     * @return the bulk scan ID
     */
    public String getBulkScan() {
        return this.bulkScan;
    }

    /**
     * Gets the scan target for this result.
     *
     * @return the scan target
     */
    public ScanTarget getScanTarget() {
        return this.scanTarget;
    }

    /**
     * Gets the result document containing scan data.
     *
     * @return the result document
     */
    public Document getResult() {
        return this.result;
    }

    /**
     * Gets the job status of this scan result.
     *
     * @return the job status
     */
    public JobStatus getResultStatus() {
        return jobStatus;
    }
}
