/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.util;

import java.util.concurrent.*;

/**
 * A custom ThreadPoolExecutor that creates CancellableFuture tasks instead of regular FutureTasks.
 * This allows for better handling of task cancellation by ensuring that partially completed results
 * can still be retrieved even after a task has been cancelled.
 *
 * <p>This executor is particularly useful in scenarios where long-running tasks may need to be
 * cancelled due to timeouts, but any partial results they've produced should still be accessible
 * rather than being lost.
 */
public class CanceallableThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * Creates a new CanceallableThreadPoolExecutor with the given initial parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks before terminating
     * @param unit the time unit for the keepAliveTime argument
     * @param workQueue the queue to use for holding tasks before they are executed
     */
    public CanceallableThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * Creates a new CanceallableThreadPoolExecutor with the given initial parameters and thread
     * factory.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks before terminating
     * @param unit the time unit for the keepAliveTime argument
     * @param workQueue the queue to use for holding tasks before they are executed
     * @param threadFactory the factory to use when the executor creates a new thread
     */
    public CanceallableThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /**
     * Creates a new CanceallableThreadPoolExecutor with the given initial parameters and rejected
     * execution handler.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks before terminating
     * @param unit the time unit for the keepAliveTime argument
     * @param workQueue the queue to use for holding tasks before they are executed
     * @param handler the handler to use when execution is blocked because the thread bounds and
     *     queue capacities are reached
     */
    public CanceallableThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    /**
     * Creates a new CanceallableThreadPoolExecutor with the given initial parameters, thread
     * factory and rejected execution handler.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks before terminating
     * @param unit the time unit for the keepAliveTime argument
     * @param workQueue the queue to use for holding tasks before they are executed
     * @param threadFactory the factory to use when the executor creates a new thread
     * @param handler the handler to use when execution is blocked because the thread bounds and
     *     queue capacities are reached
     */
    public CanceallableThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler);
    }

    /**
     * Returns a CancellableFuture for the given callable task.
     *
     * @param callable the callable task being wrapped
     * @param <T> the type of the callable's result
     * @return a CancellableFuture which, when run, will call the underlying callable
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new CancellableFuture<>(callable);
    }

    /**
     * Returns a CancellableFuture for the given runnable and default value.
     *
     * @param runnable the runnable task being wrapped
     * @param value the default value for the returned future
     * @param <T> the type of the given value
     * @return a CancellableFuture which, when run, will run the underlying runnable and set the
     *     given value as the result upon successful completion
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new CancellableFuture<>(runnable, value);
    }
}
