/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.scanner.core.execution.NamedThreadFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Generic throttler for progress updates. Ensures that updates are published at most once per
 * throttle window. When an update arrives during the throttle period, it is saved as pending and
 * scheduled to be published after the period ends. If another update arrives before the scheduled
 * publish, it replaces the pending update, ensuring the latest state is always published.
 *
 * <p>Lifecycle: call {@link #init()} before submitting updates and {@link #shutdown()} when done.
 *
 * @param <T> the type of the progress payload
 */
public class ProgressThrottler<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Default throttle interval in milliseconds. */
    public static final long DEFAULT_THROTTLE_MS = 5000;

    private final long throttleMs;
    private final String threadName;

    private long lastUpdateTime = 0;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pendingTask;
    private T pendingPayload;
    private Consumer<T> pendingConsumer;

    /**
     * Creates a new throttler.
     *
     * @param throttleMs minimum interval between published updates in milliseconds
     * @param threadName name for the scheduler thread
     */
    public ProgressThrottler(long throttleMs, String threadName) {
        this.throttleMs = throttleMs;
        this.threadName = threadName;
    }

    /** Initializes the internal scheduler. Must be called before {@link #submit}. */
    public synchronized void init() {
        if (scheduler != null) {
            throw new IllegalStateException("ProgressThrottler already initialized");
        }
        scheduler = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(threadName));
    }

    /** Shuts down the scheduler and clears all pending state. */
    public synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        pendingPayload = null;
        pendingConsumer = null;
        pendingTask = null;
    }

    /**
     * Submits a progress update through the throttle mechanism. If enough time has elapsed since
     * the last update, publishes immediately. Otherwise, saves as pending and schedules publication
     * after the throttle period.
     *
     * @param payload the progress payload to publish
     * @param consumer the consumer to publish to
     * @param currentTime the current timestamp in milliseconds
     */
    public synchronized void submit(T payload, Consumer<T> consumer, long currentTime) {
        if (scheduler == null) {
            throw new IllegalStateException(
                    "ProgressThrottler not initialized. Call init() first.");
        }
        if (currentTime - lastUpdateTime >= throttleMs) {
            publish(payload, consumer, currentTime);
        } else {
            pendingPayload = payload;
            pendingConsumer = consumer;

            if (pendingTask != null && !pendingTask.isDone()) {
                pendingTask.cancel(false);
            }

            long delay = throttleMs - (currentTime - lastUpdateTime);
            pendingTask = scheduler.schedule(this::publishPending, delay, TimeUnit.MILLISECONDS);
            LOGGER.debug("Progress update throttled, scheduled for {}ms", delay);
        }
    }

    /**
     * Flushes any pending throttled update immediately. Should be called after a scan completes to
     * ensure the last update is not lost when the scheduler is shut down.
     */
    public synchronized void flush() {
        if (pendingPayload != null && pendingConsumer != null) {
            publish(pendingPayload, pendingConsumer, System.currentTimeMillis());
        }
        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
            pendingTask = null;
        }
    }

    /** Resets throttle state between scans. */
    public synchronized void reset() {
        lastUpdateTime = 0;
        pendingPayload = null;
        pendingConsumer = null;
        if (pendingTask != null) {
            pendingTask.cancel(false);
            pendingTask = null;
        }
    }

    /** Returns the configured throttle interval in milliseconds. */
    public long getThrottleMs() {
        return throttleMs;
    }

    private void publish(T payload, Consumer<T> consumer, long currentTime) {
        consumer.accept(payload);
        lastUpdateTime = currentTime;
        pendingPayload = null;
        pendingConsumer = null;
        LOGGER.debug("Progress update published");
    }

    private synchronized void publishPending() {
        if (pendingPayload != null && pendingConsumer != null) {
            publish(pendingPayload, pendingConsumer, System.currentTimeMillis());
        }
    }
}
