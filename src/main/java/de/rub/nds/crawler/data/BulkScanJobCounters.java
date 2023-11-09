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

    public BulkScanJobCounters(BulkScan bulkScan) {
        this.bulkScan = bulkScan;
        for (JobStatus jobStatus : JobStatus.values()) {
            if (jobStatus == JobStatus.TO_BE_EXECUTED) {
                continue;
            }
            jobStatusCounters.put(jobStatus, new AtomicInteger(0));
        }
    }

    public BulkScan getBulkScan() {
        return bulkScan;
    }

    public Map<JobStatus, Integer> getJobStatusCountersCopy() {
        EnumMap<JobStatus, Integer> ret = new EnumMap<>(JobStatus.class);
        for (Map.Entry<JobStatus, AtomicInteger> entry : jobStatusCounters.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().get());
        }
        return ret;
    }

    public int getJobStatusCount(JobStatus jobStatus) {
        return jobStatusCounters.get(jobStatus).get();
    }

    public int increaseJobStatusCount(JobStatus jobStatus) {
        jobStatusCounters.get(jobStatus).incrementAndGet();
        return totalJobDoneCount.incrementAndGet();
    }
}
