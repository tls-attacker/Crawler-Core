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

public class BulkScan implements Serializable {

    @Id private String _id;

    private String name;

    private String collectionName;

    private ScanConfig scanConfig;

    private boolean monitored;

    private boolean finished;

    private long startTime;

    private long endTime;

    private int targetsGiven;

    private long scanJobsPublished;
    private long scanJobsResolutionErrors;
    private long scanJobsDenylisted;

    private int successfulScans;

    private Map<JobStatus, Integer> jobStatusCounters = new EnumMap<>(JobStatus.class);

    private String notifyUrl;

    private String scannerVersion;

    private String crawlerVersion;

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");

    @SuppressWarnings("unused")
    private BulkScan() {}

    /**
     * Creates a new bulk scan with the specified parameters.
     *
     * @param scannerClass the scanner class used to extract version information
     * @param crawlerClass the crawler class used to extract version information
     * @param name the name of the bulk scan
     * @param scanConfig the scan configuration to use
     * @param startTime the start time in milliseconds since epoch
     * @param monitored whether the bulk scan should be monitored
     * @param notifyUrl the URL to notify when the scan is complete, or null
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
     * Gets the unique identifier of this bulk scan.
     *
     * @return the unique identifier of this bulk scan
     */
    // Getter naming important for correct serialization, do not change!
    public String get_id() {
        return _id;
    }

    /**
     * Gets the name of this bulk scan.
     *
     * @return the name of this bulk scan
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the collection name for storing scan results. The collection name is formed by combining
     * the scan name with the formatted start time.
     *
     * @return the collection name for this bulk scan
     */
    public String getCollectionName() {
        return this.collectionName;
    }

    /**
     * Gets the scan configuration used for this bulk scan.
     *
     * @return the scan configuration
     */
    public ScanConfig getScanConfig() {
        return this.scanConfig;
    }

    /**
     * Checks if this bulk scan is being monitored.
     *
     * @return true if this bulk scan is monitored, false otherwise
     */
    public boolean isMonitored() {
        return this.monitored;
    }

    /**
     * Checks if this bulk scan has finished.
     *
     * @return true if this bulk scan is finished, false otherwise
     */
    public boolean isFinished() {
        return this.finished;
    }

    /**
     * Gets the start time of this bulk scan in milliseconds since epoch.
     *
     * @return the start time in milliseconds
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Gets the end time of this bulk scan in milliseconds since epoch.
     *
     * @return the end time in milliseconds
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
     * Gets the number of scan jobs that were successfully published to the queue.
     *
     * @return the number of published scan jobs
     */
    public long getScanJobsPublished() {
        return this.scanJobsPublished;
    }

    /**
     * Gets the number of successful scans completed in this bulk scan.
     *
     * @return the number of successful scans
     */
    public int getSuccessfulScans() {
        return this.successfulScans;
    }

    /**
     * Gets the URL to notify when the bulk scan is complete.
     *
     * @return the notification URL, or null if not set
     */
    public String getNotifyUrl() {
        return this.notifyUrl;
    }

    /**
     * Gets the version of the scanner used for this bulk scan.
     *
     * @return the scanner version
     */
    public String getScannerVersion() {
        return this.scannerVersion;
    }

    /**
     * Gets the version of the crawler used for this bulk scan.
     *
     * @return the crawler version
     */
    public String getCrawlerVersion() {
        return this.crawlerVersion;
    }

    /**
     * Sets the unique identifier of this bulk scan.
     *
     * @param _id the unique identifier to set
     */
    // Setter naming important for correct serialization, do not change!
    public void set_id(String _id) {
        this._id = _id;
    }

    /**
     * Sets the name of this bulk scan.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the collection name for storing scan results.
     *
     * @param collectionName the collection name to set
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Sets the scan configuration for this bulk scan.
     *
     * @param scanConfig the scan configuration to set
     */
    public void setScanConfig(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
    }

    /**
     * Sets whether this bulk scan is being monitored.
     *
     * @param monitored true if this bulk scan should be monitored, false otherwise
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets whether this bulk scan has finished.
     *
     * @param finished true if this bulk scan is finished, false otherwise
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Sets the start time of this bulk scan.
     *
     * @param startTime the start time in milliseconds since epoch
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the end time of this bulk scan.
     *
     * @param endTime the end time in milliseconds since epoch
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
     * Sets the number of scan jobs that were successfully published to the queue.
     *
     * @param scanJobsPublished the number of published scan jobs
     */
    public void setScanJobsPublished(long scanJobsPublished) {
        this.scanJobsPublished = scanJobsPublished;
    }

    /**
     * Sets the number of successful scans completed in this bulk scan.
     *
     * @param successfulScans the number of successful scans
     */
    public void setSuccessfulScans(int successfulScans) {
        this.successfulScans = successfulScans;
    }

    /**
     * Sets the URL to notify when the bulk scan is complete.
     *
     * @param notifyUrl the notification URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the version of the scanner used for this bulk scan.
     *
     * @param scannerVersion the scanner version
     */
    public void setScannerVersion(String scannerVersion) {
        this.scannerVersion = scannerVersion;
    }

    /**
     * Sets the version of the crawler used for this bulk scan.
     *
     * @param crawlerVersion the crawler version
     */
    public void setCrawlerVersion(String crawlerVersion) {
        this.crawlerVersion = crawlerVersion;
    }

    /**
     * Gets the map of job status counters showing the count of jobs in each status.
     *
     * @return a map from job status to the count of jobs in that status
     */
    public Map<JobStatus, Integer> getJobStatusCounters() {
        return jobStatusCounters;
    }

    /**
     * Sets the map of job status counters.
     *
     * @param jobStatusCounters the map from job status to job count
     */
    public void setJobStatusCounters(Map<JobStatus, Integer> jobStatusCounters) {
        this.jobStatusCounters = jobStatusCounters;
    }

    /**
     * Gets the number of scan jobs that encountered resolution errors.
     *
     * @return the number of scan jobs with resolution errors
     */
    public long getScanJobsResolutionErrors() {
        return scanJobsResolutionErrors;
    }

    /**
     * Sets the number of scan jobs that encountered resolution errors.
     *
     * @param scanJobsResolutionErrors the number of scan jobs with resolution errors
     */
    public void setScanJobsResolutionErrors(long scanJobsResolutionErrors) {
        this.scanJobsResolutionErrors = scanJobsResolutionErrors;
    }

    /**
     * Gets the number of scan jobs that were denylisted.
     *
     * @return the number of denylisted scan jobs
     */
    public long getScanJobsDenylisted() {
        return scanJobsDenylisted;
    }

    /**
     * Sets the number of scan jobs that were denylisted.
     *
     * @param scanJobsDenylisted the number of denylisted scan jobs
     */
    public void setScanJobsDenylisted(long scanJobsDenylisted) {
        this.scanJobsDenylisted = scanJobsDenylisted;
    }
}
