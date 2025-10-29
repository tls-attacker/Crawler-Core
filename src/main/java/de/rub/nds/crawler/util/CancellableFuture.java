/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.util;

import com.mongodb.lang.NonNull;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A cancellable future implementation that can return partial results even when cancelled. This
 * class wraps a standard FutureTask but captures the result when available, allowing it to be
 * retrieved even after cancellation.
 *
 * @param <V> The result type returned by this future
 */
public class CancellableFuture<V> implements RunnableFuture<V> {

    private final AtomicReference<V> result = new AtomicReference<>();
    private final RunnableFuture<V> innerFuture;
    private final Semaphore resultWritten = new Semaphore(0);

    /**
     * Creates a new cancellable future for the given callable. When the callable completes, the
     * result is stored for retrieval even after cancellation.
     *
     * @param callable The callable to be executed
     */
    public CancellableFuture(Callable<V> callable) {
        innerFuture =
                new FutureTask<>(
                        () -> {
                            V res = callable.call();
                            result.set(res);
                            resultWritten.release();
                            return res;
                        });
    }

    /**
     * Creates a new cancellable future for the given runnable and result value. When the runnable
     * completes, the result value is stored for retrieval even after cancellation.
     *
     * @param runnable The runnable to be executed
     * @param res The result value to return when the runnable completes
     */
    public CancellableFuture(Runnable runnable, V res) {
        innerFuture =
                new FutureTask<>(
                        () -> {
                            runnable.run();
                            result.set(res);
                            resultWritten.release();
                            return res;
                        });
    }

    /**
     * Attempts to cancel execution of this task.
     *
     * @param mayInterruptIfRunning True if the thread executing this task should be interrupted
     * @return True if the task was cancelled, false otherwise
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return innerFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Returns true if this task was cancelled before it completed normally.
     *
     * @return True if this task was cancelled before it completed
     */
    @Override
    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    /**
     * Returns true if this task completed. Completion may be due to normal termination, an
     * exception, or cancellation.
     *
     * @return True if this task completed
     */
    @Override
    public boolean isDone() {
        return innerFuture.isDone();
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result. If the
     * task was cancelled but the result was captured, returns the captured result.
     *
     * @return The computed result
     * @throws InterruptedException If the current thread was interrupted while waiting
     * @throws ExecutionException If the computation threw an exception
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            return innerFuture.get();
        } catch (CancellationException e) {
            resultWritten.acquire();
            return result.get();
        }
    }

    /**
     * Waits if necessary for at most the given time for the computation to complete, and then
     * retrieves its result. If the task was cancelled but the result was captured, returns the
     * captured result if available within the timeout.
     *
     * @param timeout The maximum time to wait
     * @param timeUnit The time unit of the timeout argument
     * @return The computed result
     * @throws InterruptedException If the current thread was interrupted while waiting
     * @throws ExecutionException If the computation threw an exception
     * @throws TimeoutException If the wait timed out
     */
    @Override
    public V get(long timeout, @NonNull TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return innerFuture.get(timeout, timeUnit);
        } catch (CancellationException e) {
            if (resultWritten.tryAcquire(timeout, timeUnit)) {
                return result.get();
            }
            throw new TimeoutException("Timeout while waiting for cancelled result");
        }
    }

    /** Executes the underlying task. */
    @Override
    public void run() {
        innerFuture.run();
    }
}
