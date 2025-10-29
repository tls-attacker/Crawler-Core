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
 * Interface for providers that check if a scan target is on a denylist. This can be used to skip
 * scanning of certain targets for various reasons (legal, ethical, or technical).
 */
public interface IDenylistProvider {

    /**
     * Checks if a scan target is on the denylist.
     *
     * @param target The scan target to check
     * @return True if the target is denylisted, false otherwise
     */
    boolean isDenylisted(ScanTarget target);
}
