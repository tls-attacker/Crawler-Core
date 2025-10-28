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

public class BulkScanJobCounters {

    private final BulkScan bulkScan;

    private final AtomicInteger totalJobDoneCount = new AtomicInteger(0);
    private final Map<JobStatus, AtomicInteger> jobStatusCounters = new EnumMap<>(JobStatus.class);

    /**
     * Creates a new BulkScanJobCounters for the given bulk scan.
     *
     * @param bulkScan the bulk scan to track job counters for
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
     * @return the bulk scan
     */
    public BulkScan getBulkScan() {
        return bulkScan;
    }

    /**
     * Gets a copy of the job status counters map.
     *
     * @return a copy of the job status counters
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
     * @param jobStatus the job status to get the count for
     * @return the count for the specified job status
     */
    public int getJobStatusCount(JobStatus jobStatus) {
        return jobStatusCounters.get(jobStatus).get();
    }

    /**
     * Increments the count for a specific job status.
     *
     * @param jobStatus the job status to increment the count for
     * @return the new total job done count
     */
    public int increaseJobStatusCount(JobStatus jobStatus) {
        jobStatusCounters.get(jobStatus).incrementAndGet();
        return totalJobDoneCount.incrementAndGet();
    }
}
