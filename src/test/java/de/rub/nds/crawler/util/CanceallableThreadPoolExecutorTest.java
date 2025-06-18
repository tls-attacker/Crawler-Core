/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CanceallableThreadPoolExecutorTest {

    private CanceallableThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        executor =
                new CanceallableThreadPoolExecutor(
                        2, // corePoolSize
                        4, // maximumPoolSize
                        60L, // keepAliveTime
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>());
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testConstructorWithThreadFactory() {
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        CanceallableThreadPoolExecutor executorWithFactory =
                new CanceallableThreadPoolExecutor(
                        2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), threadFactory);

        assertNotNull(executorWithFactory);
        executorWithFactory.shutdown();
    }

    @Test
    void testConstructorWithRejectedExecutionHandler() {
        RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();
        CanceallableThreadPoolExecutor executorWithHandler =
                new CanceallableThreadPoolExecutor(
                        2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), handler);

        assertNotNull(executorWithHandler);
        executorWithHandler.shutdown();
    }

    @Test
    void testConstructorWithThreadFactoryAndHandler() {
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();
        CanceallableThreadPoolExecutor executorFull =
                new CanceallableThreadPoolExecutor(
                        2,
                        4,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(),
                        threadFactory,
                        handler);

        assertNotNull(executorFull);
        executorFull.shutdown();
    }

    @Test
    void testSubmitCallable() throws Exception {
        Callable<String> task = () -> "test result";

        Future<String> future = executor.submit(task);

        assertEquals("test result", future.get(1, TimeUnit.SECONDS));
        assertInstanceOf(CancellableFuture.class, future);
    }

    @Test
    void testSubmitRunnable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable task = () -> executed.set(true);

        Future<?> future = executor.submit(task);

        future.get(1, TimeUnit.SECONDS);
        assertTrue(executed.get());
        assertInstanceOf(CancellableFuture.class, future);
    }

    @Test
    void testSubmitRunnableWithResult() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable task = () -> executed.set(true);
        String result = "test result";

        Future<String> future = executor.submit(task, result);

        assertEquals(result, future.get(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
        assertInstanceOf(CancellableFuture.class, future);
    }

    @Test
    void testCancellableFutureCreation() throws Exception {
        Callable<String> task = () -> "test";

        Future<String> future = executor.submit(task);

        assertInstanceOf(CancellableFuture.class, future);
    }

    @Test
    void testExecuteRunnable() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        executor.execute(
                () -> {
                    executed.set(true);
                    latch.countDown();
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(executed.get());
    }

    @Test
    void testMultipleTaskSubmission() throws Exception {
        int taskCount = 20;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedCount = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            Future<Integer> future =
                    executor.submit(
                            () -> {
                                try {
                                    Thread.sleep(10);
                                    completedCount.incrementAndGet();
                                    return taskId;
                                } finally {
                                    latch.countDown();
                                }
                            });
            futures.add(future);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(taskCount, completedCount.get());

        // Verify all futures completed successfully and are CancellableFutures
        for (int i = 0; i < taskCount; i++) {
            assertEquals(i, futures.get(i).get());
            assertInstanceOf(CancellableFuture.class, futures.get(i));
        }
    }

    @Test
    void testTaskRejection() {
        // Create executor with limited queue
        executor =
                new CanceallableThreadPoolExecutor(
                        1, // corePoolSize
                        1, // maximumPoolSize
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(1)); // queue size 1

        CountDownLatch blockingLatch = new CountDownLatch(1);

        // Submit blocking task to fill the thread
        executor.submit(
                () -> {
                    try {
                        blockingLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });

        // Submit task to fill the queue
        executor.submit(() -> "queued");

        // This should be rejected
        assertThrows(
                RejectedExecutionException.class,
                () -> {
                    executor.submit(() -> "rejected");
                });

        blockingLatch.countDown();
    }

    @Test
    void testShutdown() throws Exception {
        AtomicBoolean taskCompleted = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future =
                executor.submit(
                        () -> {
                            latch.await();
                            taskCompleted.set(true);
                            return null;
                        });

        executor.shutdown();
        assertTrue(executor.isShutdown());

        // Should not accept new tasks
        assertThrows(
                RejectedExecutionException.class,
                () -> {
                    executor.submit(() -> "new task");
                });

        // Let the submitted task complete
        latch.countDown();
        future.get(1, TimeUnit.SECONDS);
        assertTrue(taskCompleted.get());

        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        assertTrue(executor.isTerminated());
    }

    @Test
    void testShutdownNow() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        Future<?> future =
                executor.submit(
                        () -> {
                            try {
                                startLatch.countDown();
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                interrupted.set(true);
                                Thread.currentThread().interrupt();
                            }
                        });

        // Wait for task to start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS));

        List<Runnable> pendingTasks = executor.shutdownNow();
        assertTrue(executor.isShutdown());

        // The running task should be interrupted
        assertThrows(
                CancellationException.class,
                () -> {
                    future.get(1, TimeUnit.SECONDS);
                });

        assertTrue(interrupted.get());
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
    }

    @Test
    void testConcurrentTaskExecution() throws Exception {
        int corePoolSize = 2;
        executor =
                new CanceallableThreadPoolExecutor(
                        corePoolSize,
                        corePoolSize,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>());

        CountDownLatch startLatch = new CountDownLatch(corePoolSize);
        CountDownLatch endLatch = new CountDownLatch(corePoolSize);
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        for (int i = 0; i < corePoolSize; i++) {
            executor.submit(
                    () -> {
                        startLatch.countDown();
                        int current = concurrentCount.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, current));
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        concurrentCount.decrementAndGet();
                        endLatch.countDown();
                        return null;
                    });
        }

        assertTrue(startLatch.await(1, TimeUnit.SECONDS));
        assertTrue(endLatch.await(2, TimeUnit.SECONDS));

        // All tasks should have run concurrently
        assertEquals(corePoolSize, maxConcurrent.get());
    }

    @Test
    void testCancellableFutureIsCancelledCorrectly() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Future<String> future =
                executor.submit(
                        () -> {
                            try {
                                latch.await(); // Wait indefinitely
                                return "completed";
                            } catch (InterruptedException e) {
                                wasInterrupted.set(true);
                                Thread.currentThread().interrupt();
                                throw e;
                            }
                        });

        // Cancel the future
        assertTrue(future.cancel(true));
        assertTrue(future.isCancelled());

        // The task should have been interrupted
        Thread.sleep(100); // Give it time to process the cancellation
        assertTrue(wasInterrupted.get());

        latch.countDown(); // Clean up
    }

    @Test
    void testInvokeAll() throws Exception {
        List<Callable<String>> tasks = List.of(() -> "task1", () -> "task2", () -> "task3");

        List<Future<String>> futures = executor.invokeAll(tasks);

        assertEquals(3, futures.size());
        for (int i = 0; i < futures.size(); i++) {
            assertEquals("task" + (i + 1), futures.get(i).get());
            assertInstanceOf(CancellableFuture.class, futures.get(i));
        }
    }

    @Test
    void testInvokeAny() throws Exception {
        List<Callable<String>> tasks =
                List.of(
                        () -> {
                            Thread.sleep(100);
                            return "slow";
                        },
                        () -> "fast",
                        () -> {
                            Thread.sleep(200);
                            return "slower";
                        });

        String result = executor.invokeAny(tasks);

        assertEquals("fast", result);
    }

    @Test
    void testCorePoolSize() {
        assertEquals(2, executor.getCorePoolSize());

        executor.setCorePoolSize(3);
        assertEquals(3, executor.getCorePoolSize());
    }

    @Test
    void testMaximumPoolSize() {
        assertEquals(4, executor.getMaximumPoolSize());

        executor.setMaximumPoolSize(5);
        assertEquals(5, executor.getMaximumPoolSize());
    }

    @Test
    void testKeepAliveTime() {
        assertEquals(60L, executor.getKeepAliveTime(TimeUnit.SECONDS));

        executor.setKeepAliveTime(120L, TimeUnit.SECONDS);
        assertEquals(120L, executor.getKeepAliveTime(TimeUnit.SECONDS));
    }
}
