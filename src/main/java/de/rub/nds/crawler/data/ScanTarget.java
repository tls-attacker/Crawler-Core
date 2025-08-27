/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScanTarget implements Serializable {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Initializes a ScanTarget object from a string that potentially contains a hostname, an ip, a
     * port, the tranco rank.
     *
     * @param targetString from which to create the ScanTarget object
     * @param defaultPort that used if no port is present in targetString
     * @param denylistProvider which provides info if a host is denylisted
     * @return ScanTarget object
     */
    public static Pair<ScanTarget, JobStatus> fromTargetString(
            String targetString, int defaultPort, IDenylistProvider denylistProvider) {
        ScanTarget target = new ScanTarget();

        // check if targetString contains rank (e.g. "1,example.com")
        if (targetString.contains(",")) {
            if (targetString.split(",")[0].chars().allMatch(Character::isDigit)) {
                target.setTrancoRank(Integer.parseInt(targetString.split(",")[0]));
                targetString = targetString.split(",")[1];
            } else {
                // Testing for format: hostname, optional IP with optional port
                String[] parts = targetString.split(",", -1); // keep empty strings
                if (parts.length == 2) {
                    String[] hostnameParts = parts[0].split(":");
                    target.setHostname(hostnameParts[0]);
                    if (hostnameParts.length > 1) {
                        target.setPort(Integer.parseInt(hostnameParts[1]));
                    }
                    targetString = parts[1];
                    if (targetString.trim().isEmpty()) {
                        try {
                            target.setIp(
                                    InetAddress.getByName(target.getHostname()).getHostAddress());
                        } catch (UnknownHostException e) {
                            return Pair.of(target, JobStatus.UNRESOLVABLE);
                        }
                    }
                }
            }
        }

        // Formatting for MX hosts
        if (targetString.contains("//")) {
            targetString = targetString.split("//")[1];
        }
        if (targetString.startsWith("\"") && targetString.endsWith("\"")) {
            targetString = targetString.replace("\"", "");
            System.out.println(targetString);
        }

        // check if targetString contains port (e.g. "www.example.com:8080" or "[2001:db8::1]:8080")
        // Handle IPv6 addresses with ports (enclosed in brackets)
        if (targetString.startsWith("[") && targetString.contains("]:")) {
            int bracketEnd = targetString.indexOf("]:");
            String ipv6Address = targetString.substring(1, bracketEnd);
            String portString = targetString.substring(bracketEnd + 2);
            try {
                int port = Integer.parseInt(portString);
                if (port > 1 && port < 65535) {
                    target.setPort(port);
                } else {
                    target.setPort(defaultPort);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid port number: {}", portString);
                target.setPort(defaultPort);
            }
            targetString = ipv6Address; // Always extract the IPv6 address
        } else if (targetString.contains(":")) {
            // Check if it's an IPv6 address without port or IPv4/hostname with port
            String[] parts = targetString.split(":");
            if (parts.length == 2 && !targetString.contains("::")) {
                // Likely IPv4 or hostname with port
                try {
                    int port = Integer.parseInt(parts[1]);
                    if (port > 1 && port < 65535) {
                        target.setPort(port);
                    } else {
                        target.setPort(defaultPort);
                    }
                    targetString = parts[0]; // Always extract the address part
                } catch (NumberFormatException e) {
                    // Not a valid port, treat the whole string as address
                    target.setPort(defaultPort);
                }
            } else {
                // Multiple colons or "::" - likely an IPv6 address without port
                target.setPort(defaultPort);
            }
        } else {
            target.setPort(defaultPort);
        }

        if (InetAddressValidator.getInstance().isValid(targetString)) {
            target.setIp(targetString);
        } else if (!targetString.trim().isEmpty()) {
            target.setHostname(targetString);
            try {
                // TODO this only allows one IP per hostname; it may be interesting to scan all IPs
                // for a domain, or at least one v4 and one v6
                target.setIp(InetAddress.getByName(targetString).getHostAddress());
            } catch (UnknownHostException e) {
                LOGGER.error(
                        "Host {} is unknown or can not be reached with error {}.", targetString, e);
                // TODO in the current design we discard the exception info; maybe we want to keep
                // this in the future
                return Pair.of(target, JobStatus.UNRESOLVABLE);
            }
        }
        if (denylistProvider != null && denylistProvider.isDenylisted(target)) {
            LOGGER.error("Host {} is denylisted and will not be scanned.", targetString);
            // TODO similar to the unknownHostException, we do not keep any information as to why
            // the target is blocklisted it may be nice to distinguish cases where the domain is
            // blocked or where the IP is blocked
            return Pair.of(target, JobStatus.DENYLISTED);
        }
        return Pair.of(target, JobStatus.TO_BE_EXECUTED);
    }

    private String ip;

    private String hostname;

    private int port;

    private int trancoRank;

    public ScanTarget() {}

    @Override
    public String toString() {
        return hostname != null ? hostname : ip;
    }

    public String getIp() {
        return this.ip;
    }

    public String getHostname() {
        return this.hostname;
    }

    public int getPort() {
        return this.port;
    }

    public int getTrancoRank() {
        return this.trancoRank;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTrancoRank(int trancoRank) {
        this.trancoRank = trancoRank;
    }
}
