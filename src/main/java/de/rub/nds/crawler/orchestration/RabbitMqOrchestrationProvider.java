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
 * Provides all methods required for the communication with RabbitMQ for the controller and the
 * worker.
 */
public class RabbitMqOrchestrationProvider implements IOrchestrationProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SCAN_JOB_QUEUE = "scan-job-queue";
    private static final Map<String, Object> SCAN_JOB_QUEUE_PROPERTIES = new HashMap<>();
    private static final Map<String, Object> DONE_NOTIFY_QUEUE_PROPERTIES = new HashMap<>();

    static {
        // set TTLs such that queues are deleted if no consumer is registered
        // https://www.rabbitmq.com/ttl.html#queue-ttl
        SCAN_JOB_QUEUE_PROPERTIES.put("x-expires", 1000 * 60 * 60);
        DONE_NOTIFY_QUEUE_PROPERTIES.put("x-expires", 1000 * 60 * 5);
    }

    private Connection connection;

    private Channel channel;

    private Set<String> declaredQueues = new HashSet<>();

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

    @Override
    public void submitScanJob(ScanJobDescription scanJobDescription) {
        try {
            this.channel.basicPublish(
                    "", SCAN_JOB_QUEUE, null, SerializationUtils.serialize(scanJobDescription));
        } catch (IOException e) {
            LOGGER.error("Failed to submit ScanJobDescription: ", e);
        }
    }

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

    private void sendAck(long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            LOGGER.error("Failed to send message acknowledgment: ", e);
        }
    }

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
