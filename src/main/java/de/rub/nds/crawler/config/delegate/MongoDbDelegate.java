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

public class MongoDbDelegate {

    @Parameter(
            names = "-mongoDbHost",
            description = "Host of the MongoDB instance this crawler saves to.")
    private String mongoDbHost;

    @Parameter(
            names = "-mongoDbPort",
            description = "Port of the MongoDB instance this crawler saves to.")
    private int mongoDbPort;

    @Parameter(
            names = "-mongoDbUser",
            description = "The username to be used to authenticate with the MongoDB instance.")
    private String mongoDbUser;

    @Parameter(
            names = "-mongoDbPass",
            description = "The passwort to be used to authenticate with MongoDB.")
    private String mongoDbPass;

    @Parameter(
            names = "-mongoDbPassFile",
            description = "The passwort to be used to authenticate with MongoDB.")
    private String mongoDbPassFile;

    @Parameter(
            names = "-mongoDbAuthSource",
            description = "The DB within the MongoDB instance, in which the user:pass is defined.")
    private String mongoDbAuthSource;

    /**
     * Returns the MongoDB host.
     *
     * @return the MongoDB host
     */
    public String getMongoDbHost() {
        return mongoDbHost;
    }

    /**
     * Returns the MongoDB port.
     *
     * @return the MongoDB port
     */
    public int getMongoDbPort() {
        return mongoDbPort;
    }

    /**
     * Returns the MongoDB user.
     *
     * @return the MongoDB user
     */
    public String getMongoDbUser() {
        return mongoDbUser;
    }

    /**
     * Returns the MongoDB password.
     *
     * @return the MongoDB password
     */
    public String getMongoDbPass() {
        return mongoDbPass;
    }

    /**
     * Returns the MongoDB password file path.
     *
     * @return the MongoDB password file path
     */
    public String getMongoDbPassFile() {
        return mongoDbPassFile;
    }

    /**
     * Returns the MongoDB authentication source database.
     *
     * @return the MongoDB authentication source database
     */
    public String getMongoDbAuthSource() {
        return mongoDbAuthSource;
    }

    /**
     * Sets the MongoDB host.
     *
     * @param mongoDbHost the MongoDB host to set
     */
    public void setMongoDbHost(String mongoDbHost) {
        this.mongoDbHost = mongoDbHost;
    }

    /**
     * Sets the MongoDB port.
     *
     * @param mongoDbPort the MongoDB port to set
     */
    public void setMongoDbPort(int mongoDbPort) {
        this.mongoDbPort = mongoDbPort;
    }

    /**
     * Sets the MongoDB user.
     *
     * @param mongoDbUser the MongoDB user to set
     */
    public void setMongoDbUser(String mongoDbUser) {
        this.mongoDbUser = mongoDbUser;
    }

    /**
     * Sets the MongoDB password.
     *
     * @param mongoDbPass the MongoDB password to set
     */
    public void setMongoDbPass(String mongoDbPass) {
        this.mongoDbPass = mongoDbPass;
    }

    /**
     * Sets the MongoDB password file path.
     *
     * @param mongoDbPassFile the MongoDB password file path to set
     */
    public void setMongoDbPassFile(String mongoDbPassFile) {
        this.mongoDbPassFile = mongoDbPassFile;
    }

    /**
     * Sets the MongoDB authentication source database.
     *
     * @param mongoDbAuthSource the MongoDB authentication source database to set
     */
    public void setMongoDbAuthSource(String mongoDbAuthSource) {
        this.mongoDbAuthSource = mongoDbAuthSource;
    }
}
