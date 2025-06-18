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
 * Functional interface for consuming scan jobs from the orchestration provider.
 * Implementations process individual scan job descriptions.
 */
@FunctionalInterface
public interface ScanJobConsumer {

    /**
     * Consumes and processes a scan job description.
     *
     * @param scanJobDescription the scan job to be processed
     */
    void consumeScanJob(ScanJobDescription scanJobDescription);
}
