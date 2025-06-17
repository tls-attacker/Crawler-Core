/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.orchestration;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.scanner.core.execution.NamedThreadFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * RabbitMQ-based orchestration provider for TLS-Crawler.
 *
 * <p>Implements distributed messaging for scan coordination using RabbitMQ. Handles job
 * distribution, load balancing, progress monitoring, and TLS connections.
 *
 * @see IOrchestrationProvider
 * @see RabbitMqDelegate
 * @see ScanJobDescription
 * @see ScanJobConsumer
 * @see DoneNotificationConsumer
 */
public class RabbitMqOrchestrationProvider implements IOrchestrationProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SCAN_JOB_QUEUE = "scan-job-queue";
    private static final Map<String, Object> SCAN_JOB_QUEUE_PROPERTIES = new HashMap<>();
    private static final Map<String, Object> DONE_NOTIFY_QUEUE_PROPERTIES = new HashMap<>();

    static {
        // set TTLs such that queues are deleted if no consumer is registered
        DONE_NOTIFY_QUEUE_PROPERTIES.put("x-expires", 1000 * 60 * 5);
    }

    private Connection connection;

    private Channel channel;

    private Set<String> declaredQueues = new HashSet<>();

    /**
     * Creates a new RabbitMQ orchestration provider and establishes connection.
     *
     * <p>This constructor performs complete initialization of the RabbitMQ connection including
     * authentication, TLS setup, and queue declaration. It establishes the foundation for all
     * subsequent messaging operations.
     *
     * <p><strong>Initialization Sequence:</strong>
     *
     * <ol>
     *   <li>Creates and configures RabbitMQ ConnectionFactory
     *   <li>Sets up authentication (username/password or password file)
     *   <li>Configures TLS/SSL if enabled
     *   <li>Establishes connection and creates channel
     *   <li>Declares the main scan job queue
     * </ol>
     *
     * <p><strong>Authentication Methods:</strong>
     *
     * <ul>
     *   <li>Direct password from configuration takes precedence
     *   <li>Password file reading as fallback option
     *   <li>Graceful error handling for missing password files
     * </ul>
     *
     * <p><strong>Security Features:</strong>
     *
     * <ul>
     *   <li>Optional TLS/SSL encryption for secure communication
     *   <li>Support for username/password authentication
     *   <li>Secure password file reading
     * </ul>
     *
     * <p><strong>Thread Management:</strong> Uses a named thread factory to ensure proper thread
     * identification for monitoring and debugging purposes.
     *
     * @param rabbitMqDelegate the RabbitMQ configuration containing connection parameters
     * @throws RuntimeException if connection to RabbitMQ cannot be established
     * @see RabbitMqDelegate
     * @see ConnectionFactory
     */
    public RabbitMqOrchestrationProvider(RabbitMqDelegate rabbitMqDelegate) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMqDelegate.getRabbitMqHost());
        factory.setPort(rabbitMqDelegate.getRabbitMqPort());
        if (rabbitMqDelegate.getRabbitMqUser() != null) {
            factory.setUsername(rabbitMqDelegate.getRabbitMqUser());
        }
        if (rabbitMqDelegate.getRabbitMqPass() != null) {
            factory.setPassword(rabbitMqDelegate.getRabbitMqPass());
        } else if (rabbitMqDelegate.getRabbitMqPassFile() != null) {
            try {
                factory.setPassword(
                        Files.readAllLines(Paths.get(rabbitMqDelegate.getRabbitMqPassFile()))
                                .get(0));
            } catch (IOException e) {
                LOGGER.error("Could not read rabbitMq password file: ", e);
            }
        }
        if (rabbitMqDelegate.isRabbitMqTLS()) {
            try {
                factory.useSslProtocol();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                LOGGER.error("Could not setup rabbitMq TLS: ", e);
            }
        }
        factory.setThreadFactory(
                new NamedThreadFactory("crawler-orchestration: RabbitMqOrchestrationProvider"));
        try {
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            this.channel.queueDeclare(
                    SCAN_JOB_QUEUE, false, false, false, SCAN_JOB_QUEUE_PROPERTIES);
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Could not connect to RabbitMQ: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets or creates a notification queue for the specified bulk scan.
     *
     * <p>This method implements lazy queue creation for bulk scan completion notifications. Each
     * bulk scan gets its own dedicated notification queue to enable isolated progress monitoring
     * without interference between different scanning campaigns.
     *
     * <p><strong>Queue Properties:</strong>
     *
     * <ul>
     *   <li>Queue name format: "done-notify-queue_" + bulkScanId
     *   <li>Non-durable and auto-delete queues for temporary usage
     *   <li>5-minute TTL to automatically clean up unused queues
     *   <li>One-time declaration per bulkScanId for efficiency
     * </ul>
     *
     * <p><strong>Cleanup Strategy:</strong> Queues are automatically deleted by RabbitMQ after 5
     * minutes of inactivity to prevent resource accumulation from completed scans.
     *
     * @param bulkScanId the unique identifier of the bulk scan
     * @return the notification queue name for the specified bulk scan
     * @see #DONE_NOTIFY_QUEUE_PROPERTIES
     */
    private String getDoneNotifyQueue(String bulkScanId) {
        String queueName = "done-notify-queue_" + bulkScanId;
        if (!declaredQueues.contains(queueName)) {
            try {
                this.channel.queueDeclare(
                        queueName, false, false, true, DONE_NOTIFY_QUEUE_PROPERTIES);
                declaredQueues.add(bulkScanId);
            } catch (IOException e) {
                LOGGER.error("Could not declare done-notify-queue: ", e);
            }
        }
        return queueName;
    }

    /**
     * Submits a scan job to the RabbitMQ queue for processing by available workers.
     *
     * <p>This method publishes scan job descriptions to the main scan job queue where they are
     * distributed to available worker instances using RabbitMQ's round-robin load balancing. The
     * method uses Java object serialization for reliable data transmission.
     *
     * <p><strong>Publishing Details:</strong>
     *
     * <ul>
     *   <li>Uses default exchange (empty string) for direct queue routing
     *   <li>Publishes to the main scan job queue for worker consumption
     *   <li>Serializes job descriptions using Apache Commons SerializationUtils
     *   <li>No special message properties or persistence configuration
     * </ul>
     *
     * <p><strong>Error Handling:</strong> Network and I/O errors are logged but do not throw
     * exceptions, allowing the controller to continue operating even if some job submissions fail.
     *
     * @param scanJobDescription the scan job to submit for processing by workers
     * @see IOrchestrationProvider#submitScanJob(ScanJobDescription)
     * @see ScanJobDescription
     * @see #SCAN_JOB_QUEUE
     */
    @Override
    public void submitScanJob(ScanJobDescription scanJobDescription) {
        try {
            this.channel.basicPublish(
                    "", SCAN_JOB_QUEUE, null, SerializationUtils.serialize(scanJobDescription));
        } catch (IOException e) {
            LOGGER.error("Failed to submit ScanJobDescription: ", e);
        }
    }

    /**
     * Registers a consumer to receive and process scan jobs from the RabbitMQ queue.
     *
     * <p>This method sets up a worker instance to consume scan jobs from the main queue. It
     * configures message prefetching, deserialization handling, and error recovery to ensure
     * reliable job processing.
     *
     * <p><strong>Consumer Configuration:</strong>
     *
     * <ul>
     *   <li>Sets QoS prefetch count to control worker load
     *   <li>Disables auto-acknowledgment for reliable delivery
     *   <li>Handles deserialization errors gracefully
     *   <li>Rejects and drops invalid messages to prevent queue blocking
     * </ul>
     *
     * <p><strong>Message Processing:</strong>
     *
     * <ol>
     *   <li>Receives serialized scan job descriptions from queue
     *   <li>Deserializes messages using Apache Commons SerializationUtils
     *   <li>Adds delivery tag to job description for acknowledgment tracking
     *   <li>Delegates to the provided ScanJobConsumer for actual processing
     * </ol>
     *
     * <p><strong>Error Recovery:</strong> Malformed or undeserializable messages are rejected and
     * dropped rather than being requeued, preventing infinite processing loops.
     *
     * @param scanJobConsumer the consumer instance that will process received scan jobs
     * @param prefetchCount the maximum number of unacknowledged messages per worker
     * @see IOrchestrationProvider#registerScanJobConsumer(ScanJobConsumer, int)
     * @see ScanJobConsumer
     * @see ScanJobDescription
     */
    @Override
    public void registerScanJobConsumer(ScanJobConsumer scanJobConsumer, int prefetchCount) {
        DeliverCallback deliverCallback =
                (consumerTag, delivery) -> {
                    long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                    ScanJobDescription scanJobDescription = null;
                    try {
                        scanJobDescription = SerializationUtils.deserialize(delivery.getBody());
                        scanJobDescription.setDeliveryTag(deliveryTag);
                    } catch (SerializationException e) {
                        LOGGER.error(
                                "Failed to deserialize ScanJobDescription - rejecting and dropping",
                                e);
                        channel.basicReject(deliveryTag, false);
                    }
                    if (scanJobDescription != null) {
                        scanJobConsumer.consumeScanJob(scanJobDescription);
                    }
                };
        try {
            channel.basicQos(prefetchCount);
            channel.basicConsume(SCAN_JOB_QUEUE, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            LOGGER.error("Failed to register ScanJobDescription consumer: ", e);
        }
    }

    /**
     * Sends message acknowledgment to RabbitMQ for the specified delivery tag.
     *
     * <p>This private method handles the RabbitMQ message acknowledgment protocol. Acknowledgments
     * confirm that a message has been successfully processed and can be removed from the queue.
     *
     * <p><strong>Acknowledgment Details:</strong>
     *
     * <ul>
     *   <li>Acknowledges a single message (not multiple)
     *   <li>Confirms successful processing of scan job
     *   <li>Allows RabbitMQ to remove message from queue
     *   <li>Handles I/O errors gracefully with logging
     * </ul>
     *
     * @param deliveryTag the unique delivery tag of the message to acknowledge
     * @see #notifyOfDoneScanJob(ScanJobDescription)
     */
    private void sendAck(long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            LOGGER.error("Failed to send message acknowledgment: ", e);
        }
    }

    /**
     * Registers a consumer to receive completion notifications for a specific bulk scan.
     *
     * <p>This method sets up monitoring for bulk scan progress by registering a consumer on the
     * scan's dedicated notification queue. It enables real-time tracking of scan completion and
     * progress monitoring.
     *
     * <p><strong>Consumer Configuration:</strong>
     *
     * <ul>
     *   <li>QoS prefetch count of 1 for sequential notification processing
     *   <li>Auto-acknowledgment enabled for notification messages
     *   <li>Uses the bulk scan's unique notification queue
     *   <li>Automatic deserialization of notification payloads
     * </ul>
     *
     * <p><strong>Monitoring Features:</strong>
     *
     * <ul>
     *   <li>Per-scan isolation through dedicated queues
     *   <li>Real-time completion notifications
     *   <li>Consumer tag tracking for management
     *   <li>Automatic payload deserialization
     * </ul>
     *
     * <p><strong>Queue Management:</strong> The notification queue is created lazily when first
     * accessed and automatically cleaned up after the scan completes due to TTL configuration.
     *
     * @param bulkScan the bulk scan to monitor for completion notifications
     * @param doneNotificationConsumer the consumer to handle completion notifications
     * @see IOrchestrationProvider#registerDoneNotificationConsumer(BulkScan,
     *     DoneNotificationConsumer)
     * @see DoneNotificationConsumer
     * @see #getDoneNotifyQueue(String)
     */
    @Override
    public void registerDoneNotificationConsumer(
            BulkScan bulkScan, DoneNotificationConsumer doneNotificationConsumer) {
        DeliverCallback deliverCallback =
                (consumerTag, delivery) ->
                        doneNotificationConsumer.consumeDoneNotification(
                                consumerTag, SerializationUtils.deserialize(delivery.getBody()));
        try {
            channel.basicQos(1);
            channel.basicConsume(
                    getDoneNotifyQueue(bulkScan.get_id()),
                    true,
                    deliverCallback,
                    consumerTag -> {});
        } catch (IOException e) {
            LOGGER.error("Failed to register DoneNotification consumer: ", e);
        }
    }

    /**
     * Notifies completion of a scan job and sends progress notification if monitoring is enabled.
     *
     * <p>This method handles the completion workflow for scan jobs by acknowledging the original
     * message and optionally sending progress notifications for monitored scans. It ensures
     * reliable message processing and enables progress tracking.
     *
     * <p><strong>Completion Workflow:</strong>
     *
     * <ol>
     *   <li>Acknowledges the original scan job message
     *   <li>Checks if the bulk scan is monitored
     *   <li>Publishes completion notification if monitoring is enabled
     *   <li>Handles publishing errors gracefully
     * </ol>
     *
     * <p><strong>Monitoring Integration:</strong>
     *
     * <ul>
     *   <li>Only sends notifications for monitored bulk scans
     *   <li>Uses the bulk scan's dedicated notification queue
     *   <li>Serializes the completed job description for notification
     *   <li>Enables real-time progress tracking
     * </ul>
     *
     * <p><strong>Error Handling:</strong> Message acknowledgment always occurs regardless of
     * notification success, ensuring scan jobs don't get stuck in the queue due to monitoring
     * issues.
     *
     * @param scanJobDescription the completed scan job to acknowledge and notify
     * @see IOrchestrationProvider#notifyOfDoneScanJob(ScanJobDescription)
     * @see #sendAck(long)
     * @see #getDoneNotifyQueue(String)
     */
    @Override
    public void notifyOfDoneScanJob(ScanJobDescription scanJobDescription) {
        sendAck(scanJobDescription.getDeliveryTag());
        if (scanJobDescription.getBulkScanInfo().isMonitored()) {
            try {
                this.channel.basicPublish(
                        "",
                        getDoneNotifyQueue(scanJobDescription.getBulkScanInfo().getBulkScanId()),
                        null,
                        SerializationUtils.serialize(scanJobDescription));
            } catch (IOException e) {
                LOGGER.error("Failed to send notification for done ScanJobDescription: ", e);
            }
        }
    }

    /**
     * Closes the RabbitMQ connection and associated resources.
     *
     * <p>This method performs clean shutdown of the RabbitMQ connection by closing the channel and
     * connection in the proper order. It handles potential errors during shutdown gracefully to
     * ensure resources are released.
     *
     * <p><strong>Shutdown Sequence:</strong>
     *
     * <ol>
     *   <li>Closes the RabbitMQ channel
     *   <li>Closes the RabbitMQ connection
     *   <li>Logs any errors that occur during shutdown
     * </ol>
     *
     * <p><strong>Resource Management:</strong>
     *
     * <ul>
     *   <li>Ensures proper cleanup of RabbitMQ resources
     *   <li>Prevents resource leaks in long-running applications
     *   <li>Handles network timeouts and I/O errors gracefully
     * </ul>
     *
     * <p><strong>Error Handling:</strong> Shutdown errors are logged but do not prevent the method
     * from completing, ensuring that cleanup attempts continue even if some resources fail to
     * close.
     *
     * @see IOrchestrationProvider#closeConnection()
     */
    @Override
    public void closeConnection() {
        try {
            this.channel.close();
            this.connection.close();
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Failed to close RabbitMQ connection: ", e);
        }
    }
}
