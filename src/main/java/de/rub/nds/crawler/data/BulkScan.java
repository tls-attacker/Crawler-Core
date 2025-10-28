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
     * Creates a new bulk scan with the specified configuration.
     *
     * @param scannerClass the scanner class used for scanning
     * @param crawlerClass the crawler class used for crawling
     * @param name the name of the bulk scan
     * @param scanConfig the scan configuration
     * @param startTime the start time in milliseconds since epoch
     * @param monitored whether the scan should be monitored
     * @param notifyUrl the URL to notify when scan completes
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
     * Gets the unique identifier of the bulk scan.
     *
     * @return the bulk scan ID
     */
    // Getter naming important for correct serialization, do not change!
    public String get_id() {
        return _id;
    }

    /**
     * Gets the name of the bulk scan.
     *
     * @return the scan name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the collection name for this bulk scan.
     *
     * @return the collection name
     */
    public String getCollectionName() {
        return this.collectionName;
    }

    /**
     * Gets the scan configuration.
     *
     * @return the scan configuration
     */
    public ScanConfig getScanConfig() {
        return this.scanConfig;
    }

    /**
     * Checks if the bulk scan is monitored.
     *
     * @return true if monitored, false otherwise
     */
    public boolean isMonitored() {
        return this.monitored;
    }

    /**
     * Checks if the bulk scan has finished.
     *
     * @return true if finished, false otherwise
     */
    public boolean isFinished() {
        return this.finished;
    }

    /**
     * Gets the start time of the bulk scan.
     *
     * @return the start time in milliseconds since epoch
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Gets the end time of the bulk scan.
     *
     * @return the end time in milliseconds since epoch
     */
    public long getEndTime() {
        return this.endTime;
    }

    /**
     * Gets the number of targets given for scanning.
     *
     * @return the number of targets
     */
    public int getTargetsGiven() {
        return this.targetsGiven;
    }

    /**
     * Gets the number of scan jobs published.
     *
     * @return the number of published scan jobs
     */
    public long getScanJobsPublished() {
        return this.scanJobsPublished;
    }

    /**
     * Gets the number of successful scans.
     *
     * @return the number of successful scans
     */
    public int getSuccessfulScans() {
        return this.successfulScans;
    }

    /**
     * Gets the notification URL.
     *
     * @return the URL to notify when scan completes
     */
    public String getNotifyUrl() {
        return this.notifyUrl;
    }

    /**
     * Gets the scanner version used for this bulk scan.
     *
     * @return the scanner version
     */
    public String getScannerVersion() {
        return this.scannerVersion;
    }

    /**
     * Gets the crawler version used for this bulk scan.
     *
     * @return the crawler version
     */
    public String getCrawlerVersion() {
        return this.crawlerVersion;
    }

    /**
     * Sets the unique identifier of the bulk scan.
     *
     * @param _id the bulk scan ID
     */
    // Setter naming important for correct serialization, do not change!
    public void set_id(String _id) {
        this._id = _id;
    }

    /**
     * Sets the name of the bulk scan.
     *
     * @param name the scan name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the collection name for this bulk scan.
     *
     * @param collectionName the collection name
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Sets the scan configuration.
     *
     * @param scanConfig the scan configuration
     */
    public void setScanConfig(ScanConfig scanConfig) {
        this.scanConfig = scanConfig;
    }

    /**
     * Sets whether the bulk scan is monitored.
     *
     * @param monitored true to enable monitoring, false otherwise
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets whether the bulk scan has finished.
     *
     * @param finished true if finished, false otherwise
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Sets the start time of the bulk scan.
     *
     * @param startTime the start time in milliseconds since epoch
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the end time of the bulk scan.
     *
     * @param endTime the end time in milliseconds since epoch
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the number of targets given for scanning.
     *
     * @param targetsGiven the number of targets
     */
    public void setTargetsGiven(int targetsGiven) {
        this.targetsGiven = targetsGiven;
    }

    /**
     * Sets the number of scan jobs published.
     *
     * @param scanJobsPublished the number of published scan jobs
     */
    public void setScanJobsPublished(long scanJobsPublished) {
        this.scanJobsPublished = scanJobsPublished;
    }

    /**
     * Sets the number of successful scans.
     *
     * @param successfulScans the number of successful scans
     */
    public void setSuccessfulScans(int successfulScans) {
        this.successfulScans = successfulScans;
    }

    /**
     * Sets the notification URL.
     *
     * @param notifyUrl the URL to notify when scan completes
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the scanner version used for this bulk scan.
     *
     * @param scannerVersion the scanner version
     */
    public void setScannerVersion(String scannerVersion) {
        this.scannerVersion = scannerVersion;
    }

    /**
     * Sets the crawler version used for this bulk scan.
     *
     * @param crawlerVersion the crawler version
     */
    public void setCrawlerVersion(String crawlerVersion) {
        this.crawlerVersion = crawlerVersion;
    }

    /**
     * Gets the map of job status counters.
     *
     * @return the job status counters map
     */
    public Map<JobStatus, Integer> getJobStatusCounters() {
        return jobStatusCounters;
    }

    /**
     * Sets the map of job status counters.
     *
     * @param jobStatusCounters the job status counters map
     */
    public void setJobStatusCounters(Map<JobStatus, Integer> jobStatusCounters) {
        this.jobStatusCounters = jobStatusCounters;
    }

    /**
     * Gets the number of scan jobs with resolution errors.
     *
     * @return the number of scan jobs with resolution errors
     */
    public long getScanJobsResolutionErrors() {
        return scanJobsResolutionErrors;
    }

    /**
     * Sets the number of scan jobs with resolution errors.
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
