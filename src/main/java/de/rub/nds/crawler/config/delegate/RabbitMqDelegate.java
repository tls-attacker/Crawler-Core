/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config.delegate;

import com.beust.jcommander.Parameter;

/**
 * Configuration delegate for RabbitMQ message queue connection parameters in TLS-Crawler.
 *
 * <p>The RabbitMqDelegate encapsulates all RabbitMQ-specific configuration parameters used for
 * message queue connectivity in the TLS-Crawler distributed architecture. It provides connection
 * settings, authentication credentials, and security options for the messaging infrastructure that
 * coordinates work between controllers and workers.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Connection Configuration</strong> - Host, port, and protocol settings
 *   <li><strong>Authentication Support</strong> - Username/password and file-based credentials
 *   <li><strong>TLS Security</strong> - Optional TLS encryption for message transport
 *   <li><strong>Delegate Pattern</strong> - Reusable across controller and worker configurations
 * </ul>
 *
 * <p><strong>Authentication Methods:</strong>
 *
 * <ul>
 *   <li><strong>Direct Password</strong> - rabbitMqPass parameter for direct password specification
 *   <li><strong>Password File</strong> - rabbitMqPassFile parameter for secure credential storage
 *   <li><strong>Username</strong> - rabbitMqUser specifies the authentication username
 * </ul>
 *
 * <p><strong>Security Configuration:</strong>
 *
 * <ul>
 *   <li><strong>TLS Encryption</strong> - rabbitMqTLS enables encrypted communication
 *   <li><strong>Port Selection</strong> - Supports both standard (5672) and TLS (5671) ports
 *   <li><strong>Credential Protection</strong> - Password file option prevents command-line
 *       exposure
 * </ul>
 *
 * <p><strong>Usage Pattern:</strong> This delegate is embedded in both ControllerCommandConfig and
 * WorkerCommandConfig using JCommander's @ParametersDelegate annotation, ensuring consistent
 * RabbitMQ configuration across all distributed components.
 *
 * <p><strong>Distributed Architecture:</strong> RabbitMQ serves as the central coordination
 * mechanism in TLS-Crawler, handling scan job distribution, completion notifications, and progress
 * monitoring between controllers and multiple worker instances.
 *
 * <p><strong>Default Behavior:</strong> All parameters are optional and default to appropriate
 * values (null for strings, false for TLS, 0 for port), allowing for environment-specific
 * configuration or RabbitMQ default connection settings.
 *
 * <p>Used by ControllerCommandConfig and WorkerCommandConfig for message queue configuration.
 * Creates IOrchestrationProvider instances, typically RabbitMqOrchestrationProvider
 * implementations.
 */
public class RabbitMqDelegate {

    /** Creates a new RabbitMQ configuration delegate with default settings. */
    public RabbitMqDelegate() {
        // Default constructor for JCommander parameter injection
    }

    @Parameter(names = "-rabbitMqHost")
    private String rabbitMqHost;

    @Parameter(names = "-rabbitMqPort")
    private int rabbitMqPort;

    @Parameter(names = "-rabbitMqUser")
    private String rabbitMqUser;

    @Parameter(names = "-rabbitMqPass")
    private String rabbitMqPass;

    @Parameter(names = "-rabbitMqPassFile")
    private String rabbitMqPassFile;

    @Parameter(names = "-rabbitMqTLS")
    private boolean rabbitMqTLS;

    /**
     * Gets the RabbitMQ broker host address.
     *
     * @return the RabbitMQ hostname or IP address, or null if not configured
     */
    public String getRabbitMqHost() {
        return rabbitMqHost;
    }

    /**
     * Gets the RabbitMQ broker port number.
     *
     * @return the RabbitMQ port number, or 0 if not configured (uses RabbitMQ defaults: 5672 for
     *     plain, 5671 for TLS)
     */
    public int getRabbitMqPort() {
        return rabbitMqPort;
    }

    /**
     * Gets the RabbitMQ authentication username.
     *
     * @return the username for RabbitMQ authentication, or null if not configured
     */
    public String getRabbitMqUser() {
        return rabbitMqUser;
    }

    /**
     * Gets the RabbitMQ authentication password.
     *
     * <p><strong>Security Note:</strong> Consider using rabbitMqPassFile for production deployments
     * to avoid exposing passwords in command-line arguments.
     *
     * @return the password for RabbitMQ authentication, or null if not configured
     */
    public String getRabbitMqPass() {
        return rabbitMqPass;
    }

    /**
     * Gets the path to the RabbitMQ password file.
     *
     * <p>This provides a more secure alternative to specifying passwords directly in command-line
     * arguments by reading the password from a file.
     *
     * @return the path to the password file, or null if not configured
     */
    public String getRabbitMqPassFile() {
        return rabbitMqPassFile;
    }

    /**
     * Checks if TLS encryption is enabled for RabbitMQ connections.
     *
     * <p>When TLS is enabled, all communication between the application and RabbitMQ broker is
     * encrypted. This typically requires connecting to port 5671 instead of the default port 5672.
     *
     * @return true if TLS is enabled, false otherwise
     */
    public boolean isRabbitMqTLS() {
        return rabbitMqTLS;
    }

    /**
     * Sets the RabbitMQ broker host address.
     *
     * @param rabbitMqHost the RabbitMQ hostname or IP address
     */
    public void setRabbitMqHost(String rabbitMqHost) {
        this.rabbitMqHost = rabbitMqHost;
    }

    /**
     * Sets the RabbitMQ broker port number.
     *
     * @param rabbitMqPort the RabbitMQ port number (typically 5672 for plain or 5671 for TLS)
     */
    public void setRabbitMqPort(int rabbitMqPort) {
        this.rabbitMqPort = rabbitMqPort;
    }

    /**
     * Sets the RabbitMQ authentication username.
     *
     * @param rabbitMqUser the username for RabbitMQ authentication
     */
    public void setRabbitMqUser(String rabbitMqUser) {
        this.rabbitMqUser = rabbitMqUser;
    }

    /**
     * Sets the RabbitMQ authentication password.
     *
     * @param rabbitMqPass the password for RabbitMQ authentication
     */
    public void setRabbitMqPass(String rabbitMqPass) {
        this.rabbitMqPass = rabbitMqPass;
    }

    /**
     * Sets the path to the RabbitMQ password file.
     *
     * @param rabbitMqPassFile the path to the file containing the RabbitMQ password
     */
    public void setRabbitMqPassFile(String rabbitMqPassFile) {
        this.rabbitMqPassFile = rabbitMqPassFile;
    }

    /**
     * Sets whether TLS encryption should be used for RabbitMQ connections.
     *
     * @param rabbitMqTLS true to enable TLS encryption, false for plain connections
     */
    public void setRabbitMqTLS(boolean rabbitMqTLS) {
        this.rabbitMqTLS = rabbitMqTLS;
    }
}
