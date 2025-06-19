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
 * Abstract base configuration class for the TLS-Crawler controller component. This class provides
 * common configuration options for controlling TLS scans, including target selection, scan
 * parameters, and monitoring settings. Concrete implementations must provide version-specific
 * scanner configurations.
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
     * Constructs a new ControllerCommandConfig with default RabbitMQ and MongoDB delegates.
     * Initializes the message queue and database connection configurations.
     */
    public ControllerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    /**
     * Validates the configuration parameters. Ensures that: - At least one target source is
     * specified (hostFile, tranco, trancoEmail, or crux) - Notification URL requires monitoring to
     * be enabled - Notification URL is valid if provided
     *
     * @throws ParameterException if validation fails
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

    /**
     * Parameter validator for positive integer values. Ensures that numeric parameters are
     * non-negative.
     */
    public static class PositiveInteger implements IParameterValidator {
        /**
         * Validates that the provided value is a non-negative integer.
         *
         * @param name the parameter name
         * @param value the parameter value to validate
         * @throws ParameterException if the value is negative or not a valid integer
         */
        public void validate(String name, String value) throws ParameterException {
            int n = Integer.parseInt(value);
            if (n < 0) {
                throw new ParameterException(
                        "Parameter " + name + " should be positive (found " + value + ")");
            }
        }
    }

    /**
     * Parameter validator for cron expression syntax. Ensures that cron expressions are valid for
     * scheduling.
     */
    public static class CronSyntax implements IParameterValidator {
        /**
         * Validates that the provided value is a valid cron expression.
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
     * Gets the RabbitMQ configuration delegate.
     *
     * @return the RabbitMQ delegate for message queue configuration
     */
    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    /**
     * Gets the MongoDB configuration delegate.
     *
     * @return the MongoDB delegate for database configuration
     */
    public MongoDbDelegate getMongoDbDelegate() {
        return mongoDbDelegate;
    }

    /**
     * Gets the port number to be scanned.
     *
     * @return the target port number (default: 443)
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number to be scanned.
     *
     * @param port the target port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Gets the level of detail for the TLS scan.
     *
     * @return the scanner detail level (default: NORMAL)
     */
    public ScannerDetail getScanDetail() {
        return scanDetail;
    }

    /**
     * Gets the timeout value for TLS-Scanner operations.
     *
     * @return the timeout in milliseconds (default: 2000)
     */
    public int getScannerTimeout() {
        return scannerTimeout;
    }

    /**
     * Gets the number of re-execution attempts for unreliable tests.
     *
     * @return the number of re-executions (default: 3)
     */
    public int getReexecutions() {
        return reexecutions;
    }

    /**
     * Gets the cron expression for scheduling periodic scans.
     *
     * @return the cron expression, or null for a single immediate scan
     */
    public String getScanCronInterval() {
        return scanCronInterval;
    }

    /**
     * Gets the name identifier for this scan.
     *
     * @return the scan name
     */
    public String getScanName() {
        return scanName;
    }

    /**
     * Gets the path to the file containing hosts to scan.
     *
     * @return the host file path, or null if using a different target source
     */
    public String getHostFile() {
        return hostFile;
    }

    /**
     * Sets the path to the file containing hosts to scan.
     *
     * @param hostFile the host file path
     */
    public void setHostFile(String hostFile) {
        this.hostFile = hostFile;
    }

    /**
     * Gets the path to the denylist file containing IP ranges or domains to exclude from scanning.
     *
     * @return the denylist file path, or null if no denylist is used
     */
    public String getDenylistFile() {
        return denylistFile;
    }

    /**
     * Checks if scan progress monitoring is enabled.
     *
     * @return true if scan progress is monitored and logged
     */
    public boolean isMonitored() {
        return monitored;
    }

    /**
     * Gets the URL for sending HTTP POST notifications when a scan completes.
     *
     * @return the notification URL, or null if notifications are disabled
     */
    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * Gets the number of top hosts from the Tranco list to scan.
     *
     * @return the number of Tranco hosts (0 if not using Tranco)
     */
    public int getTranco() {
        return tranco;
    }

    /**
     * Gets the number of top hosts from the CrUX (Chrome User Experience) list to scan.
     *
     * @return the CrUX list number, or null if not using CrUX
     */
    public CruxListNumber getCrux() {
        return crux;
    }

    /**
     * Gets the number of top hosts from the Tranco list for email server (MX record) scanning.
     *
     * @return the number of Tranco email hosts (0 if not scanning email servers)
     */
    public int getTrancoEmail() {
        return trancoEmail;
    }

    /**
     * Creates and returns the appropriate target list provider based on configuration. Priority
     * order: 1. Host file if specified 2. Tranco email list if count > 0 3. CrUX list if specified
     * 4. Tranco list (default)
     *
     * @return the target list provider for generating scan targets
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
     * Gets the scan configuration specific to the TLS-Scanner version. Must be implemented by
     * concrete subclasses to provide version-specific settings.
     *
     * @return the scan configuration
     */
    public abstract ScanConfig getScanConfig();

    /**
     * Creates a new BulkScan instance with the current configuration. The scan includes scanner and
     * crawler version information, timing, and monitoring settings.
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
     * Gets the crawler implementation class for version tracking. Returns the concrete
     * configuration class to identify the crawler version.
     *
     * @return the class of the concrete configuration implementation
     */
    public Class<?> getCrawlerClassForVersion() {
        return this.getClass();
    }

    /**
     * Gets the TLS-Scanner implementation class for version tracking. Must be implemented by
     * concrete subclasses to specify the scanner version.
     *
     * @return the TLS-Scanner class for the specific version
     */
    public abstract Class<?> getScannerClassForVersion();

    /**
     * Sets the level of detail for the TLS scan.
     *
     * @param scanDetail the scanner detail level
     */
    public void setScanDetail(ScannerDetail scanDetail) {
        this.scanDetail = scanDetail;
    }

    /**
     * Sets the timeout value for TLS-Scanner operations.
     *
     * @param scannerTimeout the timeout in milliseconds
     */
    public void setScannerTimeout(int scannerTimeout) {
        this.scannerTimeout = scannerTimeout;
    }

    /**
     * Sets the number of re-execution attempts for unreliable tests.
     *
     * @param reexecutions the number of re-executions
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the cron expression for scheduling periodic scans.
     *
     * @param scanCronInterval the cron expression
     */
    public void setScanCronInterval(String scanCronInterval) {
        this.scanCronInterval = scanCronInterval;
    }

    /**
     * Sets the name identifier for this scan.
     *
     * @param scanName the scan name
     */
    public void setScanName(String scanName) {
        this.scanName = scanName;
    }

    /**
     * Sets the path to the denylist file containing IP ranges or domains to exclude from scanning.
     *
     * @param denylistFile the denylist file path
     */
    public void setDenylistFile(String denylistFile) {
        this.denylistFile = denylistFile;
    }

    /**
     * Sets whether scan progress monitoring is enabled.
     *
     * @param monitored true to enable scan progress monitoring and logging
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets the URL for sending HTTP POST notifications when a scan completes.
     *
     * @param notifyUrl the notification URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the number of top hosts from the Tranco list to scan.
     *
     * @param tranco the number of Tranco hosts
     */
    public void setTranco(int tranco) {
        this.tranco = tranco;
    }

    /**
     * Sets the number of top hosts from the CrUX (Chrome User Experience) list to scan.
     *
     * @param crux the CrUX list number
     */
    public void setCrux(CruxListNumber crux) {
        this.crux = crux;
    }

    /**
     * Sets the number of top hosts from the Tranco list for email server (MX record) scanning.
     *
     * @param trancoEmail the number of Tranco email hosts
     */
    public void setTrancoEmail(int trancoEmail) {
        this.trancoEmail = trancoEmail;
    }
}
