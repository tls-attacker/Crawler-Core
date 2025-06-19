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
     * Constructs a new BulkScan instance representing a large-scale scanning operation.
     *
     * @param scannerClass the scanner implementation class (used to determine version)
     * @param crawlerClass the crawler implementation class (used to determine version)
     * @param name the name of this bulk scan
     * @param scanConfig the configuration settings for scan execution
     * @param startTime the timestamp when this bulk scan was started (epoch milliseconds)
     * @param monitored whether progress monitoring is enabled for this bulk scan
     * @param notifyUrl optional URL to receive HTTP notification when scan completes
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
     * Gets the unique identifier for this bulk scan. Note: The underscore prefix is required for
     * MongoDB/Jackson serialization.
     *
     * @return the unique bulk scan identifier
     */
    // Getter naming important for correct serialization, do not change!
    public String get_id() {
        return _id;
    }

    /**
     * Gets the name of this bulk scan.
     *
     * @return the bulk scan name
     */
    public String getName() {
        return this.name;
    }

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
     * Checks whether this bulk scan has completed execution.
     *
     * @return true if the bulk scan is finished, false otherwise
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

    /**
     * Gets the number of scan jobs successfully published to the work queue.
     *
     * @return the count of published scan jobs
     */
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

    /**
     * Sets the unique identifier for this bulk scan. Note: The underscore prefix is required for
     * MongoDB/Jackson serialization.
     *
     * @param _id the unique bulk scan identifier
     */
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

    /**
     * Sets whether this bulk scan has completed execution.
     *
     * @param finished true if the bulk scan is finished, false otherwise
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the total number of targets provided for this bulk scan.
     *
     * @param targetsGiven the total target count
     */
    public void setTargetsGiven(int targetsGiven) {
        this.targetsGiven = targetsGiven;
    }

    /**
     * Sets the number of scan jobs successfully published to the work queue.
     *
     * @param scanJobsPublished the count of published scan jobs
     */
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
