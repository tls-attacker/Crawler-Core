/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.dns;

import java.net.UnknownHostException;

/**
 * Interface for DNS resolution. Allows for mocking and testing of DNS lookups.
 */
public interface DnsResolver {
    /**
     * Resolves a hostname to its IP address.
     *
     * @param hostname the hostname to resolve
     * @return the IP address as a string
     * @throws UnknownHostException if the hostname cannot be resolved
     */
    String resolveHostname(String hostname) throws UnknownHostException;
}
