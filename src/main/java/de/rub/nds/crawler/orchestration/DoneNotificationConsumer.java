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
 * Functional interface for consuming scan job completion notifications in distributed TLS scanning.
 *
 * <p>The DoneNotificationConsumer defines the contract for controllers and monitoring systems to
 * receive notifications when scan jobs complete processing. It enables real-time progress tracking,
 * statistics collection, and completion event handling in the TLS-Crawler distributed architecture.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li><strong>Functional Interface</strong> - Single method interface suitable for lambda
 *       expressions
 *   <li><strong>Event-Driven</strong> - Called asynchronously when scan jobs complete
 *   <li><strong>Progress Monitoring</strong> - Primary mechanism for tracking bulk scan progress
 *   <li><strong>Statistics Collection</strong> - Enables real-time performance and completion
 *       metrics
 * </ul>
 *
 * <p><strong>Usage Scenarios:</strong>
 *
 * <ul>
 *   <li><strong>Progress Tracking</strong> - ProgressMonitor uses this to track scan completion
 *   <li><strong>Statistics Updates</strong> - Update completion counters and performance metrics
 *   <li><strong>ETA Calculation</strong> - Calculate estimated time to completion
 *   <li><strong>Completion Detection</strong> - Detect when bulk scans finish
 * </ul>
 *
 * <p><strong>Implementation Pattern:</strong>
 *
 * <ol>
 *   <li><strong>Notification Reception</strong> - Receive completion event from orchestration
 *       provider
 *   <li><strong>Status Processing</strong> - Extract and categorize job completion status
 *   <li><strong>Statistics Update</strong> - Update counters and performance metrics
 *   <li><strong>Progress Logging</strong> - Log progress information and ETAs
 * </ol>
 *
 * <p><strong>Thread Safety:</strong> Implementations must be thread-safe as they may be called
 * concurrently by multiple message handling threads from the orchestration provider.
 *
 * <p><strong>Consumer Tag Usage:</strong> The consumer tag parameter identifies the specific
 * message queue consumer that delivered the notification, useful for debugging and routing.
 *
 * <p><strong>Typical Usage:</strong>
 *
 * <pre>{@code
 * // Lambda implementation
 * DoneNotificationConsumer consumer = (tag, job) -> {
 *     updateProgress(job.getStatus());
 *     logCompletion(job);
 * };
 *
 * // Method reference
 * DoneNotificationConsumer consumer = this::handleCompletion;
 *
 * // Registration with orchestration provider
 * orchestrationProvider.registerDoneNotificationConsumer(bulkScan, consumer);
 * }</pre>
 *
 * @see ScanJobDescription
 * @see IOrchestrationProvider#registerDoneNotificationConsumer(de.rub.nds.crawler.data.BulkScan,
 *     DoneNotificationConsumer) Typically implemented by
 *     ProgressMonitor.BulkscanMonitor.consumeDoneNotification method.
 */
@FunctionalInterface
public interface DoneNotificationConsumer {

    /**
     * Processes a scan job completion notification from the orchestration provider.
     *
     * <p>This method is called asynchronously by the orchestration provider when a scan job
     * completes processing. The implementation should update progress tracking, statistics, and any
     * monitoring systems based on the completed job information.
     *
     * <p><strong>Processing Responsibilities:</strong>
     *
     * <ul>
     *   <li><strong>Status Tracking</strong> - Record job completion status (SUCCESS, ERROR, etc.)
     *   <li><strong>Progress Updates</strong> - Update completion counters and percentages
     *   <li><strong>Performance Metrics</strong> - Calculate timing and throughput statistics
     *   <li><strong>Completion Detection</strong> - Detect when bulk scan operations finish
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method may be called concurrently from multiple
     * threads, so implementations must handle synchronization appropriately.
     *
     * <p><strong>Exception Handling:</strong> Implementations should catch all exceptions
     * internally to prevent disruption of the notification delivery system.
     *
     * @param consumerTag the message queue consumer tag that delivered this notification
     * @param scanJobDescription the completed scan job with final status and metadata
     */
    void consumeDoneNotification(String consumerTag, ScanJobDescription scanJobDescription);
}
