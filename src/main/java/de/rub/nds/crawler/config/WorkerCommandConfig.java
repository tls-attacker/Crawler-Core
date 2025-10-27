/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;

/**
 * Configuration class for worker instances. Contains settings for the worker's behavior, including
 * thread counts and timeouts, as well as MongoDB and RabbitMQ connection settings.
 */
public class WorkerCommandConfig {

    @ParametersDelegate private final RabbitMqDelegate rabbitMqDelegate;

    @ParametersDelegate private final MongoDbDelegate mongoDbDelegate;

    @Parameter(
            names = "-numberOfThreads",
            description = "Number of scan threads. Each thread starts a scanner instance.")
    private int parallelScanThreads = Runtime.getRuntime().availableProcessors();

    @Parameter(
            names = "-parallelProbeThreads",
            description =
                    "Number of threads used for parallel connections. These are shared between the worker threads per bulk scan.")
    private int parallelConnectionThreads = 20;

    @Parameter(
            names = "-scanTimeout",
            description =
                    "Overall timeout for one scan in ms. (Default 14 minutes)"
                            + "Has to be lower than rabbitMq consumerAck timeout (default 15min) or else rabbitMq connection will be closed if scan takes longer."
                            + "After the timeout the worker tries to shutdown the scan but a shutdown can not be guaranteed due to the TLS-Scanner implementation.")
    private int scanTimeout = 840000;

    /** Creates a new worker command configuration with default delegate settings. */
    public WorkerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    /**
     * Gets the RabbitMQ connection delegate.
     *
     * @return The RabbitMQ connection settings
     */
    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    /**
     * Gets the MongoDB connection delegate.
     *
     * @return The MongoDB connection settings
     */
    public MongoDbDelegate getMongoDbDelegate() {
        return mongoDbDelegate;
    }

    /**
     * Gets the number of parallel scan threads to use.
     *
     * @return The number of scan threads
     */
    public int getParallelScanThreads() {
        return parallelScanThreads;
    }

    /**
     * Gets the number of parallel connection threads to use per scan.
     *
     * @return The number of connection threads
     */
    public int getParallelConnectionThreads() {
        return parallelConnectionThreads;
    }

    /**
     * Gets the timeout for individual scan operations in milliseconds.
     *
     * @return The scan timeout in milliseconds
     */
    public int getScanTimeout() {
        return scanTimeout;
    }

    /**
     * Sets the number of parallel scan threads to use.
     *
     * @param parallelScanThreads The number of scan threads
     */
    public void setParallelScanThreads(int parallelScanThreads) {
        this.parallelScanThreads = parallelScanThreads;
    }

    /**
     * Sets the number of parallel connection threads to use per scan.
     *
     * @param parallelConnectionThreads The number of connection threads
     */
    public void setParallelConnectionThreads(int parallelConnectionThreads) {
        this.parallelConnectionThreads = parallelConnectionThreads;
    }

    /**
     * Sets the timeout for individual scan operations in milliseconds.
     *
     * @param scanTimeout The scan timeout in milliseconds
     */
    public void setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
    }
}
