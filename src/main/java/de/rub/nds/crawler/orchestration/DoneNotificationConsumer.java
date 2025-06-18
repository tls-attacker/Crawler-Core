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
 * Functional interface for consuming completion notifications for scan jobs.
 * Implementations handle notifications when scan jobs are completed.
 */
@FunctionalInterface
public interface DoneNotificationConsumer {

    /**
     * Consumes a notification that a scan job has been completed.
     *
     * @param consumerTag the consumer tag identifying the notification source
     * @param scanJobDescription the completed scan job description
     */
    void consumeDoneNotification(String consumerTag, ScanJobDescription scanJobDescription);
}
