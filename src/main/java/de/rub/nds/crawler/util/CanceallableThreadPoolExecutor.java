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
 * A custom thread pool executor that creates cancellable futures. This executor allows tasks to
 * return a partial result even when cancelled.
 */
public class CanceallableThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * Creates a new thread pool executor with the given parameters.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if idle
     * @param maximumPoolSize The maximum number of threads to allow in the pool
     * @param keepAliveTime How long idle threads should be kept alive
     * @param unit The time unit for the keepAliveTime
     * @param workQueue The queue to use for holding tasks before they are executed
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
     * Creates a new thread pool executor with the given parameters.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if idle
     * @param maximumPoolSize The maximum number of threads to allow in the pool
     * @param keepAliveTime How long idle threads should be kept alive
     * @param unit The time unit for the keepAliveTime
     * @param workQueue The queue to use for holding tasks before they are executed
     * @param threadFactory The factory to use when creating new threads
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
     * Creates a new thread pool executor with the given parameters.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if idle
     * @param maximumPoolSize The maximum number of threads to allow in the pool
     * @param keepAliveTime How long idle threads should be kept alive
     * @param unit The time unit for the keepAliveTime
     * @param workQueue The queue to use for holding tasks before they are executed
     * @param handler The handler to use when execution is blocked
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
     * Creates a new thread pool executor with the given parameters.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if idle
     * @param maximumPoolSize The maximum number of threads to allow in the pool
     * @param keepAliveTime How long idle threads should be kept alive
     * @param unit The time unit for the keepAliveTime
     * @param workQueue The queue to use for holding tasks before they are executed
     * @param threadFactory The factory to use when creating new threads
     * @param handler The handler to use when execution is blocked
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
     * Creates a new cancellable future for the given callable.
     *
     * @param <T> The type of the result
     * @param callable The callable to be executed
     * @return A new cancellable future for the callable
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new CancellableFuture<>(callable);
    }

    /**
     * Creates a new cancellable future for the given runnable and result value.
     *
     * @param <T> The type of the result
     * @param runnable The runnable to be executed
     * @param value The result value to return when the runnable completes
     * @return A new cancellable future for the runnable
     */
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new CancellableFuture<>(runnable, value);
    }
}
