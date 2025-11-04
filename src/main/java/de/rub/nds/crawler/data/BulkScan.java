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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import javax.persistence.Id;

/**
 * Represents a bulk scanning operation that manages multiple scanning jobs. This class tracks
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

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").withZone(ZoneId.systemDefault());

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
        this.collectionName = name + "_" + DATE_FORMATTER.format(Instant.ofEpochMilli(startTime));
        this.notifyUrl = notifyUrl;
    }

    /**
     * Gets the database ID for this bulk scan.
     *
     * @return The database ID
     */
    public String get_id() {
        // Getter naming important for correct serialization, do not change!
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

    public void set_id(String _id) {
        // Setter naming important for correct serialization, do not change!
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
