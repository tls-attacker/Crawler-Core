/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.orchestration;

import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;

/**
 * Orchestration provider interface for distributed job coordination in TLS-Crawler.
 *
 * <p>The IOrchestrationProvider defines the contract for coordinating scan job distribution between
 * controllers and workers in the TLS-Crawler distributed architecture. It abstracts the underlying
 * message queue implementation (RabbitMQ, etc.) and provides a reliable communication mechanism for
 * job submission, consumption, and completion notifications.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Job Distribution</strong> - Delivers scan jobs from controllers to available
 *       workers
 *   <li><strong>Load Balancing</strong> - Distributes work across multiple worker instances
 *   <li><strong>Reliable Messaging</strong> - Ensures job delivery with acknowledgment mechanisms
 *   <li><strong>Progress Monitoring</strong> - Provides completion notifications for tracking
 *   <li><strong>Resource Management</strong> - Manages connections and cleanup for long-running
 *       operations
 * </ul>
 *
 * <p><strong>Message Flow Architecture:</strong>
 *
 * <ol>
 *   <li><strong>Job Submission</strong> - Controllers submit jobs via submitScanJob()
 *   <li><strong>Job Distribution</strong> - Provider routes jobs to registered consumers
 *   <li><strong>Job Processing</strong> - Workers receive jobs through registered consumers
 *   <li><strong>Completion Notification</strong> - Workers notify completion via
 *       notifyOfDoneScanJob()
 *   <li><strong>Progress Tracking</strong> - Completion events are forwarded to monitoring systems
 * </ol>
 *
 * <p><strong>Consumer Registration:</strong>
 *
 * <ul>
 *   <li><strong>Scan Job Consumers</strong> - Workers register to receive scan jobs
 *   <li><strong>Done Notification Consumers</strong> - Controllers register for completion events
 *   <li><strong>Prefetch Control</strong> - Configurable flow control for consumer capacity
 * </ul>
 *
 * <p><strong>Reliability Features:</strong>
 *
 * <ul>
 *   <li><strong>Acknowledgment</strong> - Jobs must be explicitly acknowledged after processing
 *   <li><strong>Delivery Guarantees</strong> - Ensures jobs are not lost during processing
 *   <li><strong>Error Handling</strong> - Supports requeue and retry mechanisms
 *   <li><strong>Connection Recovery</strong> - Resilient to network interruptions
 * </ul>
 *
 * <p><strong>Implementation Notes:</strong>
 *
 * <ul>
 *   <li><strong>Connection Management</strong> - Providers may establish connections in constructor
 *   <li><strong>Resource Cleanup</strong> - Must implement closeConnection() for proper cleanup
 *   <li><strong>Thread Safety</strong> - Should support concurrent access from multiple threads
 *   <li><strong>Configuration</strong> - Should support flexible connection and routing
 *       configuration
 * </ul>
 *
 * <p><strong>Common Implementations:</strong>
 *
 * <ul>
 *   <li><strong>RabbitMqOrchestrationProvider</strong> - RabbitMQ-based message queue orchestration
 *   <li><strong>Local Providers</strong> - In-memory implementations for testing and development
 *   <li><strong>Cloud Providers</strong> - Integration with cloud messaging services
 * </ul>
 *
 * @see ScanJobDescription
 * @see ScanJobConsumer
 * @see DoneNotificationConsumer
 * @see BulkScan
 * @see RabbitMqOrchestrationProvider
 */
public interface IOrchestrationProvider {

    /**
     * Submits a scan job for distribution to available worker instances.
     *
     * <p>This method queues a scan job for processing by worker nodes, using the underlying message
     * queue system to ensure reliable delivery. The job will be routed to an available worker based
     * on the provider's load balancing strategy.
     *
     * <p><strong>Delivery Behavior:</strong> The implementation should ensure that jobs are
     * persistently queued and will be delivered even if no workers are currently available,
     * supporting fault-tolerant distributed processing.
     *
     * @param scanJobDescription the scan job to submit for processing
     * @throws RuntimeException if the job cannot be submitted (implementation-specific)
     */
    void submitScanJob(ScanJobDescription scanJobDescription);

    /**
     * Registers a scan job consumer to receive jobs from the orchestration provider.
     *
     * <p>This method registers a worker to receive scan jobs from the message queue. The consumer
     * will be called for each available job, and must acknowledge completion using {@link
     * #notifyOfDoneScanJob(ScanJobDescription)} to ensure reliable processing.
     *
     * <p><strong>Flow Control:</strong> The prefetchCount parameter controls how many
     * unacknowledged jobs can be delivered to this consumer simultaneously, enabling back-pressure
     * management and preventing worker overload.
     *
     * <p><strong>Consumer Lifecycle:</strong> The consumer remains active until the connection is
     * closed or the application terminates. Implementations should handle consumer failures
     * gracefully and support reregistration.
     *
     * @param scanJobConsumer the functional interface to handle incoming scan jobs
     * @param prefetchCount maximum number of unacknowledged jobs to deliver simultaneously
     * @throws RuntimeException if the consumer cannot be registered (implementation-specific)
     */
    void registerScanJobConsumer(ScanJobConsumer scanJobConsumer, int prefetchCount);

    /**
     * Registers a completion notification consumer for a specific bulk scan operation.
     *
     * <p>This method enables controllers to receive notifications when individual scan jobs within
     * a bulk scan complete. The consumer will be called for each job completion, enabling real-time
     * progress tracking and statistics collection.
     *
     * <p><strong>Bulk Scan Scope:</strong> The consumer is registered specifically for the provided
     * bulk scan and will only receive notifications for jobs belonging to that bulk scan operation.
     *
     * <p><strong>Monitoring Integration:</strong> This mechanism is typically used by
     * ProgressMonitor instances to track scan progress and calculate completion statistics.
     *
     * @param bulkScan the bulk scan operation to monitor for completion notifications
     * @param doneNotificationConsumer the consumer to handle job completion events
     * @throws RuntimeException if the consumer cannot be registered (implementation-specific)
     */
    void registerDoneNotificationConsumer(
            BulkScan bulkScan, DoneNotificationConsumer doneNotificationConsumer);

    /**
     * Acknowledges completion of a scan job and triggers completion notifications.
     *
     * <p>This method performs dual functions: it acknowledges successful processing of a scan job
     * to the message queue system, and it publishes completion notifications to registered done
     * notification consumers for progress monitoring.
     *
     * <p><strong>Acknowledgment Behavior:</strong> The method confirms to the message queue that
     * the job has been successfully processed and can be removed from the queue, preventing
     * redelivery to other workers.
     *
     * <p><strong>Notification Publishing:</strong> Simultaneously publishes the completion event to
     * any registered done notification consumers, enabling real-time progress tracking and
     * statistics updates.
     *
     * <p><strong>Status Consistency:</strong> The scan job description's status field should
     * accurately reflect the final processing outcome before calling this method.
     *
     * @param scanJobDescription the completed scan job with final status information
     * @throws RuntimeException if acknowledgment or notification fails (implementation-specific)
     */
    void notifyOfDoneScanJob(ScanJobDescription scanJobDescription);

    /**
     * Closes connections and releases resources used by the orchestration provider.
     *
     * <p>This method performs cleanup of all resources including message queue connections, thread
     * pools, and any other resources allocated during provider operation. It should be called when
     * the application is shutting down or when the provider is no longer needed.
     *
     * <p><strong>Cleanup Responsibilities:</strong>
     *
     * <ul>
     *   <li><strong>Connection Closure</strong> - Close message queue connections gracefully
     *   <li><strong>Consumer Cleanup</strong> - Unregister all active consumers
     *   <li><strong>Resource Release</strong> - Free any allocated resources (threads, memory)
     *   <li><strong>State Cleanup</strong> - Clear any internal state or caches
     * </ul>
     *
     * <p><strong>Thread Safety:</strong> This method should be safe to call from any thread and
     * should handle concurrent calls gracefully.
     */
    void closeConnection();
}
