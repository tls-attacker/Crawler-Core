/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.data.ScanJobDescription;
import java.util.concurrent.*;
import org.bson.Document;

public class TestBulkScanWorkerManager {
    private static boolean simulateTimeout = false;
    private static boolean simulateException = false;
    private static boolean simulateNullResult = false;
    private static boolean simulateSecondTimeoutException = false;

    public static void reset() {
        simulateTimeout = false;
        simulateException = false;
        simulateNullResult = false;
        simulateSecondTimeoutException = false;
    }

    public static void setSimulateTimeout(boolean value) {
        simulateTimeout = value;
    }

    public static void setSimulateException(boolean value) {
        simulateException = value;
    }

    public static void setSimulateNullResult(boolean value) {
        simulateNullResult = value;
    }

    public static void setSimulateSecondTimeoutException(boolean value) {
        simulateSecondTimeoutException = value;
    }

    public static Future<Document> handleStatic(
            ScanJobDescription scanJobDescription,
            int parallelConnectionThreads,
            int parallelScanThreads) {

        if (simulateTimeout) {
            return new TimeoutFuture();
        } else if (simulateException) {
            return new ExceptionFuture();
        } else if (simulateNullResult) {
            return new NullResultFuture();
        } else {
            // Normal case - return a successful result
            return new SuccessfulFuture();
        }
    }

    private static class SuccessfulFuture implements Future<Document> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Document get() {
            return new Document("result", "success");
        }

        @Override
        public Document get(long timeout, TimeUnit unit) {
            return get();
        }
    }

    private static class TimeoutFuture implements Future<Document> {
        private boolean cancelled = false;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Document get() throws InterruptedException, ExecutionException {
            Thread.sleep(10000); // Simulate long-running task
            return new Document("result", "timeout");
        }

        @Override
        public Document get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            if (cancelled) {
                if (simulateSecondTimeoutException) {
                    // Simulate the case where even after cancel, the future times out
                    throw new TimeoutException("Second timeout after cancel");
                }
                // Simulate successful completion after cancel
                return new Document("result", "cancelled");
            }
            throw new TimeoutException("Simulated timeout");
        }
    }

    private static class ExceptionFuture implements Future<Document> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Document get() throws ExecutionException {
            throw new ExecutionException(
                    "Simulated scan failure", new RuntimeException("Scan error"));
        }

        @Override
        public Document get(long timeout, TimeUnit unit) throws ExecutionException {
            return get();
        }
    }

    private static class NullResultFuture implements Future<Document> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Document get() {
            return null; // Simulate null result (EMPTY status)
        }

        @Override
        public Document get(long timeout, TimeUnit unit) {
            return get();
        }
    }
}
