/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;
import de.rub.nds.crawler.constant.CruxListNumber;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.targetlist.*;
import de.rub.nds.scanner.core.config.ScannerDetail;
import org.apache.commons.validator.routines.UrlValidator;
import org.quartz.CronScheduleBuilder;

/**
 * Abstract base configuration class for TLS-Crawler controller commands.
 * This class provides common configuration parameters for controlling bulk scans
 * including target host selection, scan scheduling, monitoring, and notification options.
 */
public abstract class ControllerCommandConfig {

    @ParametersDelegate private final RabbitMqDelegate rabbitMqDelegate;

    @ParametersDelegate private final MongoDbDelegate mongoDbDelegate;

    @Parameter(names = "-portToBeScanned", description = "The port that should be scanned.")
    private int port = 443;

    @Parameter(names = "-scanDetail")
    private ScannerDetail scanDetail = ScannerDetail.NORMAL;

    @Parameter(
            names = "-timeout",
            validateWith = PositiveInteger.class,
            description = "The timeout to use inside the TLS-Scanner.")
    private int scannerTimeout = 2000;

    @Parameter(
            names = "-reexecutions",
            validateWith = PositiveInteger.class,
            description = "Number of reexecutions to use in the TLS-Scanner.")
    private int reexecutions = 3;

    @Parameter(
            names = "-scanCronInterval",
            validateWith = CronSyntax.class,
            description =
                    "A cron expression which defines the interval of when scans are started. Leave empty to only start one scan immediately.")
    private String scanCronInterval;

    @Parameter(names = "-scanName", description = "The name of the scan")
    private String scanName;

    @Parameter(
            names = "-hostFile",
            description = "A file of a list of servers which should be scanned.")
    private String hostFile;

    @Parameter(
            names = "-denylist",
            description = "A file with a list of IP-Ranges or domains which should not be scanned.")
    private String denylistFile;

    @Parameter(
            names = "-monitorScan",
            description = "If set the progress of the scans is monitored and logged.")
    private boolean monitored;

    @Parameter(
            names = "-notifyUrl",
            description =
                    "If set the controller sends a HTTP Post request including the BulkScan object in JSON after a BulkScan is finished to the specified URL.")
    private String notifyUrl;

    @Parameter(
            names = "-tranco",
            description = "Number of top x hosts of the tranco list that should be scanned")
    private int tranco;

    @Parameter(
            names = "-crux",
            description = "Number of top x hosts of the crux list that should be scanned")
    private CruxListNumber crux;

    @Parameter(names = "-trancoEmail", description = "MX record for number of top x hosts")
    private int trancoEmail;

    /**
     * Constructs a new ControllerCommandConfig with default delegate instances.
     */
    public ControllerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    /**
     * Validates the configuration parameters.
     * 
     * @throws ParameterException if no target host source is specified, if notify URL is set
     *                           without monitoring enabled, or if notify URL is invalid
     */
    public void validate() {
        if (hostFile == null && tranco == 0 && trancoEmail == 0 && crux == null) {
            throw new ParameterException(
                    "You have to either pass a hostFile, specify a number of tranco hosts or specify a number of crux hosts");
        }
        if (notifyUrl != null && !notifyUrl.isEmpty() && !notifyUrl.isBlank() && !monitored) {
            throw new ParameterException(
                    "If a notify message should be sent the scan has to be monitored (-monitorScan)");
        }
        if (notifyUrl != null
                && !notifyUrl.isEmpty()
                && !notifyUrl.isBlank()
                && !new UrlValidator().isValid(notifyUrl)) {
            throw new ParameterException("Provided notify URI is not a valid URI");
        }
    }

    public static class PositiveInteger implements IParameterValidator {
        /**
         * Validates that the parameter value is a positive integer.
         * 
         * @param name the parameter name
         * @param value the parameter value to validate
         * @throws ParameterException if the value is negative
         */
        public void validate(String name, String value) throws ParameterException {
            int n = Integer.parseInt(value);
            if (n < 0) {
                throw new ParameterException(
                        "Parameter " + name + " should be positive (found " + value + ")");
            }
        }
    }

    public static class CronSyntax implements IParameterValidator {
        /**
         * Validates that the parameter value is a valid cron expression.
         * 
         * @param name the parameter name
         * @param value the cron expression to validate
         * @throws ParameterException if the cron expression is invalid
         */
        public void validate(String name, String value) throws ParameterException {
            CronScheduleBuilder.cronSchedule(value);
        }
    }

    /**
     * Gets the RabbitMQ delegate containing RabbitMQ connection configuration.
     * 
     * @return the RabbitMQ delegate
     */
    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    /**
     * Gets the MongoDB delegate containing MongoDB connection configuration.
     * 
     * @return the MongoDB delegate
     */
    public MongoDbDelegate getMongoDbDelegate() {
        return mongoDbDelegate;
    }

    /**
     * Gets the port to be scanned.
     * 
     * @return the port number (default: 443)
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port to be scanned.
     * 
     * @param port the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the scanner detail level for the scan.
     * 
     * @return the scanner detail level (default: NORMAL)
     */
    public ScannerDetail getScanDetail() {
        return scanDetail;
    }

    /**
     * Gets the timeout value for the TLS-Scanner.
     * 
     * @return the timeout in milliseconds (default: 2000)
     */
    public int getScannerTimeout() {
        return scannerTimeout;
    }

    /**
     * Gets the number of reexecutions to use in the TLS-Scanner.
     * 
     * @return the number of reexecutions (default: 3)
     */
    public int getReexecutions() {
        return reexecutions;
    }

    /**
     * Gets the cron expression defining when scans are started.
     * 
     * @return the cron expression, or null if only one immediate scan should be started
     */
    public String getScanCronInterval() {
        return scanCronInterval;
    }

    /**
     * Gets the name of the scan.
     * 
     * @return the scan name
     */
    public String getScanName() {
        return scanName;
    }

    /**
     * Gets the path to the file containing the list of servers to be scanned.
     * 
     * @return the host file path, or null if not specified
     */
    public String getHostFile() {
        return hostFile;
    }

    /**
     * Sets the path to the file containing the list of servers to be scanned.
     * 
     * @param hostFile the host file path
     */
    public void setHostFile(String hostFile) {
        this.hostFile = hostFile;
    }

    /**
     * Gets the path to the file containing IP ranges or domains that should not be scanned.
     * 
     * @return the denylist file path, or null if not specified
     */
    public String getDenylistFile() {
        return denylistFile;
    }

    /**
     * Checks if the scan progress should be monitored and logged.
     * 
     * @return true if monitoring is enabled, false otherwise
     */
    public boolean isMonitored() {
        return monitored;
    }

    /**
     * Gets the URL to send HTTP POST request with BulkScan JSON after scan completion.
     * 
     * @return the notification URL, or null if not specified
     */
    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * Gets the number of top hosts from the Tranco list to scan.
     * 
     * @return the number of hosts (0 if not using Tranco list)
     */
    public int getTranco() {
        return tranco;
    }

    /**
     * Gets the number of top hosts from the CrUX list to scan.
     * 
     * @return the CrUX list number enum, or null if not using CrUX list
     */
    public CruxListNumber getCrux() {
        return crux;
    }

    /**
     * Gets the number of top hosts for MX record scanning from Tranco list.
     * 
     * @return the number of hosts (0 if not scanning email servers)
     */
    public int getTrancoEmail() {
        return trancoEmail;
    }

    /**
     * Creates and returns the appropriate target list provider based on configuration.
     * 
     * @return a target list provider for host file, Tranco email, CrUX, or regular Tranco list
     */
    public ITargetListProvider getTargetListProvider() {
        if (getHostFile() != null) {
            return new TargetFileProvider(getHostFile());
        }
        if (getTrancoEmail() != 0) {
            return new TrancoEmailListProvider(new TrancoListProvider(getTrancoEmail()));
        }
        if (getCrux() != null) {
            return new CruxListProvider(getCrux());
        }
        return new TrancoListProvider(getTranco());
    }

    /**
     * Gets the scan configuration for this controller.
     * 
     * @return the scan configuration
     */
    public abstract ScanConfig getScanConfig();

    /**
     * Creates a new BulkScan instance with current configuration.
     * 
     * @return a new BulkScan configured with current settings
     */
    public BulkScan createBulkScan() {
        return new BulkScan(
                getScannerClassForVersion(),
                getCrawlerClassForVersion(),
                getScanName(),
                getScanConfig(),
                System.currentTimeMillis(),
                isMonitored(),
                getNotifyUrl());
    }

    /**
     * Gets the crawler class for version tracking.
     * 
     * @return the class of this controller
     */
    public Class<?> getCrawlerClassForVersion() {
        return this.getClass();
    }

    /**
     * Gets the scanner class for version tracking.
     * 
     * @return the scanner class
     */
    public abstract Class<?> getScannerClassForVersion();

    /**
     * Sets the scanner detail level for the scan.
     * 
     * @param scanDetail the scanner detail level
     */
    public void setScanDetail(ScannerDetail scanDetail) {
        this.scanDetail = scanDetail;
    }

    /**
     * Sets the timeout value for the TLS-Scanner.
     * 
     * @param scannerTimeout the timeout in milliseconds
     */
    public void setScannerTimeout(int scannerTimeout) {
        this.scannerTimeout = scannerTimeout;
    }

    /**
     * Sets the number of reexecutions to use in the TLS-Scanner.
     * 
     * @param reexecutions the number of reexecutions
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the cron expression defining when scans are started.
     * 
     * @param scanCronInterval the cron expression
     */
    public void setScanCronInterval(String scanCronInterval) {
        this.scanCronInterval = scanCronInterval;
    }

    /**
     * Sets the name of the scan.
     * 
     * @param scanName the scan name
     */
    public void setScanName(String scanName) {
        this.scanName = scanName;
    }

    /**
     * Sets the path to the file containing IP ranges or domains that should not be scanned.
     * 
     * @param denylistFile the denylist file path
     */
    public void setDenylistFile(String denylistFile) {
        this.denylistFile = denylistFile;
    }

    /**
     * Sets whether the scan progress should be monitored and logged.
     * 
     * @param monitored true to enable monitoring, false otherwise
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets the URL to send HTTP POST request with BulkScan JSON after scan completion.
     * 
     * @param notifyUrl the notification URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the number of top hosts from the Tranco list to scan.
     * 
     * @param tranco the number of hosts
     */
    public void setTranco(int tranco) {
        this.tranco = tranco;
    }

    /**
     * Sets the number of top hosts from the CrUX list to scan.
     * 
     * @param crux the CrUX list number enum
     */
    public void setCrux(CruxListNumber crux) {
        this.crux = crux;
    }

    /**
     * Sets the number of top hosts for MX record scanning from Tranco list.
     * 
     * @param trancoEmail the number of hosts
     */
    public void setTrancoEmail(int trancoEmail) {
        this.trancoEmail = trancoEmail;
    }
}
