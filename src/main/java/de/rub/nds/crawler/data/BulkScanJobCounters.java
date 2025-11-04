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
 * Counter class for tracking job statistics during a bulk scan. This class maintains thread-safe
 * counters for each job status type. Used to track statistics of finished jobs during a bulk scan.
 */
public class BulkScanJobCounters {

    private final BulkScan bulkScan;

    private final AtomicInteger totalJobDoneCount = new AtomicInteger(0);
    private final Map<JobStatus, AtomicInteger> jobStatusCounters = new EnumMap<>(JobStatus.class);

    /**
     * Creates a new BulkScanJobCounters instance for the given bulk scan. Initializes counters for
     * all job statuses except TO_BE_EXECUTED (i.e., for all done jobs).
     *
     * @param bulkScan The bulk scan to track counters for
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
     * Gets the bulk scan associated with these counters.
     *
     * @return The bulk scan
     */
    public BulkScan getBulkScan() {
        return bulkScan;
    }

    /**
     * Gets a copy of the job status counters as a non-atomic map. This creates a snapshot of the
     * current counter values.
     *
     * @return A map of job status to count
     */
    public Map<JobStatus, Integer> getJobStatusCountersCopy() {
        EnumMap<JobStatus, Integer> ret = new EnumMap<>(JobStatus.class);
        for (Map.Entry<JobStatus, AtomicInteger> entry : jobStatusCounters.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().get());
        }
        return ret;
    }

    /**
     * Gets the count for a specific job status.
     *
     * @param jobStatus The job status to get the count for
     * @return The current count for the given status
     */
    public int getJobStatusCount(JobStatus jobStatus) {
        return jobStatusCounters.get(jobStatus).get();
    }

    /**
     * Increments the count for a specific job status and the total job count.
     *
     * @param jobStatus The job status to increment the count for
     * @return The new total job count after incrementing
     */
    public int increaseJobStatusCount(JobStatus jobStatus) {
        jobStatusCounters.get(jobStatus).incrementAndGet();
        return totalJobDoneCount.incrementAndGet();
    }
}
