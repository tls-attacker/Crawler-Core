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
