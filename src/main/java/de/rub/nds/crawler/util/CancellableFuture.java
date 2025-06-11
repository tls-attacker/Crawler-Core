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
 * Enhanced Future implementation that preserves results even after cancellation.
 *
 * <p>The CancellableFuture provides a specialized Future implementation that allows retrieval of
 * results even after the future has been cancelled. This is particularly useful in scenarios where
 * partial results are valuable and should not be lost due to timeout or cancellation.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Result Preservation</strong> - Results remain accessible after cancellation
 *   <li><strong>Thread-Safe Access</strong> - Uses atomic references and semaphores for
 *       synchronization
 *   <li><strong>Timeout Support</strong> - Supports both blocking and timed result retrieval
 *   <li><strong>Standard Interface</strong> - Implements RunnableFuture for executor compatibility
 * </ul>
 *
 * <p><strong>Cancellation Behavior:</strong> Unlike standard FutureTask, this implementation allows
 * access to the computed result even after the future is cancelled. The result is captured
 * atomically before the cancellation takes effect.
 *
 * <p><strong>Synchronization Mechanism:</strong> Uses a Semaphore to coordinate access to results
 * after cancellation, ensuring thread-safe retrieval without blocking indefinitely.
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li><strong>Timeout Scenarios</strong> - Preserve partial scan results when operations timeout
 *   <li><strong>Resource Management</strong> - Cancel long-running tasks while keeping results
 *   <li><strong>Progress Tracking</strong> - Access intermediate results during cancellation
 *   <li><strong>Graceful Degradation</strong> - Use partial results when full completion fails
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> All operations are thread-safe through atomic references and
 * semaphore synchronization. Multiple threads can safely access the future concurrently.
 *
 * @param <V> the type of result produced by this future
 * @see RunnableFuture
 * @see FutureTask
 * @see CanceallableThreadPoolExecutor
 */
public class CancellableFuture<V> implements RunnableFuture<V> {

    private final AtomicReference<V> result = new AtomicReference<>();
    private final RunnableFuture<V> innerFuture;
    private final Semaphore resultWritten = new Semaphore(0);

    /**
     * Creates a new cancellable future for the specified callable task.
     *
     * <p>The future wraps the callable in a FutureTask that captures the result atomically and
     * signals completion via semaphore release, enabling result access even after cancellation.
     *
     * @param callable the task to execute that produces a result
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
     * Creates a new cancellable future for the specified runnable task with a fixed result.
     *
     * <p>The future wraps the runnable in a FutureTask that executes the task and returns the
     * provided result value, with atomic result capture for post-cancellation access.
     *
     * @param runnable the task to execute
     * @param res the result value to return upon successful completion
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

    @Override
    public boolean cancel(boolean b) {
        return innerFuture.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return innerFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return innerFuture.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            return innerFuture.get();
        } catch (CancellationException e) {
            resultWritten.acquire();
            return result.get();
        }
    }

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

    @Override
    public void run() {
        innerFuture.run();
    }
}
