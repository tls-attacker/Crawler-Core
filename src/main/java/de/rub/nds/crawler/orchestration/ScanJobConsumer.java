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
 * Functional interface for consuming scan job descriptions. Implementations typically execute the
 * scan described by the job description.
 */
@FunctionalInterface
public interface ScanJobConsumer {

    /**
     * Consumes a scan job description for processing.
     *
     * @param scanJobDescription the scan job to be consumed and processed
     */
    void consumeScanJob(ScanJobDescription scanJobDescription);
}
