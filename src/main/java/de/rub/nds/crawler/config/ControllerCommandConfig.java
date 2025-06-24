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

    public ControllerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    /**
     * Validates the configuration parameters.
     *
     * @throws ParameterException if the configuration is invalid
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
         * @throws ParameterException if the value is not a positive integer
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
         * @param value the parameter value to validate
         * @throws ParameterException if the value is not a valid cron expression
         */
        public void validate(String name, String value) throws ParameterException {
            CronScheduleBuilder.cronSchedule(value);
        }
    }

    /**
     * Gets the RabbitMQ delegate configuration.
     *
     * @return the RabbitMQ delegate
     */
    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    /**
     * Gets the MongoDB delegate configuration.
     *
     * @return the MongoDB delegate
     */
    public MongoDbDelegate getMongoDbDelegate() {
        return mongoDbDelegate;
    }

    /**
     * Gets the port to be scanned.
     *
     * @return the port number
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
     * Gets the scanner detail level.
     *
     * @return the scanner detail level
     */
    public ScannerDetail getScanDetail() {
        return scanDetail;
    }

    /**
     * Gets the scanner timeout value.
     *
     * @return the timeout value in milliseconds
     */
    public int getScannerTimeout() {
        return scannerTimeout;
    }

    /**
     * Gets the number of reexecutions.
     *
     * @return the number of reexecutions
     */
    public int getReexecutions() {
        return reexecutions;
    }

    /**
     * Gets the cron expression for scan intervals.
     *
     * @return the cron expression or null if not set
     */
    public String getScanCronInterval() {
        return scanCronInterval;
    }

    /**
     * Gets the scan name.
     *
     * @return the scan name
     */
    public String getScanName() {
        return scanName;
    }

    /**
     * Gets the path to the host file.
     *
     * @return the host file path
     */
    public String getHostFile() {
        return hostFile;
    }

    /**
     * Sets the path to the host file.
     *
     * @param hostFile the host file path
     */
    public void setHostFile(String hostFile) {
        this.hostFile = hostFile;
    }

    /**
     * Gets the path to the denylist file.
     *
     * @return the denylist file path
     */
    public String getDenylistFile() {
        return denylistFile;
    }

    /**
     * Checks if the scan should be monitored.
     *
     * @return true if monitoring is enabled, false otherwise
     */
    public boolean isMonitored() {
        return monitored;
    }

    /**
     * Gets the notification URL.
     *
     * @return the notification URL
     */
    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * Gets the number of top Tranco hosts to scan.
     *
     * @return the number of Tranco hosts
     */
    public int getTranco() {
        return tranco;
    }

    /**
     * Gets the CrUX list number.
     *
     * @return the CrUX list number
     */
    public CruxListNumber getCrux() {
        return crux;
    }

    /**
     * Gets the number of top Tranco email hosts to scan.
     *
     * @return the number of Tranco email hosts
     */
    public int getTrancoEmail() {
        return trancoEmail;
    }

    /**
     * Gets the appropriate target list provider based on configuration.
     *
     * @return the target list provider
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
     * Gets the scan configuration.
     *
     * @return the scan configuration
     */
    public abstract ScanConfig getScanConfig();

    /**
     * Creates a new bulk scan object.
     *
     * @return the created bulk scan
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
     * Gets the crawler class for versioning.
     *
     * @return the crawler class
     */
    public Class<?> getCrawlerClassForVersion() {
        return this.getClass();
    }

    /**
     * Gets the scanner class for versioning.
     *
     * @return the scanner class
     */
    public abstract Class<?> getScannerClassForVersion();

    /**
     * Sets the scanner detail level.
     *
     * @param scanDetail the scanner detail level
     */
    public void setScanDetail(ScannerDetail scanDetail) {
        this.scanDetail = scanDetail;
    }

    /**
     * Sets the scanner timeout value.
     *
     * @param scannerTimeout the timeout value in milliseconds
     */
    public void setScannerTimeout(int scannerTimeout) {
        this.scannerTimeout = scannerTimeout;
    }

    /**
     * Sets the number of reexecutions.
     *
     * @param reexecutions the number of reexecutions
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the cron expression for scan intervals.
     *
     * @param scanCronInterval the cron expression
     */
    public void setScanCronInterval(String scanCronInterval) {
        this.scanCronInterval = scanCronInterval;
    }

    /**
     * Sets the scan name.
     *
     * @param scanName the scan name
     */
    public void setScanName(String scanName) {
        this.scanName = scanName;
    }

    /**
     * Sets the path to the denylist file.
     *
     * @param denylistFile the denylist file path
     */
    public void setDenylistFile(String denylistFile) {
        this.denylistFile = denylistFile;
    }

    /**
     * Sets whether the scan should be monitored.
     *
     * @param monitored true to enable monitoring, false otherwise
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets the notification URL.
     *
     * @param notifyUrl the notification URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the number of top Tranco hosts to scan.
     *
     * @param tranco the number of Tranco hosts
     */
    public void setTranco(int tranco) {
        this.tranco = tranco;
    }

    /**
     * Sets the CrUX list number.
     *
     * @param crux the CrUX list number
     */
    public void setCrux(CruxListNumber crux) {
        this.crux = crux;
    }

    /**
     * Sets the number of top Tranco email hosts to scan.
     *
     * @param trancoEmail the number of Tranco email hosts
     */
    public void setTrancoEmail(int trancoEmail) {
        this.trancoEmail = trancoEmail;
    }
}
