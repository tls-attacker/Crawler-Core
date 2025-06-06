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
