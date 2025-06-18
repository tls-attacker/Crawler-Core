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
     * Gets the RabbitMQ host.
     *
     * @return the RabbitMQ host
     */
    public String getRabbitMqHost() {
        return rabbitMqHost;
    }

    /**
     * Gets the RabbitMQ port.
     *
     * @return the RabbitMQ port
     */
    public int getRabbitMqPort() {
        return rabbitMqPort;
    }

    /**
     * Gets the RabbitMQ username.
     *
     * @return the RabbitMQ username
     */
    public String getRabbitMqUser() {
        return rabbitMqUser;
    }

    /**
     * Gets the RabbitMQ password.
     *
     * @return the RabbitMQ password
     */
    public String getRabbitMqPass() {
        return rabbitMqPass;
    }

    /**
     * Gets the RabbitMQ password file path.
     *
     * @return the RabbitMQ password file path
     */
    public String getRabbitMqPassFile() {
        return rabbitMqPassFile;
    }

    /**
     * Checks if TLS is enabled for RabbitMQ connections.
     *
     * @return true if TLS is enabled, false otherwise
     */
    public boolean isRabbitMqTLS() {
        return rabbitMqTLS;
    }

    /**
     * Sets the RabbitMQ host.
     *
     * @param rabbitMqHost the RabbitMQ host to set
     */
    public void setRabbitMqHost(String rabbitMqHost) {
        this.rabbitMqHost = rabbitMqHost;
    }

    /**
     * Sets the RabbitMQ port.
     *
     * @param rabbitMqPort the RabbitMQ port to set
     */
    public void setRabbitMqPort(int rabbitMqPort) {
        this.rabbitMqPort = rabbitMqPort;
    }

    /**
     * Sets the RabbitMQ username.
     *
     * @param rabbitMqUser the RabbitMQ username to set
     */
    public void setRabbitMqUser(String rabbitMqUser) {
        this.rabbitMqUser = rabbitMqUser;
    }

    /**
     * Sets the RabbitMQ password.
     *
     * @param rabbitMqPass the RabbitMQ password to set
     */
    public void setRabbitMqPass(String rabbitMqPass) {
        this.rabbitMqPass = rabbitMqPass;
    }

    /**
     * Sets the RabbitMQ password file path.
     *
     * @param rabbitMqPassFile the RabbitMQ password file path to set
     */
    public void setRabbitMqPassFile(String rabbitMqPassFile) {
        this.rabbitMqPassFile = rabbitMqPassFile;
    }

    /**
     * Sets whether to use TLS for RabbitMQ connections.
     *
     * @param rabbitMqTLS true to enable TLS, false otherwise
     */
    public void setRabbitMqTLS(boolean rabbitMqTLS) {
        this.rabbitMqTLS = rabbitMqTLS;
    }
}
