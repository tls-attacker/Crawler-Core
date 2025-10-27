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
 * Configuration delegate that holds MongoDB connection settings. Used by both controller and worker
 * configurations to avoid code duplication.
 */
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
     * Gets the MongoDB host address.
     *
     * @return The MongoDB host address
     */
    public String getMongoDbHost() {
        return mongoDbHost;
    }

    /**
     * Gets the MongoDB port number.
     *
     * @return The MongoDB port number
     */
    public int getMongoDbPort() {
        return mongoDbPort;
    }

    /**
     * Gets the MongoDB username for authentication.
     *
     * @return The MongoDB username
     */
    public String getMongoDbUser() {
        return mongoDbUser;
    }

    /**
     * Gets the MongoDB password for authentication.
     *
     * @return The MongoDB password
     */
    public String getMongoDbPass() {
        return mongoDbPass;
    }

    /**
     * Gets the file path containing the MongoDB password.
     *
     * @return The MongoDB password file path
     */
    public String getMongoDbPassFile() {
        return mongoDbPassFile;
    }

    /**
     * Gets the MongoDB authentication source database name.
     *
     * @return The authentication source database name
     */
    public String getMongoDbAuthSource() {
        return mongoDbAuthSource;
    }

    /**
     * Sets the MongoDB host address.
     *
     * @param mongoDbHost The MongoDB host address
     */
    public void setMongoDbHost(String mongoDbHost) {
        this.mongoDbHost = mongoDbHost;
    }

    /**
     * Sets the MongoDB port number.
     *
     * @param mongoDbPort The MongoDB port number
     */
    public void setMongoDbPort(int mongoDbPort) {
        this.mongoDbPort = mongoDbPort;
    }

    /**
     * Sets the MongoDB username for authentication.
     *
     * @param mongoDbUser The MongoDB username
     */
    public void setMongoDbUser(String mongoDbUser) {
        this.mongoDbUser = mongoDbUser;
    }

    /**
     * Sets the MongoDB password for authentication.
     *
     * @param mongoDbPass The MongoDB password
     */
    public void setMongoDbPass(String mongoDbPass) {
        this.mongoDbPass = mongoDbPass;
    }

    /**
     * Sets the file path containing the MongoDB password.
     *
     * @param mongoDbPassFile The MongoDB password file path
     */
    public void setMongoDbPassFile(String mongoDbPassFile) {
        this.mongoDbPassFile = mongoDbPassFile;
    }

    /**
     * Sets the MongoDB authentication source database name.
     *
     * @param mongoDbAuthSource The authentication source database name
     */
    public void setMongoDbAuthSource(String mongoDbAuthSource) {
        this.mongoDbAuthSource = mongoDbAuthSource;
    }
}
