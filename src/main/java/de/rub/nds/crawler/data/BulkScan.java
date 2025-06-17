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
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import javax.persistence.Id;

/**
 * Represents a bulk scanning operation with configuration, progress tracking, and metadata.
 *
 * <p>Encapsulates large-scale TLS scanning operations with distributed coordination, progress
 * monitoring, version tracking, and time recording. Designed for MongoDB persistence.
 *
 * @see ScanConfig
 * @see JobStatus
 * @see ScanTarget
 */
public class BulkScan implements Serializable {

    /** Unique identifier for the bulk scan (managed by MongoDB). */
    @Id private String _id;

    /** Human-readable name for the scan operation. */
    private String name;

    /** MongoDB collection name where scan results are stored (auto-generated). */
    private String collectionName;

    /** Configuration parameters for the scanning operation. */
    private ScanConfig scanConfig;

    /** Whether this scan should be monitored for progress updates. */
    private boolean monitored;

    /** Whether the scan operation has completed. */
    private boolean finished;

    /** Start time of the scan operation (epoch milliseconds). */
    private long startTime;

    /** End time of the scan operation (epoch milliseconds). */
    private long endTime;

    /** Total number of targets provided for scanning. */
    private int targetsGiven;

    /** Number of scan jobs successfully published to worker queues. */
    private long scanJobsPublished;

    /** Number of targets that failed hostname resolution. */
    private long scanJobsResolutionErrors;

    /** Number of targets excluded due to denylist filtering. */
    private long scanJobsDenylisted;

    /** Number of successfully completed scans. */
    private int successfulScans;

    /** Counters for tracking job states during scan execution. */
    private Map<JobStatus, Integer> jobStatusCounters = new EnumMap<>(JobStatus.class);

    /** Optional URL for scan completion notifications. */
    private String notifyUrl;

    /** Version of the TLS scanner used for this scan. */
    private String scannerVersion;

    /** Version of the crawler framework used for this scan. */
    private String crawlerVersion;

    /** Date format used for generating collection names with timestamps. */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");

    /**
     * Default constructor for deserialization.
     *
     * <p>This constructor is used by serialization frameworks and should not be called directly.
     */
    @SuppressWarnings("unused")
    private BulkScan() {}

    /**
     * Creates a new BulkScan with the specified configuration and metadata.
     *
     * <p>This constructor initializes a new bulk scan operation with version information extracted
     * from the provided scanner and crawler classes. The collection name is automatically generated
     * using the scan name and start time.
     *
     * @param scannerClass the scanner class to extract version information from
     * @param crawlerClass the crawler class to extract version information from
     * @param name the human-readable name for this scan operation
     * @param scanConfig the scan configuration defining scan parameters
     * @param startTime the start time in epoch milliseconds
     * @param monitored whether this scan should be monitored for progress
     * @param notifyUrl optional URL for completion notifications (may be null)
     */
    public BulkScan(
            Class<?> scannerClass,
            Class<?> crawlerClass,
            String name,
            ScanConfig scanConfig,
            long startTime,
            boolean monitored,
            String notifyUrl) {
        this.scannerVersion = scannerClass.getPackage().getImplementationVersion();
        this.crawlerVersion = crawlerClass.getPackage().getImplementationVersion();
        this.name = name;
        this.scanConfig = scanConfig;
        this.finished = false;
        this.startTime = startTime;
        this.monitored = monitored;
        this.collectionName =
                name + "_" + dateFormat.format(Date.from(Instant.ofEpochMilli(startTime)));
        this.notifyUrl = notifyUrl;
    }

    /**
     * Gets the unique identifier for this bulk scan.
     *
     * <p><strong>Important:</strong> Getter naming is critical for MongoDB serialization. Do not
     * change this method name without considering serialization compatibility.
     *
     * @return the MongoDB document ID
     */
    public String get_id() {
        return _id;
    }

    /**
     * Gets the human-readable name of the bulk scan.
     *
     * @return the scan name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the MongoDB collection name where scan results are stored.
     *
     * <p>The collection name is automatically generated from the scan name and start time in the
     * format: {name}_{yyyy-MM-dd_HH-mm}
     *
     * @return the collection name for scan results
     */
    public String getCollectionName() {
        return this.collectionName;
    }

    /**
     * Gets the scan configuration for this bulk scan.
     *
     * @return the scan configuration containing scan parameters
     */
    public ScanConfig getScanConfig() {
        return this.scanConfig;
    }

    /**
     * Checks whether this bulk scan is being monitored for progress updates.
     *
     * @return true if monitoring is enabled, false otherwise
     */
    public boolean isMonitored() {
        return this.monitored;
    }

    /**
     * Checks whether the bulk scan operation has completed.
     *
     * <p>A scan is considered finished when all target processing and job publishing has been
     * completed, regardless of individual job success or failure.
     *
     * @return true if the scan is finished, false otherwise
     */
    public boolean isFinished() {
        return this.finished;
    }

    /**
     * Gets the start time of the bulk scan operation.
     *
     * @return the start time in epoch milliseconds
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Gets the end time of the bulk scan operation.
     *
     * @return the end time in epoch milliseconds, or 0 if not finished
     */
    public long getEndTime() {
        return this.endTime;
    }

    /**
     * Gets the total number of targets provided for this bulk scan.
     *
     * @return the number of targets given
     */
    public int getTargetsGiven() {
        return this.targetsGiven;
    }

    /**
     * Gets the number of scan jobs successfully published to worker queues.
     *
     * @return the number of published scan jobs
     */
    public long getScanJobsPublished() {
        return this.scanJobsPublished;
    }

    /**
     * Gets the number of successfully completed scans.
     *
     * @return the number of successful scans
     */
    public int getSuccessfulScans() {
        return this.successfulScans;
    }

    /**
     * Gets the notification URL for scan completion callbacks.
     *
     * @return the notification URL, or null if not configured
     */
    public String getNotifyUrl() {
        return this.notifyUrl;
    }

    /**
     * Gets the version of the TLS scanner used for this scan.
     *
     * @return the scanner version string
     */
    public String getScannerVersion() {
        return this.scannerVersion;
    }

    /**
     * Gets the version of the crawler framework used for this scan.
     *
     * @return the crawler version string
     */
    public String getCrawlerVersion() {
        return this.crawlerVersion;
    }

    /**
     * Sets the unique identifier for this bulk scan.
     *
     * <p><strong>Important:</strong> Setter naming is critical for MongoDB serialization. Do not
     * change this method name without considering serialization compatibility.
     *
     * @param _id the MongoDB document ID
     */
    public void set_id(String _id) {
        this._id = _id;
    }

    /**
     * Sets the human-readable name of the bulk scan.
     *
     * @param name the scan name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the MongoDB collection name for scan results.
     *
     * @param collectionName the collection name
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Sets the scan configuration for this bulk scan.
     *
     * @param scanConfig the scan configuration
     */
    public void setScanConfig(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
    }

    /**
     * Sets whether this bulk scan should be monitored for progress updates.
     *
     * @param monitored true to enable monitoring, false otherwise
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets whether the bulk scan operation has completed.
     *
     * @param finished true if the scan is finished, false otherwise
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Sets the start time of the bulk scan operation.
     *
     * @param startTime the start time in epoch milliseconds
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the end time of the bulk scan operation.
     *
     * @param endTime the end time in epoch milliseconds
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the total number of targets provided for this bulk scan.
     *
     * @param targetsGiven the number of targets given
     */
    public void setTargetsGiven(int targetsGiven) {
        this.targetsGiven = targetsGiven;
    }

    /**
     * Sets the number of scan jobs successfully published to worker queues.
     *
     * @param scanJobsPublished the number of published scan jobs
     */
    public void setScanJobsPublished(long scanJobsPublished) {
        this.scanJobsPublished = scanJobsPublished;
    }

    /**
     * Sets the number of successfully completed scans.
     *
     * @param successfulScans the number of successful scans
     */
    public void setSuccessfulScans(int successfulScans) {
        this.successfulScans = successfulScans;
    }

    /**
     * Sets the notification URL for scan completion callbacks.
     *
     * @param notifyUrl the notification URL, or null to disable notifications
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the version of the TLS scanner used for this scan.
     *
     * @param scannerVersion the scanner version string
     */
    public void setScannerVersion(String scannerVersion) {
        this.scannerVersion = scannerVersion;
    }

    /**
     * Sets the version of the crawler framework used for this scan.
     *
     * @param crawlerVersion the crawler version string
     */
    public void setCrawlerVersion(String crawlerVersion) {
        this.crawlerVersion = crawlerVersion;
    }

    /**
     * Gets the job status counters for tracking scan progress.
     *
     * <p>This map contains counters for each {@link JobStatus} value, allowing real-time monitoring
     * of scan progress and completion rates.
     *
     * @return a map of job statuses to their respective counts
     * @see JobStatus
     */
    public Map<JobStatus, Integer> getJobStatusCounters() {
        return jobStatusCounters;
    }

    /**
     * Sets the job status counters for tracking scan progress.
     *
     * @param jobStatusCounters a map of job statuses to their respective counts
     * @see JobStatus
     */
    public void setJobStatusCounters(Map<JobStatus, Integer> jobStatusCounters) {
        this.jobStatusCounters = jobStatusCounters;
    }

    /**
     * Gets the number of targets that failed hostname resolution.
     *
     * @return the number of targets with resolution errors
     */
    public long getScanJobsResolutionErrors() {
        return scanJobsResolutionErrors;
    }

    /**
     * Sets the number of targets that failed hostname resolution.
     *
     * @param scanJobsResolutionErrors the number of targets with resolution errors
     */
    public void setScanJobsResolutionErrors(long scanJobsResolutionErrors) {
        this.scanJobsResolutionErrors = scanJobsResolutionErrors;
    }

    /**
     * Gets the number of targets excluded due to denylist filtering.
     *
     * @return the number of denylisted targets
     */
    public long getScanJobsDenylisted() {
        return scanJobsDenylisted;
    }

    /**
     * Sets the number of targets excluded due to denylist filtering.
     *
     * @param scanJobsDenylisted the number of denylisted targets
     */
    public void setScanJobsDenylisted(long scanJobsDenylisted) {
        this.scanJobsDenylisted = scanJobsDenylisted;
    }
}
