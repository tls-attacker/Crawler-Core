/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.dummy;

import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.orchestration.DoneNotificationConsumer;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.orchestration.ScanJobConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;

public class DummyOrchestrationProvider implements IOrchestrationProvider {
    private final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
    private final Thread consumerThread;

    public final BlockingQueue<ScanJobDescription> jobQueue = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<ScanJobConsumer> jobConsumers = new LinkedBlockingQueue<>();
    public final Map<Long, ScanJobDescription> unackedJobs = new HashMap<>();
    public final List<DoneNotificationConsumer> doneNotificationConsumers = new ArrayList<>();
    private long jobIdCounter = 0;

    public DummyOrchestrationProvider() {
        consumerThread = new Thread(this::consumerThreadTask);
        consumerThread.start();
    }

    private void consumerThreadTask() {
        while (true) {
            try {
                // pick consumers round-robin, probably not the most efficient implementation, but
                // this is just for testing
                ScanJobConsumer consumer = jobConsumers.take();
                ScanJobDescription scanJobDescription = jobQueue.take();
                jobConsumers.put(consumer);
                long id = jobIdCounter++;
                unackedJobs.put(id, scanJobDescription);
                LOGGER.info("Sending job {} to consumer as ID {}", scanJobDescription, id);
                scanJobDescription.setDeliveryTag(id);
                consumer.consumeScanJob(scanJobDescription);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void submitScanJob(ScanJobDescription scanJobDescription) {
        LOGGER.info("Received job {}", scanJobDescription);
        jobQueue.add(scanJobDescription);
    }

    @Override
    public void registerScanJobConsumer(ScanJobConsumer scanJobConsumer, int prefetchCount) {
        jobConsumers.add(scanJobConsumer);
    }

    @Override
    public void registerDoneNotificationConsumer(
            BulkScan bulkScan, DoneNotificationConsumer doneNotificationConsumer) {
        doneNotificationConsumers.add(doneNotificationConsumer);
    }

    @Override
    public void notifyOfDoneScanJob(ScanJobDescription scanJobDescription) {
        LOGGER.info(
                "Job {} ID={} was acked", scanJobDescription, scanJobDescription.getDeliveryTag());
        unackedJobs.remove(scanJobDescription.getDeliveryTag());
        for (DoneNotificationConsumer consumer : doneNotificationConsumers) {
            consumer.consumeDoneNotification(null, scanJobDescription);
        }
    }

    @Override
    public void closeConnection() {
        consumerThread.interrupt();
    }

    /**
     * Waits until the job queue reaches the expected size or timeout occurs.
     *
     * @param expectedSize The expected number of jobs in the queue
     * @param timeout The maximum time to wait
     * @param unit The time unit for the timeout
     * @return true if the expected size was reached, false if timeout occurred
     */
    public boolean waitForJobs(int expectedSize, long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
        while (jobQueue.size() < expectedSize) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return false;
            }
            Thread.sleep(Math.min(50, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
        }
        return true;
    }
}
