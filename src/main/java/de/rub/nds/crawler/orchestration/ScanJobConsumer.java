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

@FunctionalInterface
public interface ScanJobConsumer {

    /**
     * Consumes a scan job for processing.
     *
     * @param scanJobDescription The scan job to be consumed and processed
     */
    void consumeScanJob(ScanJobDescription scanJobDescription);
}
