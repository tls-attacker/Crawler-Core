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

    public WorkerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    /**
     * Gets the RabbitMQ delegate configuration.
     *
     * @return the RabbitMQ delegate
     */
    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    /**
     * Gets the MongoDB delegate configuration.
     *
     * @return the MongoDB delegate
     */
    public MongoDbDelegate getMongoDbDelegate() {
        return mongoDbDelegate;
    }

    /**
     * Gets the number of parallel scan threads.
     *
     * @return the number of parallel scan threads
     */
    public int getParallelScanThreads() {
        return parallelScanThreads;
    }

    /**
     * Gets the number of parallel connection threads.
     *
     * @return the number of parallel connection threads
     */
    public int getParallelConnectionThreads() {
        return parallelConnectionThreads;
    }

    /**
     * Gets the scan timeout value.
     *
     * @return the scan timeout in milliseconds
     */
    public int getScanTimeout() {
        return scanTimeout;
    }

    /**
     * Sets the number of parallel scan threads.
     *
     * @param parallelScanThreads the number of parallel scan threads
     */
    public void setParallelScanThreads(int parallelScanThreads) {
        this.parallelScanThreads = parallelScanThreads;
    }

    /**
     * Sets the number of parallel connection threads.
     *
     * @param parallelConnectionThreads the number of parallel connection threads
     */
    public void setParallelConnectionThreads(int parallelConnectionThreads) {
        this.parallelConnectionThreads = parallelConnectionThreads;
    }

    /**
     * Sets the scan timeout value.
     *
     * @param scanTimeout the scan timeout in milliseconds
     */
    public void setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
    }
}
