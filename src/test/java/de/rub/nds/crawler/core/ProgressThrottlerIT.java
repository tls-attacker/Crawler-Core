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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration tests that exercise the full throttler lifecycle: init, rapid updates, flush,
 * shutdown.
 */
@Tag("IntegrationTest")
public class ProgressThrottlerIT {

    @Test
    public void testFullLifecycleFlushPublishesPendingBeforeShutdown() {
        long highThrottleMs = 180_000;
        ProgressThrottler<String> throttler =
                new ProgressThrottler<>(highThrottleMs, "it-throttler");
        throttler.init();

        List<String> received = Collections.synchronizedList(new ArrayList<>());
        // baseTime must be >= throttleMs so first update publishes immediately
        long baseTime = 200_000L;

        try {
            int totalUpdates = 20;
            for (int i = 1; i <= totalUpdates; i++) {
                throttler.submit("update-" + i, received::add, baseTime + i);
            }

            assertEquals(1, received.size(), "Only the first update should publish immediately");
            assertEquals("update-1", received.get(0));

            throttler.flush();

            assertEquals(2, received.size(), "Flush should have published the pending update");
            assertEquals("update-20", received.get(1));

            throttler.flush();
            assertEquals(2, received.size(), "Second flush should be a no-op");
        } finally {
            throttler.shutdown();
        }
    }

    @Test
    public void testFlushBeforeShutdownPreventsDataloss() {
        long throttleMs = 60_000;
        ProgressThrottler<String> throttler = new ProgressThrottler<>(throttleMs, "it-throttler-2");
        throttler.init();

        List<String> received = Collections.synchronizedList(new ArrayList<>());
        long baseTime = 100_000L;

        try {
            throttler.submit("first", received::add, baseTime);
            assertEquals(1, received.size());

            throttler.submit("middle", received::add, baseTime + 100);
            throttler.submit("last", received::add, baseTime + 200);
            assertEquals(1, received.size(), "Throttled updates should not publish yet");

            throttler.flush();
            assertEquals(2, received.size());
            assertEquals("last", received.get(1));
        } finally {
            throttler.shutdown();
            assertFalse(received.isEmpty());
        }
    }
}
