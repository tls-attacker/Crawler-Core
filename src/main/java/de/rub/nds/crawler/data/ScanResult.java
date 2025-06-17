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
 * Immutable container for TLS scan results and metadata.
 *
 * <p>Encapsulates scan outcome including target, status, result data, and traceability. Supports
 * both successful results and error conditions. Uses Jackson/BSON for persistence.
 *
 * @see ScanJobDescription
 * @see ScanTarget
 * @see JobStatus
 * @see BulkScanInfo
 */
public class ScanResult implements Serializable {

    /** Unique identifier for this scan result record. */
    private String id;

    /** Identifier of the bulk scan operation that produced this result. */
    private final String bulkScan;

    /** Target specification that was scanned to produce this result. */
    private final ScanTarget scanTarget;

    /** Final execution status indicating success, failure, or error condition. */
    private final JobStatus jobStatus;

    /** MongoDB document containing the actual scan results or error information. */
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
     * Creates a new scan result from a completed scan job description and result document.
     *
     * <p>This is the primary constructor for creating scan results from successful or failed scan
     * operations. It extracts metadata from the scan job description and associates it with the
     * result document from the scanning process.
     *
     * <p><strong>Status Validation:</strong> The constructor validates that the scan job has
     * completed execution by checking that its status is not TO_BE_EXECUTED. This ensures that only
     * completed scan jobs are converted to results.
     *
     * <p><strong>Metadata Extraction:</strong> The constructor extracts key information from the
     * scan job description including the bulk scan ID, scan target, and execution status to
     * populate the result object.
     *
     * @param scanJobDescription the completed scan job containing metadata and final status
     * @param result the BSON document containing scan results, may be null for empty results
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
     * Creates scan result from exception during scan execution.
     *
     * <p>Creates structured error document with exception details. Validates scan job is in error
     * state.
     *
     * @param scanJobDescription scan job in error state
     * @param e exception that caused scan failure
     * @return ScanResult containing exception details
     * @throws IllegalArgumentException if scan job not in error state
     */
    public static ScanResult fromException(ScanJobDescription scanJobDescription, Exception e) {
        if (!scanJobDescription.getStatus().isError()) {
            throw new IllegalArgumentException("ScanJobDescription must be in an error state");
        }
        Document errorDocument = new Document();

        // Store structured exception information for better analysis and debugging
        errorDocument.put("exceptionType", e.getClass().getSimpleName());
        errorDocument.put("exceptionMessage", e.getMessage());
        errorDocument.put("exceptionCause", e.getCause() != null ? e.getCause().toString() : null);
        errorDocument.put("timestamp", System.currentTimeMillis());

        // Include target information if available for context
        ScanTarget target = scanJobDescription.getScanTarget();
        if (target != null) {
            errorDocument.put("targetHostname", target.getHostname());
            errorDocument.put("targetIp", target.getIp());
            errorDocument.put("targetPort", target.getPort());

            // Include additional error context from the target if available
            if (target.getErrorMessage() != null) {
                errorDocument.put("targetErrorMessage", target.getErrorMessage());
            }
            if (target.getErrorType() != null) {
                errorDocument.put("targetErrorType", target.getErrorType());
            }
        }

        return new ScanResult(scanJobDescription, errorDocument);
    }

    /**
     * Creates scan result from exception with additional error context.
     *
     * @param scanJobDescription scan job in error state
     * @param e the exception that caused the scan to fail
     * @param errorContext additional error context as key-value pairs
     * @return a new ScanResult containing the exception details and additional context
     * @throws IllegalArgumentException if the scan job is not in an error state
     */
    public static ScanResult fromException(
            ScanJobDescription scanJobDescription, Exception e, String errorContext) {
        if (!scanJobDescription.getStatus().isError()) {
            throw new IllegalArgumentException("ScanJobDescription must be in an error state");
        }
        Document errorDocument = new Document();

        // Store structured exception information
        errorDocument.put("exceptionType", e.getClass().getSimpleName());
        errorDocument.put("exceptionMessage", e.getMessage());
        errorDocument.put("exceptionCause", e.getCause() != null ? e.getCause().toString() : null);
        errorDocument.put("timestamp", System.currentTimeMillis());
        errorDocument.put("errorContext", errorContext);

        // Include target information if available for context
        ScanTarget target = scanJobDescription.getScanTarget();
        if (target != null) {
            errorDocument.put("targetHostname", target.getHostname());
            errorDocument.put("targetIp", target.getIp());
            errorDocument.put("targetPort", target.getPort());

            // Include additional error context from the target if available
            if (target.getErrorMessage() != null) {
                errorDocument.put("targetErrorMessage", target.getErrorMessage());
            }
            if (target.getErrorType() != null) {
                errorDocument.put("targetErrorType", target.getErrorType());
            }
        }

        return new ScanResult(scanJobDescription, errorDocument);
    }

    /**
     * Gets the unique identifier for this scan result.
     *
     * <p>The ID is a UUID string that serves as the primary key for database storage and unique
     * identification of scan results across the system.
     *
     * @return the unique ID string for this scan result
     */
    @JsonProperty("_id")
    public String getId() {
        return this.id;
    }

    /**
     * Sets the unique identifier for this scan result.
     *
     * <p>This method is primarily used by serialization frameworks and database drivers to set the
     * ID when loading results from persistent storage.
     *
     * @param id the unique ID string to assign to this scan result
     */
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the bulk scan ID that this result belongs to.
     *
     * <p>This provides traceability back to the bulk scanning campaign that generated this
     * individual scan result.
     *
     * @return the bulk scan ID string
     */
    public String getBulkScan() {
        return this.bulkScan;
    }

    /**
     * Gets the scan target (host and port) that was scanned.
     *
     * @return the scan target containing hostname and port information
     */
    public ScanTarget getScanTarget() {
        return this.scanTarget;
    }

    /**
     * Gets the result document containing scan findings or error details.
     *
     * <p>For successful scans, this contains the TLS scanner output in BSON format. For failed
     * scans created via fromException(), this contains exception details. May be null for scans
     * that completed but produced no results.
     *
     * @return the BSON document containing scan results or error information, may be null
     */
    public Document getResult() {
        return this.result;
    }

    /**
     * Gets the final execution status of the scan job.
     *
     * <p>This status indicates how the scan completed, including success, various error conditions,
     * timeouts, and cancellations.
     *
     * @return the final job status for this scan result
     * @see JobStatus
     */
    public JobStatus getResultStatus() {
        return jobStatus;
    }
}
