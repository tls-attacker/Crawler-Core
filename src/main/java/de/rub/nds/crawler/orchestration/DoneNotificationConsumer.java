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
 * Functional interface for consumers that handle completion notifications of scan jobs. Used to
 * notify controllers when workers have completed their assigned tasks.
 */
@FunctionalInterface
public interface DoneNotificationConsumer {

    /**
     * Consumes a notification that a scan job has completed.
     *
     * @param consumerTag A tag identifying the consumer
     * @param scanJobDescription The description of the completed scan job
     */
    void consumeDoneNotification(String consumerTag, ScanJobDescription scanJobDescription);
}
