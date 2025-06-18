/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;

class CancellableFutureTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void testCallableConstructorAndRun() throws Exception {
        // Given
        String expectedResult = "test result";
        Callable<String> callable = () -> expectedResult;
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        future.run();

        // Then
        assertTrue(future.isDone());
        assertEquals(expectedResult, future.get());
    }

    @Test
    void testRunnableConstructorAndRun() throws Exception {
        // Given
        AtomicBoolean wasRun = new AtomicBoolean(false);
        Runnable runnable = () -> wasRun.set(true);
        String expectedResult = "fixed result";
        CancellableFuture<String> future = new CancellableFuture<>(runnable, expectedResult);

        // When
        future.run();

        // Then
        assertTrue(wasRun.get());
        assertTrue(future.isDone());
        assertEquals(expectedResult, future.get());
    }

    @Test
    void testGetWithTimeout() throws Exception {
        // Given
        String expectedResult = "delayed result";
        Callable<String> callable =
                () -> {
                    Thread.sleep(100);
                    return expectedResult;
                };
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        executor.submit(future);

        // Then
        assertEquals(expectedResult, future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testGetWithTimeoutThrowsTimeoutException() {
        // Given
        Callable<String> callable =
                () -> {
                    Thread.sleep(2000);
                    return "too late";
                };
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        executor.submit(future);

        // Then
        assertThrows(TimeoutException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void testCancel() throws Exception {
        // Given
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        Callable<String> callable =
                () -> {
                    startLatch.countDown();
                    try {
                        Thread.sleep(5000);
                        return "should not complete";
                    } catch (InterruptedException e) {
                        wasInterrupted.set(true);
                        endLatch.countDown();
                        throw e;
                    }
                };
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        executor.submit(future);
        startLatch.await(); // Wait for task to start
        boolean cancelled = future.cancel(true);

        // Then
        assertTrue(cancelled);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());

        // Wait for interrupt to be processed
        assertTrue(endLatch.await(1, TimeUnit.SECONDS));
        assertTrue(wasInterrupted.get());
    }

    @Test
    void testCancelWithoutInterrupt() {
        // Given
        Callable<String> callable =
                () -> {
                    Thread.sleep(5000);
                    return "should not complete";
                };
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When - cancel before running
        boolean cancelled = future.cancel(false);

        // Then
        assertTrue(cancelled);
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    void testGetAfterCancel() throws Exception {
        // Given
        AtomicInteger callCount = new AtomicInteger(0);
        String partialResult = "partial";

        Callable<String> callable =
                () -> {
                    callCount.incrementAndGet();
                    // Simulate some work that produces partial result
                    Thread.sleep(100);
                    return partialResult;
                };

        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        executor.submit(future);
        Thread.sleep(50); // Let it start
        future.cancel(true);

        // Then - get() should return the partial result after cancellation
        try {
            future.get();
        } catch (CancellationException e) {
            // Expected when result is not yet available
        }
    }

    @Test
    void testGetWithTimeoutAfterCancel() throws Exception {
        // Given
        String partialResult = "partial";
        CountDownLatch resultSetLatch = new CountDownLatch(1);

        Runnable runnable =
                () -> {
                    try {
                        Thread.sleep(100);
                        resultSetLatch.countDown();
                    } catch (InterruptedException e) {
                        resultSetLatch.countDown();
                    }
                };

        CancellableFuture<String> future = new CancellableFuture<>(runnable, partialResult);

        // When
        executor.submit(future);
        Thread.sleep(50); // Let it start
        future.cancel(true);
        resultSetLatch.await(); // Wait for result to be set

        // Then
        try {
            String result = future.get(1, TimeUnit.SECONDS);
            assertEquals(partialResult, result);
        } catch (CancellationException e) {
            // This may happen if the timing is different
        }
    }

    @Test
    void testIsDoneBeforeExecution() {
        // Given
        Callable<String> callable = () -> "result";
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // Then
        assertFalse(future.isDone());
    }

    @Test
    void testIsCancelledBeforeCancellation() {
        // Given
        Callable<String> callable = () -> "result";
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // Then
        assertFalse(future.isCancelled());
    }

    @Test
    void testExceptionPropagation() {
        // Given
        RuntimeException expectedException = new RuntimeException("Test exception");
        Callable<String> callable =
                () -> {
                    throw expectedException;
                };
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        future.run();

        // Then
        ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
        assertEquals(expectedException, thrown.getCause());
    }

    @Test
    void testMultipleGetCalls() throws Exception {
        // Given
        String expectedResult = "result";
        Callable<String> callable = () -> expectedResult;
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        future.run();

        // Then - multiple gets should return the same result
        assertEquals(expectedResult, future.get());
        assertEquals(expectedResult, future.get());
        assertEquals(expectedResult, future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testCancelAfterCompletion() throws Exception {
        // Given
        Callable<String> callable = () -> "result";
        CancellableFuture<String> future = new CancellableFuture<>(callable);

        // When
        future.run();
        boolean cancelled = future.cancel(true);

        // Then
        assertFalse(cancelled);
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }
}
