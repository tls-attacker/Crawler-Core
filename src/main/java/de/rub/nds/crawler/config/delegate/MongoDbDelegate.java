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
 * Configuration delegate for MongoDB connection parameters.
 *
 * <p>This class encapsulates all MongoDB connection settings including host, port, authentication
 * credentials, and authentication source database.
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
     * @return the hostname or IP address of the MongoDB server
     */
    public String getMongoDbHost() {
        return mongoDbHost;
    }

    /**
     * Gets the MongoDB port number.
     *
     * @return the port number on which MongoDB is listening
     */
    public int getMongoDbPort() {
        return mongoDbPort;
    }

    /**
     * Gets the MongoDB username.
     *
     * @return the username for MongoDB authentication
     */
    public String getMongoDbUser() {
        return mongoDbUser;
    }

    /**
     * Gets the MongoDB password.
     *
     * @return the password for MongoDB authentication
     */
    public String getMongoDbPass() {
        return mongoDbPass;
    }

    /**
     * Gets the path to the MongoDB password file.
     *
     * @return the file path containing the MongoDB password
     */
    public String getMongoDbPassFile() {
        return mongoDbPassFile;
    }

    /**
     * Gets the MongoDB authentication source database.
     *
     * @return the database name where user credentials are defined
     */
    public String getMongoDbAuthSource() {
        return mongoDbAuthSource;
    }

    /**
     * Sets the MongoDB host address.
     *
     * @param mongoDbHost the hostname or IP address of the MongoDB server
     */
    public void setMongoDbHost(String mongoDbHost) {
        this.mongoDbHost = mongoDbHost;
    }

    /**
     * Sets the MongoDB port number.
     *
     * @param mongoDbPort the port number on which MongoDB is listening
     */
    public void setMongoDbPort(int mongoDbPort) {
        this.mongoDbPort = mongoDbPort;
    }

    /**
     * Sets the MongoDB username.
     *
     * @param mongoDbUser the username for MongoDB authentication
     */
    public void setMongoDbUser(String mongoDbUser) {
        this.mongoDbUser = mongoDbUser;
    }

    /**
     * Sets the MongoDB password.
     *
     * @param mongoDbPass the password for MongoDB authentication
     */
    public void setMongoDbPass(String mongoDbPass) {
        this.mongoDbPass = mongoDbPass;
    }

    /**
     * Sets the path to the MongoDB password file.
     *
     * @param mongoDbPassFile the file path containing the MongoDB password
     */
    public void setMongoDbPassFile(String mongoDbPassFile) {
        this.mongoDbPassFile = mongoDbPassFile;
    }

    /**
     * Sets the MongoDB authentication source database.
     *
     * @param mongoDbAuthSource the database name where user credentials are defined
     */
    public void setMongoDbAuthSource(String mongoDbAuthSource) {
        this.mongoDbAuthSource = mongoDbAuthSource;
    }
}
