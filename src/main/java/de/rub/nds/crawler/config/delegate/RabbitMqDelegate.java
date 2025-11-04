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

/** Configuration delegate that holds RabbitMQ connection settings. */
public class RabbitMqDelegate {

    @Parameter(names = "-rabbitMqHost", description = "Host of the RabbitMQ instance")
    private String rabbitMqHost;

    @Parameter(names = "-rabbitMqPort", description = "Port of the RabbitMQ instance")
    private int rabbitMqPort;

    @Parameter(names = "-rabbitMqUser", description = "Username for RabbitMQ authentication")
    private String rabbitMqUser;

    @Parameter(
            names = "-rabbitMqPass",
            description =
                    "Password for RabbitMQ authentication. Alternatively use -rabbitMqPassFile")
    private String rabbitMqPass;

    @Parameter(
            names = "-rabbitMqPassFile",
            description =
                    "File containing the password for RabbitMQ authentication. Alternatively use -rabbitMqPass")
    private String rabbitMqPassFile;

    @Parameter(names = "-rabbitMqTLS", description = "Use TLS for the RabbitMQ connection")
    private boolean rabbitMqTLS;

    public String getRabbitMqHost() {
        return rabbitMqHost;
    }

    public int getRabbitMqPort() {
        return rabbitMqPort;
    }

    public String getRabbitMqUser() {
        return rabbitMqUser;
    }

    public String getRabbitMqPass() {
        return rabbitMqPass;
    }

    public String getRabbitMqPassFile() {
        return rabbitMqPassFile;
    }

    public boolean isRabbitMqTLS() {
        return rabbitMqTLS;
    }

    public void setRabbitMqHost(String rabbitMqHost) {
        this.rabbitMqHost = rabbitMqHost;
    }

    public void setRabbitMqPort(int rabbitMqPort) {
        this.rabbitMqPort = rabbitMqPort;
    }

    public void setRabbitMqUser(String rabbitMqUser) {
        this.rabbitMqUser = rabbitMqUser;
    }

    public void setRabbitMqPass(String rabbitMqPass) {
        this.rabbitMqPass = rabbitMqPass;
    }

    public void setRabbitMqPassFile(String rabbitMqPassFile) {
        this.rabbitMqPassFile = rabbitMqPassFile;
    }

    public void setRabbitMqTLS(boolean rabbitMqTLS) {
        this.rabbitMqTLS = rabbitMqTLS;
    }
}
