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
 * Thread pool executor that creates futures with result preservation after cancellation.
 *
 * <p>The CanceallableThreadPoolExecutor extends ThreadPoolExecutor to use CancellableFuture
 * instances instead of standard FutureTask objects. This enables tasks to preserve their results
 * even after being cancelled, which is valuable for timeout scenarios and graceful degradation in
 * distributed scanning operations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Result Preservation</strong> - Tasks retain results after cancellation
 *   <li><strong>Standard Interface</strong> - Drop-in replacement for ThreadPoolExecutor
 *   <li><strong>Timeout Handling</strong> - Better handling of scan timeouts with partial results
 *   <li><strong>Resource Management</strong> - Improved resource cleanup with preserved data
 * </ul>
 *
 * <p><strong>Use Cases:</strong>
 *
 * <ul>
 *   <li><strong>TLS Scanning</strong> - Preserve partial scan results when connections timeout
 *   <li><strong>Long-Running Tasks</strong> - Cancel tasks while keeping intermediate results
 *   <li><strong>Resource Constraints</strong> - Manage memory/CPU while preserving valuable data
 *   <li><strong>Progress Tracking</strong> - Access results from cancelled operations
 * </ul>
 *
 * <p><strong>Behavior:</strong> All submitted tasks are wrapped in CancellableFuture instances,
 * which provide the enhanced cancellation behavior. The executor maintains standard
 * ThreadPoolExecutor semantics for all other operations.
 *
 * @see CancellableFuture
 * @see ThreadPoolExecutor
 */
public class CanceallableThreadPoolExecutor extends ThreadPoolExecutor {
    /**
     * Creates a new cancellable thread pool executor with basic configuration.
     *
     * @param corePoolSize the number of threads to keep in the pool
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
     * Creates a new cancellable thread pool executor with custom thread factory.
     *
     * @param corePoolSize the number of threads to keep in the pool
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
     * Creates a new cancellable thread pool executor with custom rejection handler.
     *
     * @param corePoolSize the number of threads to keep in the pool
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
     * Creates a new cancellable thread pool executor with full configuration options.
     *
     * @param corePoolSize the number of threads to keep in the pool
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

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new CancellableFuture<>(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new CancellableFuture<>(runnable, value);
    }
}
