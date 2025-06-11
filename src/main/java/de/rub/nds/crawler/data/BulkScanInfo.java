/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import java.io.Serializable;

/**
 * Immutable metadata container for bulk scan information distributed to worker instances.
 *
 * <p>The BulkScanInfo class serves as a lightweight, serializable representation of essential bulk
 * scan metadata that workers need to execute individual scan jobs correctly. It contains only the
 * core information required for job execution while avoiding the overhead of transmitting the
 * complete BulkScan object to every worker.
 *
 * <p>Key design principles:
 *
 * <ul>
 *   <li><strong>Immutability</strong> - All fields are final and cannot be modified after creation
 *   <li><strong>Serialization Efficiency</strong> - Lightweight alternative to full BulkScan
 *       objects
 *   <li><strong>Essential Data Only</strong> - Contains only the minimum information needed by
 *       workers
 *   <li><strong>Type Safety</strong> - Provides typed access to scanner-specific configurations
 * </ul>
 *
 * <p><strong>Contained Information:</strong>
 *
 * <ul>
 *   <li><strong>Bulk Scan ID</strong> - Unique identifier for traceability and result correlation
 *   <li><strong>Scan Configuration</strong> - Scanner-specific settings and parameters
 *   <li><strong>Monitoring Flag</strong> - Whether progress monitoring is enabled for this scan
 * </ul>
 *
 * <p><strong>Lifecycle and Usage:</strong>
 *
 * <ul>
 *   <li><strong>Creation</strong> - Extracted from BulkScan objects by controllers
 *   <li><strong>Distribution</strong> - Serialized and included in ScanJobDescription messages
 *   <li><strong>Worker Usage</strong> - Used by workers to configure scan execution
 *   <li><strong>Result Correlation</strong> - Links individual results back to bulk scan
 * </ul>
 *
 * <p><strong>Immutability Guarantee:</strong> The class is designed to remain unchanged for the
 * entire duration of a bulk scan operation, ensuring consistent configuration across all
 * distributed workers and preventing configuration drift during long-running scans.
 *
 * <p><strong>Serialization:</strong> Implements Serializable for efficient transmission via message
 * queues between controller and worker instances in the distributed architecture.
 *
 * @see BulkScan
 * @see ScanConfig
 * @see ScanJobDescription
 */
public class BulkScanInfo implements Serializable {
    /** Unique identifier for the bulk scan operation. */
    private final String bulkScanId;

    /** Configuration settings for individual scan jobs within this bulk operation. */
    private final ScanConfig scanConfig;

    /** Flag indicating whether this bulk scan should be monitored for progress tracking. */
    private final boolean isMonitored;

    /**
     * Creates a new bulk scan info object by extracting essential metadata from a bulk scan.
     *
     * <p>This constructor extracts only the core information needed by workers for scan execution,
     * creating a lightweight representation that can be efficiently serialized and distributed via
     * message queues.
     *
     * <p><strong>Extracted Information:</strong>
     *
     * <ul>
     *   <li><strong>Bulk Scan ID</strong> - For result correlation and traceability
     *   <li><strong>Scan Configuration</strong> - Scanner settings and parameters
     *   <li><strong>Monitoring Status</strong> - Whether progress tracking is enabled
     * </ul>
     *
     * @param bulkScan the source bulk scan to extract metadata from
     */
    public BulkScanInfo(BulkScan bulkScan) {
        this.bulkScanId = bulkScan.get_id();
        this.scanConfig = bulkScan.getScanConfig();
        this.isMonitored = bulkScan.isMonitored();
    }

    /**
     * Gets the unique identifier of the bulk scan this metadata represents.
     *
     * <p>This ID is used for correlating individual scan job results back to their originating bulk
     * scan operation and for progress tracking.
     *
     * @return the bulk scan unique identifier
     */
    public String getBulkScanId() {
        return bulkScanId;
    }

    /**
     * Gets the scan configuration for this bulk scan operation.
     *
     * <p>The scan configuration contains scanner-specific settings and parameters that control how
     * individual scan jobs should be executed.
     *
     * @return the scan configuration object
     */
    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    /**
     * Gets the scan configuration cast to a specific scanner implementation type.
     *
     * <p>This method provides type-safe access to scanner-specific configuration implementations,
     * allowing workers to access configuration details specific to their scanner type without
     * manual casting.
     *
     * <p><strong>Usage Example:</strong>
     *
     * <pre>
     * TlsServerScanConfig tlsConfig = info.getScanConfig(TlsServerScanConfig.class);
     * </pre>
     *
     * @param <T> the specific scan configuration type
     * @param clazz the class object of the desired configuration type
     * @return the scan configuration cast to the specified type
     * @throws ClassCastException if the configuration is not of the specified type
     */
    public <T extends ScanConfig> T getScanConfig(Class<T> clazz) {
        return clazz.cast(scanConfig);
    }

    /**
     * Checks if progress monitoring is enabled for this bulk scan.
     *
     * <p>When monitoring is enabled, workers send completion notifications that are used for
     * progress tracking, performance metrics, and completion callbacks.
     *
     * @return true if progress monitoring is enabled, false otherwise
     */
    public boolean isMonitored() {
        return isMonitored;
    }
}
