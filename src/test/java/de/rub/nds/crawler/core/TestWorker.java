/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.orchestration.ScanJobConsumer;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import org.bson.Document;

public class TestWorker extends Worker {
    private final int parallelConnectionThreads;
    private final int parallelScanThreads;
    private boolean useTestBulkScanWorkerManager = false;

    public TestWorker(
            WorkerCommandConfig commandConfig,
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider) {
        super(commandConfig, orchestrationProvider, persistenceProvider);
        this.parallelConnectionThreads = commandConfig.getParallelConnectionThreads();
        this.parallelScanThreads = commandConfig.getParallelScanThreads();
    }

    public void setUseTestBulkScanWorkerManager(boolean useTest) {
        this.useTestBulkScanWorkerManager = useTest;
    }

    @Override
    public void start() {
        try {
            // Access the private orchestrationProvider field
            Field orchestrationField = Worker.class.getDeclaredField("orchestrationProvider");
            orchestrationField.setAccessible(true);
            IOrchestrationProvider orchestrationProvider =
                    (IOrchestrationProvider) orchestrationField.get(this);

            // Access the private parallelScanThreads field
            Field threadsField = Worker.class.getDeclaredField("parallelScanThreads");
            threadsField.setAccessible(true);
            int threads = (int) threadsField.get(this);

            // Create a custom ScanJobConsumer that intercepts the scan job handling
            ScanJobConsumer consumer =
                    scanJobDescription -> {
                        if (useTestBulkScanWorkerManager) {
                            handleScanJobWithTestManager(scanJobDescription);
                        } else {
                            // Use reflection to call the private handleScanJob method
                            try {
                                Method handleMethod =
                                        Worker.class.getDeclaredMethod(
                                                "handleScanJob", ScanJobDescription.class);
                                handleMethod.setAccessible(true);
                                handleMethod.invoke(this, scanJobDescription);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to invoke handleScanJob", e);
                            }
                        }
                    };

            orchestrationProvider.registerScanJobConsumer(consumer, threads);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start TestWorker", e);
        }
    }

    private void handleScanJobWithTestManager(ScanJobDescription scanJobDescription) {
        try {
            // Get the workerExecutor field
            Field executorField = Worker.class.getDeclaredField("workerExecutor");
            executorField.setAccessible(true);
            java.util.concurrent.ThreadPoolExecutor workerExecutor =
                    (java.util.concurrent.ThreadPoolExecutor) executorField.get(this);

            // Use TestBulkScanWorkerManager instead of the real one
            Future<Document> resultFuture =
                    TestBulkScanWorkerManager.handleStatic(
                            scanJobDescription, parallelConnectionThreads, parallelScanThreads);

            // Submit the task to process the result
            workerExecutor.submit(
                    () -> {
                        try {
                            Method waitForScanResultMethod =
                                    Worker.class.getDeclaredMethod(
                                            "waitForScanResult",
                                            Future.class,
                                            ScanJobDescription.class);
                            waitForScanResultMethod.setAccessible(true);

                            Method persistResultMethod =
                                    Worker.class.getDeclaredMethod(
                                            "persistResult",
                                            ScanJobDescription.class,
                                            de.rub.nds.crawler.data.ScanResult.class);
                            persistResultMethod.setAccessible(true);

                            de.rub.nds.crawler.data.ScanResult scanResult = null;
                            boolean persist = true;

                            try {
                                scanResult =
                                        (de.rub.nds.crawler.data.ScanResult)
                                                waitForScanResultMethod.invoke(
                                                        this, resultFuture, scanJobDescription);
                            } catch (Exception e) {
                                // Handle all the exception cases similar to the original
                                // handleScanJob
                                Throwable cause = e.getCause();
                                if (cause instanceof InterruptedException) {
                                    scanJobDescription.setStatus(
                                            de.rub.nds.crawler.constant.JobStatus.INTERNAL_ERROR);
                                    persist = false;
                                    Thread.currentThread().interrupt();
                                } else if (cause
                                        instanceof java.util.concurrent.ExecutionException) {
                                    scanJobDescription.setStatus(
                                            de.rub.nds.crawler.constant.JobStatus.ERROR);
                                    scanResult =
                                            de.rub.nds.crawler.data.ScanResult.fromException(
                                                    scanJobDescription, (Exception) cause);
                                } else if (cause instanceof java.util.concurrent.TimeoutException) {
                                    scanJobDescription.setStatus(
                                            de.rub.nds.crawler.constant.JobStatus.CANCELLED);
                                    resultFuture.cancel(true);
                                    scanResult =
                                            de.rub.nds.crawler.data.ScanResult.fromException(
                                                    scanJobDescription, (Exception) cause);
                                } else {
                                    scanJobDescription.setStatus(
                                            de.rub.nds.crawler.constant.JobStatus.CRAWLER_ERROR);
                                    scanResult =
                                            de.rub.nds.crawler.data.ScanResult.fromException(
                                                    scanJobDescription, new Exception(cause));
                                }
                            } finally {
                                if (persist) {
                                    persistResultMethod.invoke(
                                            this, scanJobDescription, scanResult);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to process scan job", e);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle scan job with test manager", e);
        }
    }
}
