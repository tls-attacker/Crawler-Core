/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.constant.JobStatus;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkScanJobCountersTest {

    @Mock private BulkScan mockBulkScan;

    private BulkScanJobCounters counters;

    @BeforeEach
    void setUp() {
        counters = new BulkScanJobCounters(mockBulkScan);
    }

    @Test
    void testConstructor() {
        assertNotNull(counters);
        assertEquals(mockBulkScan, counters.getBulkScan());

        // Verify all JobStatus values except TO_BE_EXECUTED are initialized with 0
        Map<JobStatus, Integer> statusCounts = counters.getJobStatusCountersCopy();
        assertEquals(JobStatus.values().length - 1, statusCounts.size());

        for (JobStatus status : JobStatus.values()) {
            if (status != JobStatus.TO_BE_EXECUTED) {
                assertTrue(statusCounts.containsKey(status));
                assertEquals(0, statusCounts.get(status));
            } else {
                assertFalse(statusCounts.containsKey(status));
            }
        }
    }

    @Test
    void testGetBulkScan() {
        assertEquals(mockBulkScan, counters.getBulkScan());
    }

    @Test
    void testGetJobStatusCountersCopy() {
        // Increase some counters
        counters.increaseJobStatusCount(JobStatus.SUCCESS);
        counters.increaseJobStatusCount(JobStatus.SUCCESS);
        counters.increaseJobStatusCount(JobStatus.ERROR);

        Map<JobStatus, Integer> copy1 = counters.getJobStatusCountersCopy();
        Map<JobStatus, Integer> copy2 = counters.getJobStatusCountersCopy();

        // Verify copies are independent
        assertNotSame(copy1, copy2);
        assertEquals(copy1, copy2);

        // Verify counts
        assertEquals(2, copy1.get(JobStatus.SUCCESS));
        assertEquals(1, copy1.get(JobStatus.ERROR));
        assertEquals(0, copy1.get(JobStatus.EMPTY));

        // Verify TO_BE_EXECUTED is not in the map
        assertFalse(copy1.containsKey(JobStatus.TO_BE_EXECUTED));
    }

    @Test
    void testGetJobStatusCount() {
        assertEquals(0, counters.getJobStatusCount(JobStatus.SUCCESS));

        counters.increaseJobStatusCount(JobStatus.SUCCESS);
        assertEquals(1, counters.getJobStatusCount(JobStatus.SUCCESS));

        counters.increaseJobStatusCount(JobStatus.SUCCESS);
        assertEquals(2, counters.getJobStatusCount(JobStatus.SUCCESS));

        // Test other statuses remain at 0
        assertEquals(0, counters.getJobStatusCount(JobStatus.ERROR));
        assertEquals(0, counters.getJobStatusCount(JobStatus.DENYLISTED));
    }

    @Test
    void testIncreaseJobStatusCount() {
        // Test that increaseJobStatusCount returns the total count
        assertEquals(1, counters.increaseJobStatusCount(JobStatus.SUCCESS));
        assertEquals(2, counters.increaseJobStatusCount(JobStatus.ERROR));
        assertEquals(3, counters.increaseJobStatusCount(JobStatus.SUCCESS));
        assertEquals(4, counters.increaseJobStatusCount(JobStatus.EMPTY));

        // Verify individual counts
        assertEquals(2, counters.getJobStatusCount(JobStatus.SUCCESS));
        assertEquals(1, counters.getJobStatusCount(JobStatus.ERROR));
        assertEquals(1, counters.getJobStatusCount(JobStatus.EMPTY));
    }

    @Test
    void testAllJobStatusValues() {
        // Test incrementing all valid job statuses
        int expectedTotal = 0;
        for (JobStatus status : JobStatus.values()) {
            if (status != JobStatus.TO_BE_EXECUTED) {
                expectedTotal++;
                assertEquals(expectedTotal, counters.increaseJobStatusCount(status));
                assertEquals(1, counters.getJobStatusCount(status));
            }
        }
    }

    @Test
    void testConcurrentIncrement() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // Create threads that will increment SUCCESS counter
        for (int i = 0; i < threadCount; i++) {
            executor.submit(
                    () -> {
                        try {
                            startLatch.await();
                            for (int j = 0; j < incrementsPerThread; j++) {
                                counters.increaseJobStatusCount(JobStatus.SUCCESS);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            endLatch.countDown();
                        }
                    });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify the count is correct
        assertEquals(
                threadCount * incrementsPerThread, counters.getJobStatusCount(JobStatus.SUCCESS));
    }

    @Test
    void testConcurrentMixedOperations() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // Some threads increment, others read
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(
                    () -> {
                        try {
                            startLatch.await();
                            if (threadId % 2 == 0) {
                                // Even threads increment
                                for (int j = 0; j < 50; j++) {
                                    counters.increaseJobStatusCount(JobStatus.SUCCESS);
                                    counters.increaseJobStatusCount(JobStatus.ERROR);
                                }
                            } else {
                                // Odd threads read
                                for (int j = 0; j < 50; j++) {
                                    counters.getJobStatusCount(JobStatus.SUCCESS);
                                    counters.getJobStatusCountersCopy();
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            endLatch.countDown();
                        }
                    });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify counts
        assertEquals(250, counters.getJobStatusCount(JobStatus.SUCCESS)); // 5 even threads * 50
        assertEquals(250, counters.getJobStatusCount(JobStatus.ERROR)); // 5 even threads * 50
    }

    @Test
    void testNullPointerExceptionForToBeExecuted() {
        // This should throw NPE because TO_BE_EXECUTED is not in the map
        assertThrows(
                NullPointerException.class,
                () -> {
                    counters.getJobStatusCount(JobStatus.TO_BE_EXECUTED);
                });

        assertThrows(
                NullPointerException.class,
                () -> {
                    counters.increaseJobStatusCount(JobStatus.TO_BE_EXECUTED);
                });
    }

    @Test
    void testGetJobStatusCountersCopyIndependence() {
        counters.increaseJobStatusCount(JobStatus.SUCCESS);

        Map<JobStatus, Integer> copy = counters.getJobStatusCountersCopy();
        assertEquals(1, copy.get(JobStatus.SUCCESS));

        // Modify the original
        counters.increaseJobStatusCount(JobStatus.SUCCESS);

        // Copy should not change
        assertEquals(1, copy.get(JobStatus.SUCCESS));

        // New copy should have updated value
        Map<JobStatus, Integer> newCopy = counters.getJobStatusCountersCopy();
        assertEquals(2, newCopy.get(JobStatus.SUCCESS));
    }

    @Test
    void testTotalCountAcrossAllStatuses() {
        AtomicInteger expectedTotal = new AtomicInteger(0);

        // Increment various statuses
        assertEquals(
                expectedTotal.incrementAndGet(),
                counters.increaseJobStatusCount(JobStatus.SUCCESS));
        assertEquals(
                expectedTotal.incrementAndGet(), counters.increaseJobStatusCount(JobStatus.ERROR));
        assertEquals(
                expectedTotal.incrementAndGet(), counters.increaseJobStatusCount(JobStatus.EMPTY));
        assertEquals(
                expectedTotal.incrementAndGet(),
                counters.increaseJobStatusCount(JobStatus.SUCCESS));
        assertEquals(
                expectedTotal.incrementAndGet(),
                counters.increaseJobStatusCount(JobStatus.DENYLISTED));
        assertEquals(
                expectedTotal.incrementAndGet(),
                counters.increaseJobStatusCount(JobStatus.UNRESOLVABLE));

        // Verify individual counts
        assertEquals(2, counters.getJobStatusCount(JobStatus.SUCCESS));
        assertEquals(1, counters.getJobStatusCount(JobStatus.ERROR));
        assertEquals(1, counters.getJobStatusCount(JobStatus.EMPTY));
        assertEquals(1, counters.getJobStatusCount(JobStatus.DENYLISTED));
        assertEquals(1, counters.getJobStatusCount(JobStatus.UNRESOLVABLE));

        // Verify total
        assertEquals(6, expectedTotal.get());
    }
}
