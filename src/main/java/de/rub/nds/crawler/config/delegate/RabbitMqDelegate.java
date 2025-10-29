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
 * Configuration delegate that holds RabbitMQ connection settings.
 */
public class RabbitMqDelegate {

    @Parameter(names = "-rabbitMqHost", description = "Host of the RabbitMQ instance")
    private String rabbitMqHost;

    @Parameter(names = "-rabbitMqPort", description = "Port of the RabbitMQ instance")
    private int rabbitMqPort;

    @Parameter(names = "-rabbitMqUser", description = "Username for RabbitMQ authentication")
    private String rabbitMqUser;

    @Parameter(names = "-rabbitMqPass", description = "Password for RabbitMQ authentication. Alternatively use -rabbitMqPassFile")
    private String rabbitMqPass;

    @Parameter(
            names = "-rabbitMqPassFile",
            description = "File containing the password for RabbitMQ authentication. Alternatively use -rabbitMqPass")
    private String rabbitMqPassFile;

    @Parameter(
            names = "-rabbitMqTLS",
            description = "Whether to use TLS for the RabbitMQ connection")
    private boolean rabbitMqTLS;

    /**
     * Gets the RabbitMQ host address.
     *
     * @return The RabbitMQ host address
     */
    public String getRabbitMqHost() {
        return rabbitMqHost;
    }

    /**
     * Gets the RabbitMQ port number.
     *
     * @return The RabbitMQ port number
     */
    public int getRabbitMqPort() {
        return rabbitMqPort;
    }

    /**
     * Gets the RabbitMQ username for authentication.
     *
     * @return The RabbitMQ username
     */
    public String getRabbitMqUser() {
        return rabbitMqUser;
    }

    /**
     * Gets the RabbitMQ password for authentication.
     *
     * @return The RabbitMQ password
     */
    public String getRabbitMqPass() {
        return rabbitMqPass;
    }

    /**
     * Gets the file path containing the RabbitMQ password.
     *
     * @return The RabbitMQ password file path
     */
    public String getRabbitMqPassFile() {
        return rabbitMqPassFile;
    }

    /**
     * Checks if TLS should be used for the RabbitMQ connection.
     *
     * @return True if TLS should be used, false otherwise
     */
    public boolean isRabbitMqTLS() {
        return rabbitMqTLS;
    }

    /**
     * Sets the RabbitMQ host address.
     *
     * @param rabbitMqHost The RabbitMQ host address
     */
    public void setRabbitMqHost(String rabbitMqHost) {
        this.rabbitMqHost = rabbitMqHost;
    }

    /**
     * Sets the RabbitMQ port number.
     *
     * @param rabbitMqPort The RabbitMQ port number
     */
    public void setRabbitMqPort(int rabbitMqPort) {
        this.rabbitMqPort = rabbitMqPort;
    }

    /**
     * Sets the RabbitMQ username for authentication.
     *
     * @param rabbitMqUser The RabbitMQ username
     */
    public void setRabbitMqUser(String rabbitMqUser) {
        this.rabbitMqUser = rabbitMqUser;
    }

    /**
     * Sets the RabbitMQ password for authentication.
     *
     * @param rabbitMqPass The RabbitMQ password
     */
    public void setRabbitMqPass(String rabbitMqPass) {
        this.rabbitMqPass = rabbitMqPass;
    }

    /**
     * Sets the file path containing the RabbitMQ password.
     *
     * @param rabbitMqPassFile The RabbitMQ password file path
     */
    public void setRabbitMqPassFile(String rabbitMqPassFile) {
        this.rabbitMqPassFile = rabbitMqPassFile;
    }

    /**
     * Sets whether TLS should be used for the RabbitMQ connection.
     *
     * @param rabbitMqTLS True if TLS should be used, false otherwise
     */
    public void setRabbitMqTLS(boolean rabbitMqTLS) {
        this.rabbitMqTLS = rabbitMqTLS;
    }
}
