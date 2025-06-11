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
 * Represents a bulk scanning operation with its configuration, progress tracking, and metadata.
 *
 * <p>A BulkScan encapsulates all information about a large-scale TLS scanning operation, including
 * the scan configuration, target statistics, job status tracking, and version information. This
 * class serves as the primary coordination entity for distributed scanning operations.
 *
 * <p>The bulk scan lifecycle typically follows this pattern:
 *
 * <ol>
 *   <li>Creation with scan configuration and target list
 *   <li>Target processing and job publishing to worker queues
 *   <li>Progress monitoring through job status counters
 *   <li>Completion marking and result aggregation
 * </ol>
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Distributed coordination</strong> - Tracks jobs across multiple worker instances
 *   <li><strong>Progress monitoring</strong> - Real-time status counters for different job states
 *   <li><strong>Version tracking</strong> - Records scanner and crawler versions for
 *       reproducibility
 *   <li><strong>Time tracking</strong> - Start and end time recording for performance analysis
 *   <li><strong>Collection management</strong> - Automatic database collection naming with
 *       timestamps
 * </ul>
 *
 * <p><strong>Persistence:</strong> This class is designed for MongoDB persistence with JPA
 * annotations. Method naming follows serialization conventions and should not be changed without
 * considering backward compatibility.
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

    public ScanConfig getScanConfig() {
        return this.scanConfig;
    }

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

    public long getStartTime() {
        return this.startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public int getTargetsGiven() {
        return this.targetsGiven;
    }

    public long getScanJobsPublished() {
        return this.scanJobsPublished;
    }

    public int getSuccessfulScans() {
        return this.successfulScans;
    }

    public String getNotifyUrl() {
        return this.notifyUrl;
    }

    public String getScannerVersion() {
        return this.scannerVersion;
    }

    public String getCrawlerVersion() {
        return this.crawlerVersion;
    }

    // Setter naming important for correct serialization, do not change!
    public void set_id(String _id) {
        this._id = _id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setScanConfig(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
    }

    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setTargetsGiven(int targetsGiven) {
        this.targetsGiven = targetsGiven;
    }

    public void setScanJobsPublished(long scanJobsPublished) {
        this.scanJobsPublished = scanJobsPublished;
    }

    public void setSuccessfulScans(int successfulScans) {
        this.successfulScans = successfulScans;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public void setScannerVersion(String scannerVersion) {
        this.scannerVersion = scannerVersion;
    }

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

    public void setJobStatusCounters(Map<JobStatus, Integer> jobStatusCounters) {
        this.jobStatusCounters = jobStatusCounters;
    }

    public long getScanJobsResolutionErrors() {
        return scanJobsResolutionErrors;
    }

    public void setScanJobsResolutionErrors(long scanJobsResolutionErrors) {
        this.scanJobsResolutionErrors = scanJobsResolutionErrors;
    }

    public long getScanJobsDenylisted() {
        return scanJobsDenylisted;
    }

    public void setScanJobsDenylisted(long scanJobsDenylisted) {
        this.scanJobsDenylisted = scanJobsDenylisted;
    }
}
