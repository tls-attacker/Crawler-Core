/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import de.rub.nds.crawler.constant.JobStatus;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe job status counters for tracking bulk scan progress and completion statistics.
 *
 * <p>The BulkScanJobCounters class provides atomic counting and tracking of scan job completion
 * status across all worker threads in a distributed TLS scanning operation. It maintains separate
 * counters for each job status type and provides thread-safe access to progress metrics used by the
 * monitoring and progress tracking systems.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li><strong>Thread Safety</strong> - Uses AtomicInteger for concurrent access from multiple
 *       threads
 *   <li><strong>Status Categorization</strong> - Separate counters for each JobStatus enum value
 *   <li><strong>Total Tracking</strong> - Maintains overall completion count across all statuses
 *   <li><strong>Progress Monitoring</strong> - Provides real-time statistics for ProgressMonitor
 * </ul>
 *
 * <p><strong>Atomic Operations:</strong>
 *
 * <ul>
 *   <li><strong>Status Increment</strong> - Thread-safe increment of specific job status counters
 *   <li><strong>Total Increment</strong> - Synchronized increment of overall completion count
 *   <li><strong>Snapshot Access</strong> - Thread-safe reading of current counter values
 * </ul>
 *
 * <p><strong>Status Categories Tracked:</strong>
 *
 * <ul>
 *   <li><strong>SUCCESS</strong> - Scan completed successfully with results
 *   <li><strong>EMPTY</strong> - Scan completed but produced no results
 *   <li><strong>ERROR</strong> - Scanner-level execution failure
 *   <li><strong>CANCELLED</strong> - Scan timed out and was cancelled
 *   <li><strong>INTERNAL_ERROR</strong> - Worker-level processing failure
 *   <li><strong>SERIALIZATION_ERROR</strong> - Result serialization failure
 *   <li><strong>CRAWLER_ERROR</strong> - Unexpected crawler exception
 * </ul>
 *
 * <p><strong>Excluded Status:</strong> The TO_BE_EXECUTED status is not tracked as it represents
 * jobs that haven't completed yet, and this class only tracks completion statistics.
 *
 * <p><strong>Performance Metrics:</strong> The counters support real-time calculation of completion
 * rates, error rates, and progress percentages for monitoring dashboards and ETA calculations.
 *
 * <p><strong>Memory Efficiency:</strong> Uses EnumMap for optimal memory usage and access speed
 * when dealing with the finite set of JobStatus enum values.
 *
 * @see BulkScan
 * @see JobStatus Used by ProgressMonitor for tracking bulk scan completion statistics.
 * @see AtomicInteger
 */
public class BulkScanJobCounters {

    private final BulkScan bulkScan;

    private final AtomicInteger totalJobDoneCount = new AtomicInteger(0);
    private final Map<JobStatus, AtomicInteger> jobStatusCounters = new EnumMap<>(JobStatus.class);

    /**
     * Creates a new job counter tracker for the specified bulk scan.
     *
     * <p>This constructor initializes atomic counters for all completion status types, excluding
     * TO_BE_EXECUTED which represents jobs that haven't completed yet. Each counter starts at zero
     * and is thread-safe for concurrent updates.
     *
     * <p><strong>Counter Initialization:</strong> Creates AtomicInteger instances for each
     * JobStatus enum value except TO_BE_EXECUTED, ensuring thread-safe access from multiple worker
     * threads and monitoring components.
     *
     * @param bulkScan the bulk scan operation to track counters for
     */
    public BulkScanJobCounters(BulkScan bulkScan) {
        this.bulkScan = bulkScan;
        for (JobStatus jobStatus : JobStatus.values()) {
            if (jobStatus == JobStatus.TO_BE_EXECUTED) {
                continue;
            }
            jobStatusCounters.put(jobStatus, new AtomicInteger(0));
        }
    }

    /**
     * Gets the bulk scan operation that these counters are tracking.
     *
     * @return the associated bulk scan object
     */
    public BulkScan getBulkScan() {
        return bulkScan;
    }

    /**
     * Creates a snapshot copy of all job status counters at the current moment.
     *
     * <p>This method provides a thread-safe way to get a consistent view of all counter values
     * without holding locks. The returned map contains the current count for each job status type
     * and can be safely used for reporting or persistence without affecting the ongoing counter
     * updates.
     *
     * <p><strong>Thread Safety:</strong> While individual counter reads are atomic, the overall
     * snapshot may not be perfectly consistent if updates occur during iteration. However, this
     * provides a reasonable approximation for monitoring purposes.
     *
     * @return a new EnumMap containing current counter values for all job statuses
     */
    public Map<JobStatus, Integer> getJobStatusCountersCopy() {
        EnumMap<JobStatus, Integer> ret = new EnumMap<>(JobStatus.class);
        for (Map.Entry<JobStatus, AtomicInteger> entry : jobStatusCounters.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().get());
        }
        return ret;
    }

    /**
     * Gets the current count for a specific job status type.
     *
     * <p>This method provides thread-safe access to individual counter values, returning the
     * current count for the specified job status.
     *
     * @param jobStatus the job status type to get the count for
     * @return the current count for the specified job status
     * @throws NullPointerException if jobStatus is TO_BE_EXECUTED (not tracked)
     */
    public int getJobStatusCount(JobStatus jobStatus) {
        return jobStatusCounters.get(jobStatus).get();
    }

    /**
     * Atomically increments the counter for a specific job status and returns the new total.
     *
     * <p>This method performs two atomic operations: incrementing the specific job status counter
     * and incrementing the overall completion count. The operations are performed in sequence but
     * are individually atomic, ensuring thread safety but not perfect consistency between the two
     * counters at any given instant.
     *
     * <p><strong>Usage:</strong> Called by workers when scan jobs complete with a specific status,
     * providing real-time updates for progress monitoring and statistics.
     *
     * @param jobStatus the job status type to increment
     * @return the new total count of completed jobs across all status types
     * @throws NullPointerException if jobStatus is TO_BE_EXECUTED (not tracked)
     */
    public int increaseJobStatusCount(JobStatus jobStatus) {
        jobStatusCounters.get(jobStatus).incrementAndGet();
        return totalJobDoneCount.incrementAndGet();
    }
}
