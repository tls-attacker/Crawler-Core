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
 * A RunnableFuture implementation that allows proper cancellation of tasks. This implementation
 * ensures that even when a task is cancelled, any partial results that were computed before
 * cancellation can still be retrieved.
 *
 * @param <V> the result type returned by this Future's get methods
 */
public class CancellableFuture<V> implements RunnableFuture<V> {

    private final AtomicReference<V> result = new AtomicReference<>();
    private final RunnableFuture<V> innerFuture;
    private final Semaphore resultWritten = new Semaphore(0);

    /**
     * Creates a CancellableFuture that will execute the given Callable.
     *
     * @param callable the callable task to execute
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
     * Creates a CancellableFuture that will execute the given Runnable and return the given result.
     *
     * @param runnable the runnable task to execute
     * @param res the result to return when the task completes
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
     * @param b if true, the thread executing this task should be interrupted; otherwise,
     *     in-progress tasks are allowed to complete
     * @return false if the task could not be cancelled, typically because it has already completed;
     *     true otherwise
     */
    @Override
    public boolean cancel(boolean b) {
        return innerFuture.cancel(b);
    }

    /**
     * Returns true if this task was cancelled before it completed normally.
     *
     * @return true if this task was cancelled before it completed
     */
    @Override
    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    /**
     * Returns true if this task completed.
     *
     * @return true if this task completed
     */
    @Override
    public boolean isDone() {
        return innerFuture.isDone();
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result. If the
     * task was cancelled but had already produced a result, this method returns that result.
     *
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
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
     * retrieves its result, if available. If the task was cancelled but had already produced a
     * result, this method returns that result.
     *
     * @param l the maximum time to wait
     * @param timeUnit the time unit of the timeout argument
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    @Override
    public V get(long l, @NonNull TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return innerFuture.get(l, timeUnit);
        } catch (CancellationException e) {
            if (resultWritten.tryAcquire(l, timeUnit)) {
                return result.get();
            }
            throw new TimeoutException("Timeout while waiting for cancelled result");
        }
    }

    /** Sets this Future to the result of its computation unless it has been cancelled. */
    @Override
    public void run() {
        innerFuture.run();
    }
}
