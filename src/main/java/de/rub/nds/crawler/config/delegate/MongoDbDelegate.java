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
 * Configuration delegate for MongoDB database connection parameters in TLS-Crawler.
 *
 * <p>The MongoDbDelegate encapsulates all MongoDB-specific configuration parameters used for
 * database connectivity in the TLS-Crawler distributed architecture. It uses JCommander annotations
 * to provide command-line parameter parsing and supports both password-based and file-based
 * authentication methods.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Connection Configuration</strong> - Host, port, and database specification
 *   <li><strong>Authentication Support</strong> - Username/password and file-based credentials
 *   <li><strong>Security Options</strong> - Password file support for secure credential storage
 *   <li><strong>Delegate Pattern</strong> - Reusable across controller and worker configurations
 * </ul>
 *
 * <p><strong>Authentication Methods:</strong>
 *
 * <ul>
 *   <li><strong>Direct Password</strong> - mongoDbPass parameter for direct password specification
 *   <li><strong>Password File</strong> - mongoDbPassFile parameter for file-based password storage
 *   <li><strong>Auth Source</strong> - mongoDbAuthSource specifies the authentication database
 * </ul>
 *
 * <p><strong>Usage Pattern:</strong> This delegate is embedded in both ControllerCommandConfig and
 * WorkerCommandConfig using JCommander's @ParametersDelegate annotation, allowing the same MongoDB
 * configuration to be shared across all application components.
 *
 * <p><strong>Security Considerations:</strong>
 *
 * <ul>
 *   <li>Password file option prevents credentials from appearing in command-line history
 *   <li>Authentication source allows for centralized user management
 *   <li>Connection parameters support both local and remote MongoDB deployments
 * </ul>
 *
 * <p><strong>Default Behavior:</strong> All parameters are optional and default to null, allowing
 * for environment-specific configuration or default MongoDB connection settings.
 *
 * <p>Used by ControllerCommandConfig and WorkerCommandConfig for database configuration. Creates
 * IPersistenceProvider instances, typically MongoPersistenceProvider implementations.
 */
public class MongoDbDelegate {

    /** Creates a new MongoDB configuration delegate with default settings. */
    public MongoDbDelegate() {
        // Default constructor for JCommander parameter injection
    }

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
     * @return the MongoDB hostname or IP address, or null if not configured
     */
    public String getMongoDbHost() {
        return mongoDbHost;
    }

    /**
     * Gets the MongoDB port number.
     *
     * @return the MongoDB port number, or 0 if not configured (uses MongoDB default)
     */
    public int getMongoDbPort() {
        return mongoDbPort;
    }

    /**
     * Gets the MongoDB authentication username.
     *
     * @return the username for MongoDB authentication, or null if not configured
     */
    public String getMongoDbUser() {
        return mongoDbUser;
    }

    /**
     * Gets the MongoDB authentication password.
     *
     * <p><strong>Security Note:</strong> Consider using mongoDbPassFile for production deployments
     * to avoid exposing passwords in command-line arguments.
     *
     * @return the password for MongoDB authentication, or null if not configured
     */
    public String getMongoDbPass() {
        return mongoDbPass;
    }

    /**
     * Gets the path to the MongoDB password file.
     *
     * <p>This provides a more secure alternative to specifying passwords directly in command-line
     * arguments by reading the password from a file.
     *
     * @return the path to the password file, or null if not configured
     */
    public String getMongoDbPassFile() {
        return mongoDbPassFile;
    }

    /**
     * Gets the MongoDB authentication source database.
     *
     * <p>This specifies which database contains the user credentials for authentication. Commonly
     * set to "admin" for centralized user management.
     *
     * @return the authentication source database name, or null if not configured
     */
    public String getMongoDbAuthSource() {
        return mongoDbAuthSource;
    }

    /**
     * Sets the MongoDB host address.
     *
     * @param mongoDbHost the MongoDB hostname or IP address
     */
    public void setMongoDbHost(String mongoDbHost) {
        this.mongoDbHost = mongoDbHost;
    }

    /**
     * Sets the MongoDB port number.
     *
     * @param mongoDbPort the MongoDB port number (typically 27017)
     */
    public void setMongoDbPort(int mongoDbPort) {
        this.mongoDbPort = mongoDbPort;
    }

    /**
     * Sets the MongoDB authentication username.
     *
     * @param mongoDbUser the username for MongoDB authentication
     */
    public void setMongoDbUser(String mongoDbUser) {
        this.mongoDbUser = mongoDbUser;
    }

    /**
     * Sets the MongoDB authentication password.
     *
     * @param mongoDbPass the password for MongoDB authentication
     */
    public void setMongoDbPass(String mongoDbPass) {
        this.mongoDbPass = mongoDbPass;
    }

    /**
     * Sets the path to the MongoDB password file.
     *
     * @param mongoDbPassFile the path to the file containing the MongoDB password
     */
    public void setMongoDbPassFile(String mongoDbPassFile) {
        this.mongoDbPassFile = mongoDbPassFile;
    }

    /**
     * Sets the MongoDB authentication source database.
     *
     * @param mongoDbAuthSource the database name containing user credentials
     */
    public void setMongoDbAuthSource(String mongoDbAuthSource) {
        this.mongoDbAuthSource = mongoDbAuthSource;
    }
}
