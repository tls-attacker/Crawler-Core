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
 * Represents a bulk scanning operation that manages multiple TLS scanning jobs. This class tracks
 * metadata about a scan batch including scan configuration, timing information, job statistics, and
 * version information.
 */
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
     * Creates a new bulk scan with the given parameters.
     *
     * @param scannerClass A scanner implementation class for retrieving version information
     * @param crawlerClass A crawler implementation class for retrieving version information
     * @param name The name of the bulk scan
     * @param scanConfig The configuration to use for this scan
     * @param startTime The start time as a timestamp in milliseconds
     * @param monitored Whether this scan should be monitored for progress
     * @param notifyUrl Optional URL to notify when the scan is complete
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

    // Getter naming important for correct serialization, do not change!
    /**
     * Gets the database ID for this bulk scan.
     *
     * @return The database ID
     */
    public String get_id() {
        return _id;
    }

    /**
     * Gets the name of this bulk scan.
     *
     * @return The name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the collection name where scan results will be stored.
     *
     * @return The collection name
     */
    public String getCollectionName() {
        return this.collectionName;
    }

    /**
     * Gets the scan configuration for this bulk scan.
     *
     * @return The scan configuration
     */
    public ScanConfig getScanConfig() {
        return this.scanConfig;
    }

    /**
     * Checks if this bulk scan is monitored for progress.
     *
     * @return True if the scan is monitored, false otherwise
     */
    public boolean isMonitored() {
        return this.monitored;
    }

    /**
     * Checks if this bulk scan has finished.
     *
     * @return True if the scan is finished, false otherwise
     */
    public boolean isFinished() {
        return this.finished;
    }

    /**
     * Gets the start time of this bulk scan.
     *
     * @return The start time as a timestamp in milliseconds
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Gets the end time of this bulk scan.
     *
     * @return The end time as a timestamp in milliseconds
     */
    public long getEndTime() {
        return this.endTime;
    }

    /**
     * Gets the total number of targets provided for this bulk scan.
     *
     * @return The number of targets
     */
    public int getTargetsGiven() {
        return this.targetsGiven;
    }

    /**
     * Gets the number of scan jobs published for this bulk scan.
     *
     * @return The number of scan jobs published
     */
    public long getScanJobsPublished() {
        return this.scanJobsPublished;
    }

    /**
     * Gets the number of successful scans completed for this bulk scan.
     *
     * @return The number of successful scans
     */
    public int getSuccessfulScans() {
        return this.successfulScans;
    }

    /**
     * Gets the URL to notify when this bulk scan is complete.
     *
     * @return The notification URL
     */
    public String getNotifyUrl() {
        return this.notifyUrl;
    }

    /**
     * Gets the version of the scanner used for this bulk scan.
     *
     * @return The scanner version
     */
    public String getScannerVersion() {
        return this.scannerVersion;
    }

    /**
     * Gets the version of the crawler used for this bulk scan.
     *
     * @return The crawler version
     */
    public String getCrawlerVersion() {
        return this.crawlerVersion;
    }

    // Setter naming important for correct serialization, do not change!
    /**
     * Sets the database ID for this bulk scan.
     *
     * @param _id The database ID
     */
    public void set_id(String _id) {
        this._id = _id;
    }

    /**
     * Sets the name of this bulk scan.
     *
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the collection name where scan results will be stored.
     *
     * @param collectionName The collection name
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Sets the scan configuration for this bulk scan.
     *
     * @param scanConfig The scan configuration
     */
    public void setScanConfig(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
    }

    /**
     * Sets whether this bulk scan is monitored for progress.
     *
     * @param monitored True if the scan should be monitored, false otherwise
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets whether this bulk scan is finished.
     *
     * @param finished True if the scan is finished, false otherwise
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Sets the start time of this bulk scan.
     *
     * @param startTime The start time as a timestamp in milliseconds
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the end time of this bulk scan.
     *
     * @param endTime The end time as a timestamp in milliseconds
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the total number of targets for this bulk scan.
     *
     * @param targetsGiven The number of targets
     */
    public void setTargetsGiven(int targetsGiven) {
        this.targetsGiven = targetsGiven;
    }

    /**
     * Sets the number of scan jobs published for this bulk scan.
     *
     * @param scanJobsPublished The number of scan jobs published
     */
    public void setScanJobsPublished(long scanJobsPublished) {
        this.scanJobsPublished = scanJobsPublished;
    }

    /**
     * Sets the number of successful scans completed for this bulk scan.
     *
     * @param successfulScans The number of successful scans
     */
    public void setSuccessfulScans(int successfulScans) {
        this.successfulScans = successfulScans;
    }

    /**
     * Sets the URL to notify when this bulk scan is complete.
     *
     * @param notifyUrl The notification URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the version of the scanner used for this bulk scan.
     *
     * @param scannerVersion The scanner version
     */
    public void setScannerVersion(String scannerVersion) {
        this.scannerVersion = scannerVersion;
    }

    /**
     * Sets the version of the crawler used for this bulk scan.
     *
     * @param crawlerVersion The crawler version
     */
    public void setCrawlerVersion(String crawlerVersion) {
        this.crawlerVersion = crawlerVersion;
    }

    /**
     * Gets the job status counters for this bulk scan.
     *
     * @return A map of job status to count
     */
    public Map<JobStatus, Integer> getJobStatusCounters() {
        return jobStatusCounters;
    }

    /**
     * Sets the job status counters for this bulk scan.
     *
     * @param jobStatusCounters A map of job status to count
     */
    public void setJobStatusCounters(Map<JobStatus, Integer> jobStatusCounters) {
        this.jobStatusCounters = jobStatusCounters;
    }

    /**
     * Gets the number of scan jobs that failed due to domain resolution errors.
     *
     * @return The number of resolution errors
     */
    public long getScanJobsResolutionErrors() {
        return scanJobsResolutionErrors;
    }

    /**
     * Sets the number of scan jobs that failed due to domain resolution errors.
     *
     * @param scanJobsResolutionErrors The number of resolution errors
     */
    public void setScanJobsResolutionErrors(long scanJobsResolutionErrors) {
        this.scanJobsResolutionErrors = scanJobsResolutionErrors;
    }

    /**
     * Gets the number of scan jobs skipped due to denylisting.
     *
     * @return The number of denylisted scan jobs
     */
    public long getScanJobsDenylisted() {
        return scanJobsDenylisted;
    }

    /**
     * Sets the number of scan jobs skipped due to denylisting.
     *
     * @param scanJobsDenylisted The number of denylisted scan jobs
     */
    public void setScanJobsDenylisted(long scanJobsDenylisted) {
        this.scanJobsDenylisted = scanJobsDenylisted;
    }
}
