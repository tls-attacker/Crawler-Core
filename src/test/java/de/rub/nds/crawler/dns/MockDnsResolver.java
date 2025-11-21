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
import java.util.HashMap;
import java.util.Map;

/**
 * Mock DNS resolver for testing purposes. Allows configuring hostname-to-IP mappings and
 * controlling whether a hostname should throw an UnknownHostException.
 */
public class MockDnsResolver implements DnsResolver {
    
    private final Map<String, String> hostnameToIpMap = new HashMap<>();
    private final Map<String, Boolean> hostnameToUnresolvableMap = new HashMap<>();
    
    /**
     * Adds a mapping from hostname to IP address.
     *
     * @param hostname the hostname to map
     * @param ipAddress the IP address to return for this hostname
     */
    public void addMapping(String hostname, String ipAddress) {
        hostnameToIpMap.put(hostname, ipAddress);
        hostnameToUnresolvableMap.put(hostname, false);
    }
    
    /**
     * Configures a hostname to throw UnknownHostException when resolved.
     *
     * @param hostname the hostname that should be unresolvable
     */
    public void addUnresolvableHost(String hostname) {
        hostnameToUnresolvableMap.put(hostname, true);
    }
    
    @Override
    public String resolveHostname(String hostname) throws UnknownHostException {
        if (hostnameToUnresolvableMap.getOrDefault(hostname, false)) {
            throw new UnknownHostException("Mock: hostname is unresolvable: " + hostname);
        }
        
        String ipAddress = hostnameToIpMap.get(hostname);
        if (ipAddress != null) {
            return ipAddress;
        }
        
        // If no mapping is found, throw UnknownHostException
        throw new UnknownHostException("Mock: no mapping configured for hostname: " + hostname);
    }
}
