/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.persistence;

import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;

/**
 * Persistence provider interface for database operations in the TLS-Crawler distributed
 * architecture.
 *
 * <p>The IPersistenceProvider defines the contract for storing and retrieving scan data throughout
 * the TLS-Crawler workflow. It abstracts the underlying storage implementation (MongoDB, file
 * system, etc.) and provides a consistent interface for controllers and workers to persist scan
 * metadata, results, and progress information.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Scan Result Storage</strong> - Persists individual scan results with metadata
 *   <li><strong>Bulk Scan Management</strong> - Handles bulk scan lifecycle (create, update)
 *   <li><strong>Data Consistency</strong> - Ensures reliable storage across distributed operations
 *   <li><strong>Storage Abstraction</strong> - Provides database-agnostic persistence interface
 * </ul>
 *
 * <p><strong>Implementation Requirements:</strong>
 *
 * <ul>
 *   <li><strong>Thread Safety</strong> - Must support concurrent access from multiple worker
 *       threads
 *   <li><strong>Error Handling</strong> - Should handle storage failures gracefully with
 *       appropriate exceptions
 *   <li><strong>ID Generation</strong> - Must assign unique IDs to BulkScan objects during
 *       insertion
 *   <li><strong>Data Integrity</strong> - Ensure scan results are correctly associated with their
 *       bulk scans
 * </ul>
 *
 * <p><strong>Storage Workflow:</strong>
 *
 * <ol>
 *   <li><strong>Bulk Scan Creation</strong> - Controller creates bulk scan with insertBulkScan()
 *   <li><strong>Job Processing</strong> - Workers store individual results with insertScanResult()
 *   <li><strong>Progress Updates</strong> - Controller updates bulk scan metadata with
 *       updateBulkScan()
 *   <li><strong>Completion</strong> - Final statistics and status updates via updateBulkScan()
 * </ol>
 *
 * <p><strong>Data Relationships:</strong>
 *
 * <ul>
 *   <li><strong>BulkScan</strong> - Parent container with metadata and aggregate statistics
 *   <li><strong>ScanResult</strong> - Individual scan outcomes linked to bulk scan via ID
 *   <li><strong>ScanJobDescription</strong> - Job metadata for result correlation and debugging
 * </ul>
 *
 * <p><strong>Common Implementations:</strong>
 *
 * <ul>
 *   <li><strong>MongoPersistenceProvider</strong> - MongoDB-based storage with JSON serialization
 *   <li><strong>File-based Providers</strong> - Local file system storage for development/testing
 *   <li><strong>API Providers</strong> - REST API integration for external systems
 * </ul>
 *
 * @see BulkScan
 * @see ScanResult
 * @see ScanJobDescription
 * @see MongoPersistenceProvider
 */
public interface IPersistenceProvider {

    /**
     * Persists a scan result and its associated job metadata to the database.
     *
     * <p>This method stores the complete outcome of a scan job execution, including the scan
     * findings, execution status, and metadata for traceability. The implementation must ensure the
     * result is correctly linked to its parent bulk scan.
     *
     * <p><strong>Storage Requirements:</strong>
     *
     * <ul>
     *   <li><strong>Result Data</strong> - Store the complete scan result document
     *   <li><strong>Job Metadata</strong> - Include job description for debugging and audit
     *   <li><strong>Bulk Scan Link</strong> - Maintain relationship to parent bulk scan
     *   <li><strong>Timestamp</strong> - Record insertion time for analysis
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method must be thread-safe as it will be called
     * concurrently by multiple worker threads processing scan jobs.
     *
     * @param scanResult the scan result containing findings and execution status
     * @param job the job description containing metadata and configuration details
     * @throws RuntimeException if the result cannot be persisted (implementation-specific)
     */
    void insertScanResult(ScanResult scanResult, ScanJobDescription job);

    /**
     * Creates a new bulk scan record in the database and assigns a unique identifier.
     *
     * <p>This method initializes a bulk scan operation by persisting its configuration and metadata
     * to the database. The implementation must generate and assign a unique ID to the bulk scan
     * object, which will be used to correlate individual scan results.
     *
     * <p><strong>Initialization Responsibilities:</strong>
     *
     * <ul>
     *   <li><strong>ID Assignment</strong> - Generate and set unique bulk scan identifier
     *   <li><strong>Metadata Storage</strong> - Persist scan configuration and parameters
     *   <li><strong>Timestamp Recording</strong> - Set creation timestamp for tracking
     *   <li><strong>Initial Status</strong> - Establish starting state for monitoring
     * </ul>
     *
     * <p><strong>ID Generation:</strong> The implementation must ensure the generated ID is unique
     * across all bulk scans and suitable for use as a foreign key reference in scan result records.
     *
     * @param bulkScan the bulk scan object to persist (ID will be assigned)
     * @throws RuntimeException if the bulk scan cannot be created (implementation-specific)
     */
    void insertBulkScan(BulkScan bulkScan);

    /**
     * Updates an existing bulk scan record with current progress and statistics.
     *
     * <p>This method replaces the existing bulk scan record with updated information, typically
     * called to record progress updates, final statistics, or completion status. The bulk scan ID
     * must remain unchanged during updates.
     *
     * <p><strong>Update Scenarios:</strong>
     *
     * <ul>
     *   <li><strong>Progress Updates</strong> - Job submission counts and statistics
     *   <li><strong>Status Changes</strong> - Monitoring state and completion flags
     *   <li><strong>Final Statistics</strong> - Success/error counts and performance metrics
     *   <li><strong>Completion</strong> - End timestamp and notification status
     * </ul>
     *
     * <p><strong>Consistency Requirements:</strong> The implementation should ensure that updates
     * are atomic and maintain data consistency, especially when called concurrently with scan
     * result insertions.
     *
     * @param bulkScan the bulk scan object with updated information
     * @throws RuntimeException if the bulk scan cannot be updated (implementation-specific)
     */
    void updateBulkScan(BulkScan bulkScan);
}
