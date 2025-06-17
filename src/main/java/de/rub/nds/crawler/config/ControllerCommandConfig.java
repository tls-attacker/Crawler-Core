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
 * Abstract base configuration class for TLS-Crawler controller command-line arguments.
 *
 * <p>This class defines the common configuration parameters needed by controller implementations to
 * orchestrate large-scale TLS scanning operations. It uses JCommander annotations for command-line
 * parsing and provides comprehensive validation of input parameters.
 *
 * <p>Key configuration areas:
 *
 * <ul>
 *   <li><strong>Connection Configuration</strong> - RabbitMQ and MongoDB connection settings
 *   <li><strong>Scan Parameters</strong> - Port, timeout, reexecutions, and detail level
 *   <li><strong>Target Selection</strong> - Host files, Tranco lists, Crux lists, email MX records
 *   <li><strong>Scheduling</strong> - Cron expressions for recurring scans
 *   <li><strong>Monitoring</strong> - Progress tracking and notification options
 *   <li><strong>Filtering</strong> - Denylist support for excluded targets
 * </ul>
 *
 * <p><strong>Target List Priority:</strong> When multiple target sources are specified, the
 * following priority is used:
 *
 * <ol>
 *   <li>Host file (if specified)
 *   <li>Tranco email list (MX records)
 *   <li>Crux list
 *   <li>Standard Tranco list
 * </ol>
 *
 * <p><strong>Validation Rules:</strong>
 *
 * <ul>
 *   <li>At least one target source must be specified
 *   <li>Notification URLs require monitoring to be enabled
 *   <li>Cron expressions must be valid Quartz syntax
 *   <li>Timeout and reexecution values must be positive
 * </ul>
 *
 * <p><strong>Extension Points:</strong> Subclasses must implement:
 *
 * <ul>
 *   <li>{@link #getScanConfig()} - Provide scanner-specific configuration
 *   <li>{@link #getScannerClassForVersion()} - Return scanner implementation class
 * </ul>
 *
 * @see RabbitMqDelegate
 * @see MongoDbDelegate
 * @see ITargetListProvider
 * @see BulkScan
 * @see ScanConfig
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
     * Creates a new controller command configuration with default delegate instances.
     *
     * <p>This constructor initializes the delegate objects that handle RabbitMQ and MongoDB
     * configuration parameters. The delegates use JCommander's @ParametersDelegate annotation to
     * include their parameters in the overall command-line parsing.
     *
     * <p><strong>Delegate Initialization:</strong>
     *
     * <ul>
     *   <li>RabbitMqDelegate - Handles message queue connection parameters
     *   <li>MongoDbDelegate - Handles database connection and storage parameters
     * </ul>
     */
    public ControllerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    /**
     * Validates the configuration parameters for consistency and completeness.
     *
     * <p>This method performs comprehensive validation of all configuration parameters to ensure
     * they form a valid and consistent configuration. It checks for required parameters, validates
     * dependencies between parameters, and verifies format requirements.
     *
     * <p><strong>Validation Rules:</strong>
     *
     * <ul>
     *   <li><strong>Target Source Required</strong> - At least one target source must be specified:
     *       hostFile, tranco, trancoEmail, or crux
     *   <li><strong>Monitoring Dependency</strong> - Notification URLs require monitoring to be
     *       enabled
     *   <li><strong>URL Validation</strong> - Notification URLs must be valid URIs
     * </ul>
     *
     * <p><strong>Parameter Dependencies:</strong>
     *
     * <ul>
     *   <li>notifyUrl parameter requires monitored=true
     *   <li>URL validation uses Apache Commons UrlValidator
     * </ul>
     *
     * @throws ParameterException if validation fails with descriptive error message
     * @see ParameterException
     * @see UrlValidator
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
     * JCommander parameter validator for positive integer values.
     *
     * <p>This validator ensures that integer parameters have positive values (>= 0). It is used for
     * timeout and reexecution parameters where negative values would be meaningless.
     *
     * <p><strong>Validation Logic:</strong>
     *
     * <ul>
     *   <li>Parses the string value as an integer
     *   <li>Rejects values less than 0
     *   <li>Provides descriptive error messages with parameter name and value
     * </ul>
     *
     * @see IParameterValidator
     */
    public static class PositiveInteger implements IParameterValidator {

        /** Creates a new positive integer validator. */
        public PositiveInteger() {
            // Default constructor for JCommander parameter validation
        }

        /**
         * Validates that the parameter value is a positive integer.
         *
         * @param name the parameter name for error reporting
         * @param value the string value to validate
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

    /**
     * JCommander parameter validator for Quartz cron expression syntax.
     *
     * <p>This validator ensures that cron expression parameters conform to valid Quartz cron
     * syntax. It is used for the scanCronInterval parameter to validate recurring scan schedules.
     *
     * <p><strong>Validation Method:</strong>
     *
     * <ul>
     *   <li>Uses Quartz CronScheduleBuilder to parse the expression
     *   <li>Throws ParameterException if parsing fails
     *   <li>Supports standard Quartz cron format (seconds, minutes, hours, day, month, weekday)
     * </ul>
     *
     * @see IParameterValidator
     * @see CronScheduleBuilder
     */
    public static class CronSyntax implements IParameterValidator {

        /** Creates a new cron syntax validator. */
        public CronSyntax() {
            // Default constructor for JCommander parameter validation
        }

        /**
         * Validates that the parameter value is a valid Quartz cron expression.
         *
         * @param name the parameter name for error reporting
         * @param value the cron expression string to validate
         * @throws ParameterException if the cron expression is invalid
         */
        public void validate(String name, String value) throws ParameterException {
            CronScheduleBuilder.cronSchedule(value);
        }
    }

    /**
     * Gets the RabbitMQ connection configuration delegate.
     *
     * @return the RabbitMQ configuration delegate
     */
    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    /**
     * Gets the MongoDB connection configuration delegate.
     *
     * @return the MongoDB configuration delegate
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
     * Gets the scanner detail level configuration.
     *
     * @return the scanner detail level
     */
    public ScannerDetail getScanDetail() {
        return scanDetail;
    }

    /**
     * Gets the scanner timeout value in milliseconds.
     *
     * @return the scanner timeout (default: 2000ms)
     */
    public int getScannerTimeout() {
        return scannerTimeout;
    }

    /**
     * Gets the number of reexecutions for failed scans.
     *
     * @return the reexecution count (default: 3)
     */
    public int getReexecutions() {
        return reexecutions;
    }

    /**
     * Gets the cron expression for recurring scans.
     *
     * @return the cron interval expression, or null for one-time execution
     */
    public String getScanCronInterval() {
        return scanCronInterval;
    }

    /**
     * Gets the human-readable name for this scan campaign.
     *
     * @return the scan name
     */
    public String getScanName() {
        return scanName;
    }

    /**
     * Gets the path to the host file containing scan targets.
     *
     * @return the host file path
     */
    public String getHostFile() {
        return hostFile;
    }

    /**
     * Sets the path to the host file containing scan targets.
     *
     * @param hostFile the host file path
     */
    public void setHostFile(String hostFile) {
        this.hostFile = hostFile;
    }

    /**
     * Gets the path to the denylist file for excluded targets.
     *
     * @return the denylist file path
     */
    public String getDenylistFile() {
        return denylistFile;
    }

    /**
     * Checks if scan progress monitoring is enabled.
     *
     * @return true if monitoring is enabled, false otherwise
     */
    public boolean isMonitored() {
        return monitored;
    }

    /**
     * Gets the notification URL for scan completion callbacks.
     *
     * @return the notification URL, or null if not configured
     */
    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * Gets the number of top Tranco list hosts to scan.
     *
     * @return the Tranco host count
     */
    public int getTranco() {
        return tranco;
    }

    /**
     * Gets the Crux list configuration for Chrome UX Report data.
     *
     * @return the Crux list number configuration
     */
    public CruxListNumber getCrux() {
        return crux;
    }

    /**
     * Gets the number of Tranco hosts for email MX record scanning.
     *
     * @return the Tranco email host count
     */
    public int getTrancoEmail() {
        return trancoEmail;
    }

    /**
     * Creates and returns the appropriate target list provider based on configuration.
     *
     * <p>This method implements the target source priority logic, selecting the appropriate
     * provider based on which parameters were specified. It provides a single point of target list
     * creation with consistent priority ordering.
     *
     * <p><strong>Priority Order:</strong>
     *
     * <ol>
     *   <li><strong>Host File</strong> - Direct file with target hosts (highest priority)
     *   <li><strong>Tranco Email</strong> - MX records from Tranco list entries
     *   <li><strong>Crux List</strong> - Google Chrome UX Report data
     *   <li><strong>Tranco List</strong> - Standard website popularity ranking (fallback)
     * </ol>
     *
     * <p><strong>Provider Types:</strong>
     *
     * <ul>
     *   <li>{@link TargetFileProvider} - Reads targets from a local file
     *   <li>{@link TrancoEmailListProvider} - Extracts MX records from Tranco data
     *   <li>{@link CruxListProvider} - Uses Chrome UX Report target lists
     *   <li>{@link TrancoListProvider} - Standard Tranco website ranking
     * </ul>
     *
     * @return the target list provider instance based on configuration priority
     * @see ITargetListProvider
     * @see TargetFileProvider
     * @see TrancoListProvider
     * @see CruxListProvider
     * @see TrancoEmailListProvider
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
     * Returns the scanner-specific configuration for this controller implementation.
     *
     * <p>This abstract method must be implemented by subclasses to provide the appropriate
     * ScanConfig instance for their specific scanner type. The scan configuration defines how
     * individual scan jobs should be executed.
     *
     * <p><strong>Implementation Requirements:</strong> Subclasses should create a ScanConfig that
     * includes:
     *
     * <ul>
     *   <li>Scanner implementation class
     *   <li>Scanner-specific parameters
     *   <li>Worker factory configuration
     *   <li>Any custom scan behavior settings
     * </ul>
     *
     * @return the scan configuration for this controller's scanner type
     * @see ScanConfig
     */
    public abstract ScanConfig getScanConfig();

    /**
     * Creates a new BulkScan instance using the current configuration parameters.
     *
     * <p>This factory method constructs a BulkScan object with all necessary metadata and
     * configuration for a scanning campaign. The BulkScan serves as the central coordination object
     * for the entire scanning operation.
     *
     * <p><strong>BulkScan Components:</strong>
     *
     * <ul>
     *   <li><strong>Scanner Class</strong> - The scanner implementation to use
     *   <li><strong>Crawler Class</strong> - The controller implementation class
     *   <li><strong>Scan Name</strong> - Human-readable identifier for the scan
     *   <li><strong>Scan Config</strong> - Scanner-specific configuration
     *   <li><strong>Timestamp</strong> - Creation time for tracking
     *   <li><strong>Monitoring</strong> - Whether progress tracking is enabled
     *   <li><strong>Notification URL</strong> - Optional completion notification endpoint
     * </ul>
     *
     * @return a new BulkScan instance configured with current parameters
     * @see BulkScan
     * @see #getScanConfig()
     * @see #getScannerClassForVersion()
     * @see #getCrawlerClassForVersion()
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
     * Returns the controller class for version tracking and compatibility.
     *
     * <p>This method provides the controller implementation class for tracking which version of the
     * crawler was used to create a bulk scan. This information is stored in the BulkScan metadata
     * for debugging and compatibility purposes.
     *
     * @return the concrete controller class that extends this configuration
     */
    public Class<?> getCrawlerClassForVersion() {
        return this.getClass();
    }

    /**
     * Returns the scanner implementation class for version tracking.
     *
     * <p>This abstract method must be implemented by subclasses to provide the specific scanner
     * class they use. This information is stored in BulkScan metadata for version tracking and
     * worker compatibility verification.
     *
     * <p><strong>Implementation Notes:</strong>
     *
     * <ul>
     *   <li>Should return the main scanner class (e.g., TlsServerScanner.class)
     *   <li>Used for version compatibility checks
     *   <li>Helps ensure workers use the correct scanner implementation
     * </ul>
     *
     * @return the scanner implementation class for this controller
     */
    public abstract Class<?> getScannerClassForVersion();

    /**
     * Sets the scanner detail level configuration.
     *
     * @param scanDetail the scanner detail level to use
     */
    public void setScanDetail(ScannerDetail scanDetail) {
        this.scanDetail = scanDetail;
    }

    /**
     * Sets the scanner timeout value in milliseconds.
     *
     * @param scannerTimeout the scanner timeout value
     */
    public void setScannerTimeout(int scannerTimeout) {
        this.scannerTimeout = scannerTimeout;
    }

    /**
     * Sets the number of reexecutions for failed scans.
     *
     * @param reexecutions the reexecution count
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the cron expression for recurring scans.
     *
     * @param scanCronInterval the cron interval expression
     */
    public void setScanCronInterval(String scanCronInterval) {
        this.scanCronInterval = scanCronInterval;
    }

    /**
     * Sets the human-readable name for this scan campaign.
     *
     * @param scanName the scan name
     */
    public void setScanName(String scanName) {
        this.scanName = scanName;
    }

    /**
     * Sets the path to the denylist file for excluded targets.
     *
     * @param denylistFile the denylist file path
     */
    public void setDenylistFile(String denylistFile) {
        this.denylistFile = denylistFile;
    }

    /**
     * Sets whether scan progress monitoring is enabled.
     *
     * @param monitored true to enable monitoring, false to disable
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    /**
     * Sets the notification URL for scan completion callbacks.
     *
     * @param notifyUrl the notification URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * Sets the number of top Tranco list hosts to scan.
     *
     * @param tranco the Tranco host count
     */
    public void setTranco(int tranco) {
        this.tranco = tranco;
    }

    /**
     * Sets the Crux list configuration for Chrome UX Report data.
     *
     * @param crux the Crux list number configuration
     */
    public void setCrux(CruxListNumber crux) {
        this.crux = crux;
    }

    /**
     * Sets the number of Tranco hosts for email MX record scanning.
     *
     * @param trancoEmail the Tranco email host count
     */
    public void setTrancoEmail(int trancoEmail) {
        this.trancoEmail = trancoEmail;
    }
}
