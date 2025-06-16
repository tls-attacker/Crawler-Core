/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.orchestration;

import de.rub.nds.crawler.data.ScanJobDescription;

/**
 * Functional interface for consuming scan jobs from the orchestration provider in distributed TLS
 * scanning.
 *
 * <p>The ScanJobConsumer defines the contract for worker instances to receive and process scan jobs
 * from the message queue system. It serves as the callback mechanism that enables asynchronous job
 * processing in the TLS-Crawler distributed architecture.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li><strong>Functional Interface</strong> - Single method interface suitable for lambda
 *       expressions
 *   <li><strong>Asynchronous Processing</strong> - Called by orchestration provider when jobs
 *       arrive
 *   <li><strong>Worker Integration</strong> - Typically implemented by Worker class instances
 *   <li><strong>Acknowledgment Responsibility</strong> - Must ensure job completion is acknowledged
 * </ul>
 *
 * <p><strong>Implementation Pattern:</strong>
 *
 * <ol>
 *   <li><strong>Job Reception</strong> - Receive ScanJobDescription from orchestration provider
 *   <li><strong>Processing</strong> - Execute the TLS scan based on job configuration
 *   <li><strong>Result Handling</strong> - Store results and handle any errors
 *   <li><strong>Acknowledgment</strong> - Notify orchestration provider of completion
 * </ol>
 *
 * <p><strong>Thread Safety:</strong> Implementations must be thread-safe as they may be called
 * concurrently by the orchestration provider's message handling threads.
 *
 * <p><strong>Error Handling:</strong> Implementations should handle all exceptions internally and
 * ensure proper acknowledgment even in error scenarios to prevent message redelivery issues.
 *
 * <p><strong>Typical Usage:</strong>
 *
 * <pre>{@code
 * // Lambda implementation
 * ScanJobConsumer consumer = jobDescription -> {
 *     // Process the scan job
 *     processJob(jobDescription);
 * };
 *
 * // Method reference
 * ScanJobConsumer consumer = this::handleScanJob;
 *
 * // Registration with orchestration provider
 * orchestrationProvider.registerScanJobConsumer(consumer, prefetchCount);
 * }</pre>
 *
 * @see ScanJobDescription
 * @see IOrchestrationProvider#registerScanJobConsumer(ScanJobConsumer, int) Typically implemented
 *     by Worker.handleScanJob(ScanJobDescription) method.
 */
@FunctionalInterface
public interface ScanJobConsumer {

    /**
     * Processes a scan job received from the orchestration provider.
     *
     * <p>This method is called asynchronously by the orchestration provider when a scan job becomes
     * available for processing. The implementation must handle the complete job lifecycle including
     * execution, result storage, and acknowledgment.
     *
     * <p><strong>Processing Responsibilities:</strong>
     *
     * <ul>
     *   <li><strong>Job Execution</strong> - Perform the TLS scan based on job configuration
     *   <li><strong>Result Storage</strong> - Persist scan results to the configured database
     *   <li><strong>Error Handling</strong> - Handle and categorize any processing errors
     *   <li><strong>Acknowledgment</strong> - Notify completion via orchestration provider
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method may be called concurrently from multiple
     * threads, so implementations must be thread-safe or handle synchronization appropriately.
     *
     * <p><strong>Exception Handling:</strong> Implementations should catch all exceptions
     * internally and not allow them to propagate, as uncaught exceptions may disrupt the message
     * queue processing loop.
     *
     * @param scanJobDescription the scan job to process, containing target and configuration
     *     details
     */
    void consumeScanJob(ScanJobDescription scanJobDescription);
}
