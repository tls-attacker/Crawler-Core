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
 * Configuration delegate for RabbitMQ connection parameters.
 *
 * <p>This class encapsulates all RabbitMQ connection settings including host, port, authentication
 * credentials, and TLS configuration.
 */
public class RabbitMqDelegate {

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
     * Gets the RabbitMQ host address.
     *
     * @return the hostname or IP address of the RabbitMQ server
     */
    public String getRabbitMqHost() {
        return rabbitMqHost;
    }

    /**
     * Gets the RabbitMQ port number.
     *
     * @return the port number on which RabbitMQ is listening
     */
    public int getRabbitMqPort() {
        return rabbitMqPort;
    }

    /**
     * Gets the RabbitMQ username.
     *
     * @return the username for RabbitMQ authentication
     */
    public String getRabbitMqUser() {
        return rabbitMqUser;
    }

    /**
     * Gets the RabbitMQ password.
     *
     * @return the password for RabbitMQ authentication
     */
    public String getRabbitMqPass() {
        return rabbitMqPass;
    }

    /**
     * Gets the path to the RabbitMQ password file.
     *
     * @return the file path containing the RabbitMQ password
     */
    public String getRabbitMqPassFile() {
        return rabbitMqPassFile;
    }

    /**
     * Checks if TLS is enabled for RabbitMQ connections.
     *
     * @return true if TLS should be used for RabbitMQ connections, false otherwise
     */
    public boolean isRabbitMqTLS() {
        return rabbitMqTLS;
    }

    /**
     * Sets the RabbitMQ host address.
     *
     * @param rabbitMqHost the hostname or IP address of the RabbitMQ server
     */
    public void setRabbitMqHost(String rabbitMqHost) {
        this.rabbitMqHost = rabbitMqHost;
    }

    /**
     * Sets the RabbitMQ port number.
     *
     * @param rabbitMqPort the port number on which RabbitMQ is listening
     */
    public void setRabbitMqPort(int rabbitMqPort) {
        this.rabbitMqPort = rabbitMqPort;
    }

    /**
     * Sets the RabbitMQ username.
     *
     * @param rabbitMqUser the username for RabbitMQ authentication
     */
    public void setRabbitMqUser(String rabbitMqUser) {
        this.rabbitMqUser = rabbitMqUser;
    }

    /**
     * Sets the RabbitMQ password.
     *
     * @param rabbitMqPass the password for RabbitMQ authentication
     */
    public void setRabbitMqPass(String rabbitMqPass) {
        this.rabbitMqPass = rabbitMqPass;
    }

    /**
     * Sets the path to the RabbitMQ password file.
     *
     * @param rabbitMqPassFile the file path containing the RabbitMQ password
     */
    public void setRabbitMqPassFile(String rabbitMqPassFile) {
        this.rabbitMqPassFile = rabbitMqPassFile;
    }

    /**
     * Sets whether TLS should be used for RabbitMQ connections.
     *
     * @param rabbitMqTLS true to enable TLS, false to disable
     */
    public void setRabbitMqTLS(boolean rabbitMqTLS) {
        this.rabbitMqTLS = rabbitMqTLS;
    }
}
