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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A Future implementation that supports tracking progress through partial results.
 *
 * <p>This class extends the standard {@link Future} contract with the ability to:
 *
 * <ul>
 *   <li>Get the current partial result via {@link #getCurrentResult()}
 *   <li>Update the partial result as work progresses via {@link #updateResult(Object)}
 *   <li>Wait for the final result via standard Future methods ({@link #get()}, {@link #get(long,
 *       TimeUnit)})
 * </ul>
 *
 * @param <T> The type of result this future produces
 */
public class ProgressableFuture<T> implements Future<T> {

    private volatile T currentResult;
    private final CompletableFuture<T> delegate = new CompletableFuture<>();

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }

    /**
     * Get the current result. If the operation is still in progress, this returns the latest
     * partial result. If the operation is complete, this returns the final result.
     *
     * @return The current result, or null if no result is available yet
     */
    public T getCurrentResult() {
        return currentResult;
    }

    /**
     * Update the current result with a partial result. This is called during processing when new
     * partial results are available.
     *
     * @param partialResult The updated partial result
     */
    public void updateResult(T partialResult) {
        if (delegate.isDone()) {
            return;
        }
        this.currentResult = partialResult;
    }

    /**
     * Mark the operation as complete with the final result. This will complete the Future and
     * notify any waiting consumers.
     *
     * @param result The final result
     */
    void complete(T result) {
        if (delegate.isDone()) {
            return;
        }
        this.currentResult = result;
        this.delegate.complete(result);
    }

    /**
     * Mark the operation as failed with an exception.
     *
     * @param exception The exception that caused the failure
     */
    void completeExceptionally(Throwable exception) {
        if (delegate.isDone()) {
            return;
        }
        this.delegate.completeExceptionally(exception);
    }
}
