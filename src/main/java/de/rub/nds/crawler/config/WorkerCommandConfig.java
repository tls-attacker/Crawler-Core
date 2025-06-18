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
import de.rub.nds.crawler.orchestration.RabbitMqOrchestrationProvider;
import de.rub.nds.crawler.persistence.MongoPersistenceProvider;

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

    public RabbitMqOrchestrationProvider createRabbitMqOrchestrationProvider() {
        return new RabbitMqOrchestrationProvider(rabbitMqDelegate);
    }

    public MongoPersistenceProvider createMongoPersistenceProvider() {
        return new MongoPersistenceProvider(mongoDbDelegate);
    }

    public int getParallelScanThreads() {
        return parallelScanThreads;
    }

    public int getParallelConnectionThreads() {
        return parallelConnectionThreads;
    }

    public int getScanTimeout() {
        return scanTimeout;
    }

    public void setParallelScanThreads(int parallelScanThreads) {
        this.parallelScanThreads = parallelScanThreads;
    }

    public void setParallelConnectionThreads(int parallelConnectionThreads) {
        this.parallelConnectionThreads = parallelConnectionThreads;
    }

    public void setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
    }
}
