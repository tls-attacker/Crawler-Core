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
 * A ThreadPoolExecutor that creates CancellableFuture tasks. This executor ensures that submitted
 * tasks can be properly cancelled, including interrupting the underlying thread when a task is
 * cancelled.
 */
public class CanceallableThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * Creates a new CanceallableThreadPoolExecutor with the given initial parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks
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
     * @param corePoolSize the number of threads to keep in the pool
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks
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
     * Creates a new CanceallableThreadPoolExecutor with the given initial parameters and rejection
     * handler.
     *
     * @param corePoolSize the number of threads to keep in the pool
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks
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
     * factory, and rejection handler.
     *
     * @param corePoolSize the number of threads to keep in the pool
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks
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

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new CancellableFuture<>(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new CancellableFuture<>(runnable, value);
    }
}
