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
 * Configuration class for TLS-Crawler worker command-line arguments and parameters.
 *
 * <p>This class defines the configuration parameters needed by worker instances to participate in
 * distributed TLS scanning operations. Workers consume scan jobs from the message queue, execute
 * TLS scans, and store results in the database. The configuration controls worker performance,
 * concurrency, and integration with the distributed infrastructure.
 *
 * <p>Key configuration areas:
 *
 * <ul>
 *   <li><strong>Connection Configuration</strong> - RabbitMQ and MongoDB connection settings
 *   <li><strong>Threading Configuration</strong> - Parallel scan and connection thread pools
 *   <li><strong>Timeout Management</strong> - Scan timeout and RabbitMQ coordination
 *   <li><strong>Performance Tuning</strong> - CPU utilization and throughput optimization
 * </ul>
 *
 * <p><strong>Threading Architecture:</strong>
 *
 * <ul>
 *   <li><strong>Scan Threads</strong> - Each thread runs a separate scanner instance for parallel
 *       execution
 *   <li><strong>Connection Threads</strong> - Shared pool for parallel network connections within
 *       scans
 *   <li><strong>Default Sizing</strong> - Scan threads default to CPU count, connections default to
 *       20
 * </ul>
 *
 * <p><strong>Timeout Coordination:</strong>
 *
 * <ul>
 *   <li>Scan timeout (14 min default) must be less than RabbitMQ consumer ACK timeout (15 min)
 *   <li>Prevents RabbitMQ connection closure due to unacknowledged messages
 *   <li>Worker attempts graceful scan shutdown on timeout (not guaranteed)
 *   <li>Timeout violations can lead to orphaned scan processes
 * </ul>
 *
 * <p><strong>Resource Management:</strong>
 *
 * <ul>
 *   <li>CPU-aware default thread count for optimal processor utilization
 *   <li>Connection pooling for efficient network resource usage
 *   <li>Timeout controls to prevent resource exhaustion
 * </ul>
 *
 * <p><strong>Infrastructure Integration:</strong> Uses delegate pattern for RabbitMQ and MongoDB
 * configuration to maintain separation of concerns and enable reuse across controller and worker
 * configurations.
 *
 * @see RabbitMqDelegate
 * @see MongoDbDelegate
 * @see ControllerCommandConfig
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

    /**
     * Creates a new worker command configuration with default delegate instances.
     *
     * <p>This constructor initializes the delegate objects that handle RabbitMQ and MongoDB
     * configuration parameters. The delegates use JCommander's @ParametersDelegate annotation to
     * include their parameters in the worker's command-line parsing.
     *
     * <p><strong>Delegate Initialization:</strong>
     *
     * <ul>
     *   <li>RabbitMqDelegate - Handles message queue connection and consumption parameters
     *   <li>MongoDbDelegate - Handles database connection and result storage parameters
     * </ul>
     *
     * <p><strong>Default Values:</strong>
     *
     * <ul>
     *   <li>Parallel scan threads - CPU count (Runtime.availableProcessors())
     *   <li>Parallel connection threads - 20
     *   <li>Scan timeout - 840,000 ms (14 minutes)
     * </ul>
     */
    public WorkerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    /**
     * Gets the RabbitMQ connection configuration delegate.
     *
     * @return the RabbitMQ configuration delegate for message queue operations
     */
    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    /**
     * Gets the MongoDB connection configuration delegate.
     *
     * @return the MongoDB configuration delegate for database operations
     */
    public MongoDbDelegate getMongoDbDelegate() {
        return mongoDbDelegate;
    }

    /**
     * Gets the number of parallel scan threads for concurrent scanner execution.
     *
     * <p>Each scan thread runs a separate TLS scanner instance, allowing the worker to process
     * multiple scan jobs simultaneously. The default value equals the number of available CPU cores
     * for optimal processor utilization.
     *
     * @return the number of parallel scan threads (default: CPU count)
     */
    public int getParallelScanThreads() {
        return parallelScanThreads;
    }

    /**
     * Gets the number of parallel connection threads for network operations.
     *
     * <p>These threads are shared across all scan threads within a bulk scan to handle concurrent
     * network connections efficiently. A higher count allows more simultaneous connections but
     * increases resource usage.
     *
     * @return the number of parallel connection threads (default: 20)
     */
    public int getParallelConnectionThreads() {
        return parallelConnectionThreads;
    }

    /**
     * Gets the overall timeout for individual scan operations.
     *
     * <p><strong>Critical Timing Constraint:</strong> This timeout must be lower than the RabbitMQ
     * consumer acknowledgment timeout (default 15 minutes) to prevent connection closure due to
     * unacknowledged messages.
     *
     * <p><strong>Timeout Behavior:</strong>
     *
     * <ul>
     *   <li>Worker attempts graceful scan shutdown when timeout is reached
     *   <li>Shutdown is not guaranteed due to TLS-Scanner implementation constraints
     *   <li>Exceeded timeouts may result in orphaned scan processes
     * </ul>
     *
     * @return the scan timeout in milliseconds (default: 840,000 ms / 14 minutes)
     */
    public int getScanTimeout() {
        return scanTimeout;
    }

    /**
     * Sets the number of parallel scan threads for concurrent scanner execution.
     *
     * <p>Configures how many TLS scanner instances can run simultaneously within this worker.
     * Higher values increase throughput but also CPU and memory usage.
     *
     * @param parallelScanThreads the number of parallel scan threads
     */
    public void setParallelScanThreads(int parallelScanThreads) {
        this.parallelScanThreads = parallelScanThreads;
    }

    /**
     * Sets the number of parallel connection threads for network operations.
     *
     * <p>Configures the shared thread pool size for concurrent network connections across all scan
     * operations. Balance between connection capacity and resource usage.
     *
     * @param parallelConnectionThreads the number of parallel connection threads
     */
    public void setParallelConnectionThreads(int parallelConnectionThreads) {
        this.parallelConnectionThreads = parallelConnectionThreads;
    }

    /**
     * Sets the overall timeout for individual scan operations.
     *
     * <p><strong>Important:</strong> Must be less than RabbitMQ consumer ACK timeout to prevent
     * message queue connection issues.
     *
     * @param scanTimeout the scan timeout in milliseconds
     */
    public void setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
    }
}
