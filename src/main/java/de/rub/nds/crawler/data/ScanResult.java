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

    public ScanResult(ScanJob scanJob, Document result) {
        this(scanJob.getBulkScanId(), scanJob.getScanTarget(), scanJob.getStatus(), result);
        if (scanJob.getStatus() == JobStatus.TO_BE_EXECUTED) {
            throw new IllegalArgumentException("ScanJob must not be in TO_BE_EXECUTED state");
        }
    }

    public static ScanResult fromException(ScanJob scanJob, Exception e) {
        if (!scanJob.getStatus().isError()) {
            throw new IllegalArgumentException("ScanJob must be in an error state");
        }
        Document errorDocument = new Document();
        errorDocument.put("exception", e);
        return new ScanResult(scanJob, errorDocument);
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
}
