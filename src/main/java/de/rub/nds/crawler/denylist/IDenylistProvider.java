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
 * Provider interface for managing denylisted scan targets.
 * Implementations determine which targets should be excluded from scanning.
 */
public interface IDenylistProvider {

    /**
     * Checks if a target is denylisted and should be excluded from scanning.
     *
     * @param target the scan target to check
     * @return true if the target is denylisted, false otherwise
     */
    boolean isDenylisted(ScanTarget target);
}
