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
 * Provider interface for obtaining scan target lists.
 * Implementations supply lists of targets to be scanned.
 */
public interface ITargetListProvider {

    /**
     * Retrieves the list of targets to be scanned.
     *
     * @return a list of target identifiers (e.g., hostnames, IP addresses)
     */
    List<String> getTargetList();
}
