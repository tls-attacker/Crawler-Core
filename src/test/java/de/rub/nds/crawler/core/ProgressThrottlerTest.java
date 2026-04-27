/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class ProgressThrottlerTest {

    private static final long THROTTLE_MS = 5000;

    private static ProgressThrottler<String> createThrottler() {
        ProgressThrottler<String> throttler =
                new ProgressThrottler<>(THROTTLE_MS, "test-throttler");
        throttler.init();
        return throttler;
    }

    @Test
    public void testFirstUpdatePublishesImmediately() {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            List<String> received = new ArrayList<>();
            throttler.submit("update-1", received::add, THROTTLE_MS);

            assertEquals(1, received.size());
            assertEquals("update-1", received.get(0));
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testUpdateWithinThrottleWindowIsDeferred() {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            List<String> received = new ArrayList<>();
            long baseTime = 10000L;

            throttler.submit("update-1", received::add, baseTime);
            assertEquals(1, received.size());

            throttler.submit("update-2", received::add, baseTime + 1000);
            assertEquals(1, received.size());
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testUpdateAfterThrottleWindowPublishesImmediately() {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            List<String> received = new ArrayList<>();
            long baseTime = 10000L;

            throttler.submit("update-1", received::add, baseTime);
            assertEquals(1, received.size());

            long afterThrottle = baseTime + THROTTLE_MS;
            throttler.submit("update-2", received::add, afterThrottle);
            assertEquals(2, received.size());
            assertEquals("update-2", received.get(1));
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testScheduledPublishFiresAfterDelay() throws Exception {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            List<String> received = Collections.synchronizedList(new ArrayList<>());
            long baseTime = 10000L;

            throttler.submit("update-1", received::add, baseTime);
            assertEquals(1, received.size());

            Consumer<String> latchConsumer =
                    s -> {
                        received.add(s);
                        latch.countDown();
                    };
            long nearEndOfWindow = baseTime + THROTTLE_MS - 1;
            throttler.submit("update-2", latchConsumer, nearEndOfWindow);
            assertEquals(1, received.size());

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(2, received.size());
            assertEquals("update-2", received.get(1));
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testNewerPendingUpdateReplacesPrevious() throws Exception {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            List<String> received = Collections.synchronizedList(new ArrayList<>());
            long baseTime = 10000L;

            throttler.submit("update-1", received::add, baseTime);
            assertEquals(1, received.size());

            Consumer<String> latchConsumer =
                    s -> {
                        received.add(s);
                        latch.countDown();
                    };

            throttler.submit("update-2", latchConsumer, baseTime + 2000);
            long nearEnd = baseTime + THROTTLE_MS - 1;
            throttler.submit("update-3", latchConsumer, nearEnd);

            assertTrue(latch.await(2, TimeUnit.SECONDS));

            assertEquals(2, received.size());
            assertEquals("update-1", received.get(0));
            assertEquals("update-3", received.get(1));
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testFlushPublishesPendingUpdate() {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            List<String> received = new ArrayList<>();
            long baseTime = 10000L;

            throttler.submit("update-1", received::add, baseTime);
            assertEquals(1, received.size());

            throttler.submit("update-2", received::add, baseTime + 1000);
            assertEquals(1, received.size());

            throttler.flush();
            assertEquals(2, received.size());
            assertEquals("update-2", received.get(1));
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testFlushWithNoPendingUpdateIsNoOp() {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            List<String> received = new ArrayList<>();
            long baseTime = 10000L;

            throttler.submit("update-1", received::add, baseTime);
            assertEquals(1, received.size());

            throttler.flush();
            assertEquals(1, received.size());
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testResetAllowsImmediatePublishAgain() {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            List<String> received = new ArrayList<>();
            long baseTime = 10000L;

            throttler.submit("update-1", received::add, baseTime);
            assertEquals(1, received.size());

            throttler.submit("update-2", received::add, baseTime + 100);
            assertEquals(1, received.size());

            throttler.reset();

            throttler.submit("update-3", received::add, baseTime + 200);
            assertEquals(2, received.size());
            assertEquals("update-3", received.get(1));
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testSubmitWithoutInitThrows() {
        ProgressThrottler<String> throttler = new ProgressThrottler<>(THROTTLE_MS, "test");
        assertThrows(IllegalStateException.class, () -> throttler.submit("x", s -> {}, 10000L));
    }

    @Test
    public void testDoubleInitThrows() {
        ProgressThrottler<String> throttler = createThrottler();
        try {
            assertThrows(IllegalStateException.class, throttler::init);
        } finally {
            throttler.shutdown();
        }
    }
}
