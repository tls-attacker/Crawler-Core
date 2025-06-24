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
 * Functional interface for consuming scan job completion notifications. Implementations can process
 * completed scan jobs for various purposes such as persisting results or triggering follow-up
 * actions.
 */
@FunctionalInterface
public interface DoneNotificationConsumer {

    /**
     * Consumes a notification that a scan job has been completed.
     *
     * @param consumerTag the tag identifying the consumer
     * @param scanJobDescription the description of the completed scan job
     */
    void consumeDoneNotification(String consumerTag, ScanJobDescription scanJobDescription);
}
