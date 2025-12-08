/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.bson.Document;

/**
 * Represents a scheduled scan that tracks progress and provides both partial and final results.
 *
 * <p>This class provides a clean abstraction for the scan lifecycle:
 *
 * <ul>
 *   <li>Check if the scan is complete via {@link #isComplete()}
 *   <li>Get the current result (partial or final) via {@link #getCurrentResult()}
 *   <li>Wait for the final result via {@link #getFinalResult()}
 * </ul>
 */
public class ScheduledScan {

    private volatile Document currentResult;
    private final CompletableFuture<Document> finalResult = new CompletableFuture<>();

    /**
     * Check if the scan has completed.
     *
     * @return true if the scan is complete, false if still in progress
     */
    public boolean isComplete() {
        return finalResult.isDone();
    }

    /**
     * Get the current result document. If the scan is still in progress, this returns the latest
     * partial result. If the scan is complete, this returns the final result.
     *
     * @return The current result document, or null if no result is available yet
     */
    public Document getCurrentResult() {
        return currentResult;
    }

    /**
     * Get a Future that will resolve to the final result when the scan completes.
     *
     * @return A Future containing the final scan result
     */
    public Future<Document> getFinalResult() {
        return finalResult;
    }

    /**
     * Update the current result. This is called by the scan worker when new partial results are
     * available.
     *
     * @param partialResult The updated partial result document
     */
    public void updateResult(Document partialResult) {
        this.currentResult = partialResult;
    }

    /**
     * Mark the scan as complete with the final result. This will complete the Future and notify any
     * waiting consumers.
     *
     * @param result The final scan result
     */
    void complete(Document result) {
        this.currentResult = result;
        this.finalResult.complete(result);
    }

    /**
     * Mark the scan as failed with an exception.
     *
     * @param exception The exception that caused the failure
     */
    void completeExceptionally(Throwable exception) {
        this.finalResult.completeExceptionally(exception);
    }
}
