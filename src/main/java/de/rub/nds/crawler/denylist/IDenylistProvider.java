/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.denylist;

import de.rub.nds.crawler.data.ScanTarget;

/**
 * Interface for providers that check if scan targets are on a denylist. Implementations can use
 * various sources to determine if a target should be excluded from scanning.
 */
public interface IDenylistProvider {

    /**
     * Checks if the specified scan target is on the denylist.
     *
     * @param target the scan target to check
     * @return true if the target is denylisted and should not be scanned, false otherwise
     */
    boolean isDenylisted(ScanTarget target);
}
