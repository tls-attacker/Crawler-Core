/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.orchestration;

import de.rub.nds.crawler.data.ScanJobDescription;

/**
 * Functional interface for consumers that process scan jobs. Used by workers to receive jobs from
 * the orchestration system.
 */
@FunctionalInterface
public interface ScanJobConsumer {

    /**
     * Consumes and processes a scan job.
     *
     * @param scanJobDescription The description of the scan job to process
     */
    void consumeScanJob(ScanJobDescription scanJobDescription);
}
