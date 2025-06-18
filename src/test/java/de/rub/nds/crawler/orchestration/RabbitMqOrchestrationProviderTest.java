/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.orchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.rabbitmq.client.*;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanTarget;
import de.rub.nds.scanner.core.config.ScannerDetail;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;

class RabbitMqOrchestrationProviderTest {

    @Mock private ConnectionFactory connectionFactory;

    @Mock private Connection connection;

    @Mock private Channel channel;

    @Mock private RabbitMqDelegate rabbitMqDelegate;

    @Captor private ArgumentCaptor<DeliverCallback> deliverCallbackCaptor;

    @Captor private ArgumentCaptor<CancelCallback> cancelCallbackCaptor;

    @TempDir Path tempDir;

    private RabbitMqOrchestrationProvider provider;

    @BeforeEach
    void setUp() throws IOException, TimeoutException {
        MockitoAnnotations.openMocks(this);
        when(rabbitMqDelegate.getRabbitMqHost()).thenReturn("localhost");
        when(rabbitMqDelegate.getRabbitMqPort()).thenReturn(5672);
    }

    private BulkScan createTestBulkScan(String id, String name) {
        de.rub.nds.crawler.data.ScanConfig scanConfig =
                new de.rub.nds.crawler.data.ScanConfig(ScannerDetail.NORMAL, 3, 2000) {
                    @Override
                    public BulkScanWorker<? extends de.rub.nds.crawler.data.ScanConfig>
                            createWorker(
                                    String bulkScanID,
                                    int parallelConnectionThreads,
                                    int parallelScanThreads) {
                        return null;
                    }
                };
        BulkScan bulkScan =
                new BulkScan(
                        RabbitMqOrchestrationProviderTest.class,
                        RabbitMqOrchestrationProviderTest.class,
                        name,
                        scanConfig,
                        System.currentTimeMillis(),
                        true,
                        null);
        bulkScan.set_id(id);
        return bulkScan;
    }

    @Test
    void testConstructorWithBasicAuth() throws Exception {
        try (MockedConstruction<ConnectionFactory> mockedFactory =
                mockConstruction(
                        ConnectionFactory.class,
                        (mock, context) -> {
                            when(mock.newConnection()).thenReturn(connection);
                        })) {
            when(connection.createChannel()).thenReturn(channel);
            when(rabbitMqDelegate.getRabbitMqUser()).thenReturn("user");
            when(rabbitMqDelegate.getRabbitMqPass()).thenReturn("pass");

            provider = new RabbitMqOrchestrationProvider(rabbitMqDelegate);

            ConnectionFactory factory = mockedFactory.constructed().get(0);
            verify(factory).setHost("localhost");
            verify(factory).setPort(5672);
            verify(factory).setUsername("user");
            verify(factory).setPassword("pass");
            verify(channel)
                    .queueDeclare(
                            eq("scan-job-queue"), eq(false), eq(false), eq(false), any(Map.class));
        }
    }

    @Test
    void testConstructorWithPasswordFile() throws Exception {
        Path passFile = tempDir.resolve("password.txt");
        Files.write(passFile, "filepass".getBytes());

        try (MockedConstruction<ConnectionFactory> mockedFactory =
                mockConstruction(
                        ConnectionFactory.class,
                        (mock, context) -> {
                            when(mock.newConnection()).thenReturn(connection);
                        })) {
            when(connection.createChannel()).thenReturn(channel);
            when(rabbitMqDelegate.getRabbitMqPassFile()).thenReturn(passFile.toString());

            provider = new RabbitMqOrchestrationProvider(rabbitMqDelegate);

            ConnectionFactory factory = mockedFactory.constructed().get(0);
            verify(factory).setPassword("filepass");
        }
    }

    @Test
    void testConstructorWithTLS() throws Exception {
        try (MockedConstruction<ConnectionFactory> mockedFactory =
                mockConstruction(
                        ConnectionFactory.class,
                        (mock, context) -> {
                            when(mock.newConnection()).thenReturn(connection);
                        })) {
            when(connection.createChannel()).thenReturn(channel);
            when(rabbitMqDelegate.isRabbitMqTLS()).thenReturn(true);

            provider = new RabbitMqOrchestrationProvider(rabbitMqDelegate);

            ConnectionFactory factory = mockedFactory.constructed().get(0);
            verify(factory).useSslProtocol();
        }
    }

    @Test
    void testConstructorWithTLSException() throws Exception {
        try (MockedConstruction<ConnectionFactory> mockedFactory =
                mockConstruction(
                        ConnectionFactory.class,
                        (mock, context) -> {
                            when(mock.newConnection()).thenReturn(connection);
                            doThrow(new NoSuchAlgorithmException("Test"))
                                    .when(mock)
                                    .useSslProtocol();
                        })) {
            when(connection.createChannel()).thenReturn(channel);
            when(rabbitMqDelegate.isRabbitMqTLS()).thenReturn(true);

            // Should not throw, just log error
            assertDoesNotThrow(
                    () -> provider = new RabbitMqOrchestrationProvider(rabbitMqDelegate));
        }
    }

    @Test
    void testConstructorConnectionException() throws Exception {
        try (MockedConstruction<ConnectionFactory> mockedFactory =
                mockConstruction(
                        ConnectionFactory.class,
                        (mock, context) -> {
                            when(mock.newConnection())
                                    .thenThrow(new IOException("Connection failed"));
                        })) {

            assertThrows(
                    RuntimeException.class,
                    () -> provider = new RabbitMqOrchestrationProvider(rabbitMqDelegate));
        }
    }

    @Test
    void testSubmitScanJob() throws Exception {
        setupProvider();

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan bulkScan =
                new BulkScan(
                        RabbitMqOrchestrationProviderTest.class,
                        RabbitMqOrchestrationProviderTest.class,
                        "test-scan",
                        null,
                        System.currentTimeMillis(),
                        true,
                        null);
        ScanJobDescription scanJob =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);

        provider.submitScanJob(scanJob);

        verify(channel)
                .basicPublish(
                        eq(""),
                        eq("scan-job-queue"),
                        isNull(),
                        eq(SerializationUtils.serialize(scanJob)));
    }

    @Test
    void testSubmitScanJobIOException() throws Exception {
        setupProvider();

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan bulkScan = createTestBulkScan("bulk-id", "test-scan");
        ScanJobDescription scanJob =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);
        doThrow(new IOException("Publish failed"))
                .when(channel)
                .basicPublish(any(), any(), any(), any());

        // Should not throw, just log error
        assertDoesNotThrow(() -> provider.submitScanJob(scanJob));
    }

    @Test
    void testRegisterScanJobConsumer() throws Exception {
        setupProvider();

        ScanJobConsumer consumer = mock(ScanJobConsumer.class);
        int prefetchCount = 10;

        provider.registerScanJobConsumer(consumer, prefetchCount);

        verify(channel).basicQos(prefetchCount);
        verify(channel)
                .basicConsume(
                        eq("scan-job-queue"),
                        eq(false),
                        deliverCallbackCaptor.capture(),
                        any(CancelCallback.class));

        // Test the delivery callback
        DeliverCallback deliverCallback = deliverCallbackCaptor.getValue();
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan bulkScan =
                new BulkScan(
                        RabbitMqOrchestrationProviderTest.class,
                        RabbitMqOrchestrationProviderTest.class,
                        "test-scan",
                        null,
                        System.currentTimeMillis(),
                        true,
                        null);
        ScanJobDescription scanJob =
                new ScanJobDescription(target, bulkScan, JobStatus.TO_BE_EXECUTED);

        Envelope envelope = mock(Envelope.class);
        when(envelope.getDeliveryTag()).thenReturn(123L);

        AMQP.BasicProperties properties = mock(AMQP.BasicProperties.class);
        Delivery delivery =
                new Delivery(envelope, properties, SerializationUtils.serialize(scanJob));

        deliverCallback.handle("consumerTag", delivery);

        ArgumentCaptor<ScanJobDescription> scanJobCaptor =
                ArgumentCaptor.forClass(ScanJobDescription.class);
        verify(consumer).consumeScanJob(scanJobCaptor.capture());
        assertEquals("example.com", scanJobCaptor.getValue().getScanTarget().getHostname());
        assertEquals(123L, scanJobCaptor.getValue().getDeliveryTag());
    }

    @Test
    void testRegisterScanJobConsumerDeserializationError() throws Exception {
        setupProvider();

        ScanJobConsumer consumer = mock(ScanJobConsumer.class);

        provider.registerScanJobConsumer(consumer, 1);

        verify(channel)
                .basicConsume(
                        eq("scan-job-queue"),
                        eq(false),
                        deliverCallbackCaptor.capture(),
                        any(CancelCallback.class));

        // Test the delivery callback with bad data
        DeliverCallback deliverCallback = deliverCallbackCaptor.getValue();

        Envelope envelope = mock(Envelope.class);
        when(envelope.getDeliveryTag()).thenReturn(123L);

        AMQP.BasicProperties properties = mock(AMQP.BasicProperties.class);
        Delivery delivery = new Delivery(envelope, properties, "bad data".getBytes());

        deliverCallback.handle("consumerTag", delivery);

        verify(channel).basicReject(123L, false);
        verify(consumer, never()).consumeScanJob(any());
    }

    @Test
    void testRegisterScanJobConsumerIOException() throws Exception {
        setupProvider();

        ScanJobConsumer consumer = mock(ScanJobConsumer.class);
        doThrow(new IOException("Register failed"))
                .when(channel)
                .basicConsume(
                        any(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class));

        // Should not throw, just log error
        assertDoesNotThrow(() -> provider.registerScanJobConsumer(consumer, 1));
    }

    @Test
    void testRegisterDoneNotificationConsumer() throws Exception {
        setupProvider();

        BulkScan bulkScan = createTestBulkScan("bulk-123", "test-scan");
        DoneNotificationConsumer consumer = mock(DoneNotificationConsumer.class);

        provider.registerDoneNotificationConsumer(bulkScan, consumer);

        verify(channel)
                .queueDeclare(
                        eq("done-notify-queue_bulk-123"),
                        eq(false),
                        eq(false),
                        eq(true),
                        any(Map.class));
        verify(channel).basicQos(1);
        verify(channel)
                .basicConsume(
                        eq("done-notify-queue_bulk-123"),
                        eq(true),
                        deliverCallbackCaptor.capture(),
                        any(CancelCallback.class));

        // Test the delivery callback
        DeliverCallback deliverCallback = deliverCallbackCaptor.getValue();
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan testBulkScan = createTestBulkScan("test-id", "test-scan");
        ScanJobDescription scanJob =
                new ScanJobDescription(target, testBulkScan, JobStatus.TO_BE_EXECUTED);

        Envelope envelope = mock(Envelope.class);
        AMQP.BasicProperties properties = mock(AMQP.BasicProperties.class);
        Delivery delivery =
                new Delivery(envelope, properties, SerializationUtils.serialize(scanJob));

        deliverCallback.handle("consumerTag", delivery);

        ArgumentCaptor<ScanJobDescription> scanJobCaptor =
                ArgumentCaptor.forClass(ScanJobDescription.class);
        verify(consumer).consumeDoneNotification(eq("consumerTag"), scanJobCaptor.capture());
        assertEquals("example.com", scanJobCaptor.getValue().getScanTarget().getHostname());
    }

    @Test
    void testRegisterDoneNotificationConsumerQueueAlreadyDeclared() throws Exception {
        setupProvider();

        // First registration
        BulkScan bulkScan = createTestBulkScan("bulk-123", "test-scan");
        DoneNotificationConsumer consumer1 = mock(DoneNotificationConsumer.class);
        provider.registerDoneNotificationConsumer(bulkScan, consumer1);

        // Second registration with same bulk scan ID
        DoneNotificationConsumer consumer2 = mock(DoneNotificationConsumer.class);
        provider.registerDoneNotificationConsumer(bulkScan, consumer2);

        // Queue should only be declared once
        verify(channel, times(1))
                .queueDeclare(
                        eq("done-notify-queue_bulk-123"),
                        eq(false),
                        eq(false),
                        eq(true),
                        any(Map.class));
    }

    @Test
    void testRegisterDoneNotificationConsumerIOException() throws Exception {
        setupProvider();

        BulkScan bulkScan = createTestBulkScan("bulk-123", "test-scan");
        DoneNotificationConsumer consumer = mock(DoneNotificationConsumer.class);

        doThrow(new IOException("Queue declare failed"))
                .when(channel)
                .queueDeclare(any(), anyBoolean(), anyBoolean(), anyBoolean(), any());

        // Should not throw, just log error
        assertDoesNotThrow(() -> provider.registerDoneNotificationConsumer(bulkScan, consumer));
    }

    @Test
    void testNotifyOfDoneScanJobMonitored() throws Exception {
        setupProvider();

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan bulkScan = createTestBulkScan("bulk-123", "test-scan");
        bulkScan.setMonitored(true);
        ScanJobDescription scanJob = new ScanJobDescription(target, bulkScan, JobStatus.SUCCESS);
        scanJob.setDeliveryTag(123L);

        provider.notifyOfDoneScanJob(scanJob);

        verify(channel).basicAck(123L, false);
        verify(channel)
                .queueDeclare(
                        eq("done-notify-queue_bulk-123"),
                        eq(false),
                        eq(false),
                        eq(true),
                        any(Map.class));
        verify(channel)
                .basicPublish(
                        eq(""),
                        eq("done-notify-queue_bulk-123"),
                        isNull(),
                        eq(SerializationUtils.serialize(scanJob)));
    }

    @Test
    void testNotifyOfDoneScanJobNotMonitored() throws Exception {
        setupProvider();

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan bulkScan = createTestBulkScan("bulk-123", "test-scan");
        bulkScan.setMonitored(false);
        ScanJobDescription scanJob = new ScanJobDescription(target, bulkScan, JobStatus.SUCCESS);
        scanJob.setDeliveryTag(123L);

        provider.notifyOfDoneScanJob(scanJob);

        verify(channel).basicAck(123L, false);
        verify(channel, never()).basicPublish(any(), any(), any(), any());
    }

    @Test
    void testNotifyOfDoneScanJobAckException() throws Exception {
        setupProvider();

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan bulkScan = createTestBulkScan("bulk-123", "test-scan");
        bulkScan.setMonitored(false);
        ScanJobDescription scanJob = new ScanJobDescription(target, bulkScan, JobStatus.SUCCESS);
        scanJob.setDeliveryTag(123L);

        doThrow(new IOException("Ack failed")).when(channel).basicAck(anyLong(), anyBoolean());

        // Should not throw, just log error
        assertDoesNotThrow(() -> provider.notifyOfDoneScanJob(scanJob));
    }

    @Test
    void testNotifyOfDoneScanJobPublishException() throws Exception {
        setupProvider();

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        BulkScan bulkScan = createTestBulkScan("bulk-123", "test-scan");
        bulkScan.setMonitored(true);
        ScanJobDescription scanJob = new ScanJobDescription(target, bulkScan, JobStatus.SUCCESS);
        scanJob.setDeliveryTag(123L);

        doThrow(new IOException("Publish failed"))
                .when(channel)
                .basicPublish(any(), any(), any(), any());

        // Should not throw, just log error
        assertDoesNotThrow(() -> provider.notifyOfDoneScanJob(scanJob));

        // Ack should still be sent
        verify(channel).basicAck(123L, false);
    }

    @Test
    void testCloseConnection() throws Exception {
        setupProvider();

        provider.closeConnection();

        verify(channel).close();
        verify(connection).close();
    }

    @Test
    void testCloseConnectionException() throws Exception {
        setupProvider();

        doThrow(new IOException("Close failed")).when(channel).close();

        // Should not throw, just log error
        assertDoesNotThrow(() -> provider.closeConnection());
    }

    private void setupProvider() throws Exception {
        try (MockedConstruction<ConnectionFactory> mockedFactory =
                mockConstruction(
                        ConnectionFactory.class,
                        (mock, context) -> {
                            when(mock.newConnection()).thenReturn(connection);
                        })) {
            when(connection.createChannel()).thenReturn(channel);
            provider = new RabbitMqOrchestrationProvider(rabbitMqDelegate);
        }
    }
}
