/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.BulkScanJobCounters;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.orchestration.DoneNotificationConsumer;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * The ProgressMonitor keeps track of the progress of the running bulk scans. It consumes the done
 * notifications from the workers and counts for each bulk scan how many scans are done, how many
 * timed out and how many results were written to the DB.
 */
public class ProgressMonitor {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, BulkScanJobCounters> scanJobDetailsById;

    private final IOrchestrationProvider orchestrationProvider;

    private final IPersistenceProvider persistenceProvider;

    private final Scheduler scheduler;

    private boolean listenerRegistered;

    /**
     * Creates a new ProgressMonitor instance.
     *
     * @param orchestrationProvider the orchestration provider for job management
     * @param persistenceProvider the persistence provider for data storage
     * @param scheduler the Quartz scheduler instance
     */
    public ProgressMonitor(
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider,
            Scheduler scheduler) {
        this.scanJobDetailsById = new HashMap<>();
        this.orchestrationProvider = orchestrationProvider;
        this.persistenceProvider = persistenceProvider;
        this.scheduler = scheduler;
    }

    private class BulkscanMonitor implements DoneNotificationConsumer {
        private final BulkScan bulkScan;
        private final BulkScanJobCounters counters;
        private final String bulkScanId;
        private double movingAverageDuration = -1;
        private long lastTime = System.currentTimeMillis();

        public BulkscanMonitor(BulkScan bulkScan, BulkScanJobCounters counters) {
            this.bulkScan = bulkScan;
            this.counters = counters;
            this.bulkScanId = bulkScan.get_id();
        }

        private String formatTime(double millis) {
            if (millis < 1000) {
                return String.format("%4.0f ms", millis);
            }
            double seconds = millis / 1000;
            if (seconds < 100) {
                return String.format("%5.2f s", seconds);
            }

            double minutes = seconds / 60;
            seconds = seconds % 60;
            if (minutes < 100) {
                return String.format("%2.0f m %2.0f s", minutes, seconds);
            }
            double hours = minutes / 60;
            minutes = minutes % 60;
            if (hours < 48) {
                return String.format("%2.0f h %2.0f m", hours, minutes);
            }
            double days = hours / 24;
            return String.format("%.1f d", days);
        }

        @Override
        public void consumeDoneNotification(String consumerTag, ScanJobDescription scanJob) {
            try {
                long totalDone = counters.increaseJobStatusCount(scanJob.getStatus());
                long expectedTotal =
                        bulkScan.getScanJobsPublished() != 0
                                ? bulkScan.getScanJobsPublished()
                                : bulkScan.getTargetsGiven();
                long now = System.currentTimeMillis();
                // global average
                double globalAverageDuration = (now - bulkScan.getStartTime()) / (double) totalDone;
                // exponential moving average
                // start with a large alpha to not over-emphasize the first results
                double alpha = totalDone > 20 ? 0.1 : 2 / (double) (totalDone + 1);
                long duration = now - lastTime;
                lastTime = now;
                movingAverageDuration = alpha * duration + (1 - alpha) * movingAverageDuration;

                double eta = (expectedTotal - totalDone) * movingAverageDuration;
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(
                            "BulkScan '{}' - {} of {} scan jobs done | Global Average {}/report | Moving Average {}/report | ETA: {}",
                            bulkScanId,
                            totalDone,
                            expectedTotal,
                            formatTime(globalAverageDuration),
                            formatTime(movingAverageDuration),
                            formatTime(eta));
                    LOGGER.info(
                            "BulkScan '{}' - Successful: {} | Empty: {} | Timeout: {} | Error: {} | Serialization Error: {} | Internal Error: {}",
                            bulkScanId,
                            counters.getJobStatusCount(JobStatus.SUCCESS),
                            counters.getJobStatusCount(JobStatus.EMPTY),
                            counters.getJobStatusCount(JobStatus.CANCELLED),
                            counters.getJobStatusCount(JobStatus.ERROR),
                            counters.getJobStatusCount(JobStatus.SERIALIZATION_ERROR),
                            counters.getJobStatusCount(JobStatus.INTERNAL_ERROR));
                }
                if (totalDone == expectedTotal) {
                    stopMonitoringAndFinalizeBulkScan(scanJob.getBulkScanInfo().getBulkScanId());
                }
            } catch (Exception e) {
                LOGGER.error("Exception in done notification consumer:", e);
            }
        }
    }

    /**
     * Adds a listener for the done notification queue that updates the counters for the bulk scans
     * and checks if a bulk scan is finished.
     *
     * @param bulkScan that should be monitored
     */
    public void startMonitoringBulkScanProgress(BulkScan bulkScan) {
        final BulkScanJobCounters counters = new BulkScanJobCounters(bulkScan);
        scanJobDetailsById.put(bulkScan.get_id(), counters);

        if (!listenerRegistered) {
            orchestrationProvider.registerDoneNotificationConsumer(
                    bulkScan, new BulkscanMonitor(bulkScan, counters));
            listenerRegistered = true;
        }
    }

    /**
     * Finishes the monitoring, updates the bulk scan in DB, sends HTTP notification if configured
     * and shuts the controller down if all bulk scans are finished.
     *
     * @param bulkScanId of the bulk scan for which the monitoring should be stopped.
     */
    public void stopMonitoringAndFinalizeBulkScan(String bulkScanId) {
        LOGGER.info("BulkScan '{}' is finished", bulkScanId);
        BulkScanJobCounters bulkScanJobCounters = scanJobDetailsById.get(bulkScanId);
        BulkScan scan = bulkScanJobCounters.getBulkScan();
        scan.setFinished(true);
        scan.setEndTime(System.currentTimeMillis());
        scan.setSuccessfulScans(bulkScanJobCounters.getJobStatusCount(JobStatus.SUCCESS));
        scan.setJobStatusCounters(bulkScanJobCounters.getJobStatusCountersCopy());
        persistenceProvider.updateBulkScan(scan);
        LOGGER.info("Persisted updated BulkScan with id: {}", scan.get_id());

        scanJobDetailsById.remove(bulkScanId);

        if (scan.getNotifyUrl() != null
                && !scan.getNotifyUrl().isEmpty()
                && !scan.getNotifyUrl().isBlank()) {
            try {
                String response = notify(scan);
                LOGGER.info(
                        "BulkScan {}(id={}): sent notification to '{}' got response: '{}'",
                        scan.getName(),
                        scan.get_id(),
                        scan.getNotifyUrl(),
                        response);
            } catch (IOException e) {
                LOGGER.error(
                        "Could not send notification for bulkScan '{}' because: ", bulkScanId, e);
            } catch (InterruptedException e) {
                LOGGER.error(
                        "Could not send notification for bulkScan '{}' because we were interrupted: ",
                        bulkScanId,
                        e);
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (scanJobDetailsById.isEmpty() && scheduler.isShutdown()) {
                LOGGER.info("All bulkScans are finished. Closing rabbitMq connection.");
                orchestrationProvider.closeConnection();
            }
        } catch (SchedulerException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Sends an HTTP POST request containing the bulk scan object as json as body to the url that is
     * specified for the bulk scan.
     *
     * @param bulkScan for which a done notification request should be sent
     * @return body of the http response as string
     */
    private static String notify(BulkScan bulkScan) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody =
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bulkScan);

        HttpRequest request =
                HttpRequest.newBuilder(URI.create(bulkScan.getNotifyUrl()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString())
                .body();
    }
}
