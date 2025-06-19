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
 * Interface for providers that supply lists of scan targets. Implementations can retrieve target
 * lists from various sources such as web services, local files, or databases.
 */
public interface ITargetListProvider {

    /**
     * Gets the list of scan targets.
     *
     * @return a list of target identifiers (e.g., hostnames, IP addresses)
     */
    List<String> getTargetList();
}
