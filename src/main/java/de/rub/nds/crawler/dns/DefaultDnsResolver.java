/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Default DNS resolver implementation that uses the standard Java DNS resolution via InetAddress.
 */
public class DefaultDnsResolver implements DnsResolver {

    /** Creates a new default DNS resolver instance. */
    public DefaultDnsResolver() {}

    @Override
    public String resolveHostname(String hostname) throws UnknownHostException {
        return InetAddress.getByName(hostname).getHostAddress();
    }
}
