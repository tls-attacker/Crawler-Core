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
import java.time.Instant;
import java.util.UUID;
import org.bson.Document;

public class ScanResult implements Serializable {

    private String id;

    private final String bulkScan;

    private final ScanTarget scanTarget;

    private final JobStatus jobStatus;

    private final Document result;

    private final String scanJobDescriptionId;

    private final Instant timestamp;

    @JsonCreator
    private ScanResult(
            @JsonProperty("scanJobDescription") String scanJobDescriptionId,
            @JsonProperty("bulkScan") String bulkScan,
            @JsonProperty("scanTarget") ScanTarget scanTarget,
            @JsonProperty("resultStatus") JobStatus jobStatus,
            @JsonProperty("result") Document result,
            @JsonProperty("timestamp") Instant timestamp) {
        this.id = UUID.randomUUID().toString();
        this.scanJobDescriptionId = scanJobDescriptionId;
        this.bulkScan = bulkScan;
        this.scanTarget = scanTarget;
        this.jobStatus = jobStatus;
        this.result = result;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public ScanResult(ScanJobDescription scanJobDescription, Document result) {
        this(
                scanJobDescription.getId().toString(),
                scanJobDescription.getBulkScanInfo().getBulkScanId(),
                scanJobDescription.getScanTarget(),
                scanJobDescription.getStatus(),
                result,
                Instant.now());
        if (scanJobDescription.getStatus() == JobStatus.TO_BE_EXECUTED) {
            throw new IllegalArgumentException(
                    "ScanJobDescription must not be in TO_BE_EXECUTED state");
        }
        // Key the document by the job ID so that partial results and the final result share the
        // same _id, letting the final result overwrite any partial result written during the scan.
        this.id = scanJobDescription.getId().toString();
    }

    /**
     * Builds an in-progress (partial) scan result for the given job. The result is keyed by the job
     * ID and marked {@link JobStatus#RUNNING} so that successive partial writes — and the final
     * result — overwrite the same document. Unlike the public constructor, this does not require
     * the job to have left the {@link JobStatus#TO_BE_EXECUTED} state.
     *
     * @param scanJobDescription The job the partial result belongs to.
     * @param result The partial result content (same shape as the final result content).
     * @return A ScanResult in {@link JobStatus#RUNNING} state, keyed by the job ID.
     */
    public static ScanResult partialResult(ScanJobDescription scanJobDescription, Document result) {
        ScanResult scanResult =
                new ScanResult(
                        scanJobDescription.getId().toString(),
                        scanJobDescription.getBulkScanInfo().getBulkScanId(),
                        scanJobDescription.getScanTarget(),
                        JobStatus.RUNNING,
                        result,
                        Instant.now());
        scanResult.setId(scanJobDescription.getId().toString());
        return scanResult;
    }

    public static ScanResult fromException(ScanJobDescription scanJobDescription, Exception e) {
        if (!scanJobDescription.getStatus().isError()) {
            throw new IllegalArgumentException("ScanJobDescription must be in an error state");
        }
        Document errorDocument = new Document();
        errorDocument.put("exception", e);
        return new ScanResult(scanJobDescription, errorDocument);
    }

    @JsonProperty("_id")
    public String getId() {
        return this.id;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    public String getBulkScan() {
        return this.bulkScan;
    }

    public ScanTarget getScanTarget() {
        return this.scanTarget;
    }

    public Document getResult() {
        return this.result;
    }

    public JobStatus getResultStatus() {
        return jobStatus;
    }

    @JsonProperty("scanJobDescription")
    public String getScanJobDescriptionId() {
        return scanJobDescriptionId;
    }

    @JsonProperty("timestamp")
    public Instant getTimestamp() {
        return timestamp;
    }
}
