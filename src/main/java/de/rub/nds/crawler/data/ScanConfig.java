/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.Serializable;

/**
 * Abstract base configuration class for TLS scanner implementations in distributed scanning.
 *
 * <p>The ScanConfig class provides the foundation for scanner-specific configuration in the
 * TLS-Crawler distributed architecture. It defines common scanning parameters that apply across
 * different TLS scanner implementations while allowing concrete subclasses to add scanner-specific
 * configuration options.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Common Configuration</strong> - Provides scanner detail, timeout, and retry
 *       settings
 *   <li><strong>Worker Factory</strong> - Abstract factory method for creating scan workers
 *   <li><strong>Serialization</strong> - Supports JSON/BSON serialization for distributed messaging
 *   <li><strong>Type Safety</strong> - Generic typing ensures worker compatibility with
 *       configuration
 * </ul>
 *
 * <p><strong>Configuration Parameters:</strong>
 *
 * <ul>
 *   <li><strong>Scanner Detail</strong> - Controls depth and comprehensiveness of scanning
 *   <li><strong>Reexecutions</strong> - Number of retry attempts for failed scans
 *   <li><strong>Timeout</strong> - Maximum execution time per scan in milliseconds
 * </ul>
 *
 * <p><strong>Factory Pattern:</strong> The abstract createWorker() method implements the factory
 * pattern, allowing each scanner implementation to create appropriately configured worker instances
 * that match the scanner's requirements and capabilities.
 *
 * <p><strong>Serialization Support:</strong> The class implements Serializable and includes a
 * no-argument constructor for compatibility with serialization frameworks used in distributed
 * messaging and database persistence.
 *
 * <p><strong>Extension Points:</strong> Subclasses should:
 *
 * <ul>
 *   <li>Add scanner-specific configuration parameters
 *   <li>Implement the createWorker() method to return appropriate worker instances
 *   <li>Ensure proper serialization of additional fields
 *   <li>Maintain compatibility with the distributed architecture
 * </ul>
 *
 * <p><strong>Common Usage Pattern:</strong> Configuration instances are created by controllers,
 * serialized and distributed to workers via message queues, then used to create scanner-specific
 * worker instances that execute the actual TLS scans.
 *
 * @see BulkScanWorker
 * @see ScannerDetail
 * @see BulkScan
 */
public abstract class ScanConfig implements Serializable {

    /** Scanner implementation details and configuration parameters. */
    private ScannerDetail scannerDetail;

    /** Number of retry attempts for failed scan operations. */
    private int reexecutions;

    /** Maximum execution time in milliseconds for individual scan operations. */
    private int timeout;

    @SuppressWarnings("unused")
    private ScanConfig() {}

    /**
     * Creates a new scan configuration with the specified parameters.
     *
     * <p>This protected constructor is intended for use by subclasses to initialize the common
     * configuration parameters that apply to all scanner implementations.
     *
     * @param scannerDetail the scanner detail level controlling scan comprehensiveness
     * @param reexecutions the number of retry attempts for failed scans
     * @param timeout the maximum execution time per scan in milliseconds
     */
    protected ScanConfig(ScannerDetail scannerDetail, int reexecutions, int timeout) {
        this.scannerDetail = scannerDetail;
        this.reexecutions = reexecutions;
        this.timeout = timeout;
    }

    /**
     * Gets the scanner detail level configuration.
     *
     * <p>The scanner detail level controls how comprehensive the TLS scanning should be, affecting
     * factors like the number of probes executed, the depth of analysis, and the amount of data
     * collected.
     *
     * @return the scanner detail level
     */
    public ScannerDetail getScannerDetail() {
        return this.scannerDetail;
    }

    /**
     * Gets the number of reexecution attempts for failed scans.
     *
     * <p>When a scan fails due to network issues or other transient problems, the scanner will
     * retry the scan up to this many times before marking it as failed.
     *
     * @return the number of retry attempts (typically 3)
     */
    public int getReexecutions() {
        return this.reexecutions;
    }

    /**
     * Gets the timeout value for individual scan operations.
     *
     * <p>This timeout controls how long the scanner will wait for a single scan to complete before
     * considering it failed. The timeout applies to the TLS-Scanner execution, not the overall
     * worker timeout.
     *
     * @return the scan timeout in milliseconds (typically 2000ms)
     */
    public int getTimeout() {
        return this.timeout;
    }

    /**
     * Sets the scanner detail level configuration.
     *
     * @param scannerDetail the scanner detail level to use
     */
    public void setScannerDetail(ScannerDetail scannerDetail) {
        this.scannerDetail = scannerDetail;
    }

    /**
     * Sets the number of reexecution attempts for failed scans.
     *
     * @param reexecutions the number of retry attempts
     */
    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    /**
     * Sets the timeout value for individual scan operations.
     *
     * @param timeout the scan timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Factory method for creating scanner-specific worker instances.
     *
     * <p>This abstract method must be implemented by subclasses to create appropriate
     * BulkScanWorker instances that are compatible with their specific scanner implementation. The
     * worker will use this configuration to control scanning behavior.
     *
     * <p><strong>Worker Creation:</strong> The created worker should be properly configured with
     * the scanner implementation, threading parameters, and this configuration instance.
     *
     * <p><strong>Threading Parameters:</strong>
     *
     * <ul>
     *   <li><strong>Connection Threads</strong> - Shared pool for parallel network connections
     *   <li><strong>Scan Threads</strong> - Number of concurrent scanner instances
     * </ul>
     *
     * @param bulkScanID the ID of the bulk scan this worker belongs to
     * @param parallelConnectionThreads the number of threads for parallel connections
     * @param parallelScanThreads the number of parallel scanner instances
     * @return a new BulkScanWorker instance configured for this scanner type
     */
    public abstract BulkScanWorker<? extends ScanConfig> createWorker(
            String bulkScanID, int parallelConnectionThreads, int parallelScanThreads);
}
