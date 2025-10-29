/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import java.util.List;

/**
 * Interface for providers that supply lists of targets to scan. Implementations can retrieve
 * targets from different sources such as files, databases, or web services.
 */
public interface ITargetListProvider {

    /**
     * Gets the list of targets to scan.
     *
     * @return A list of target hostnames or IP addresses
     */
    List<String> getTargetList();
}
