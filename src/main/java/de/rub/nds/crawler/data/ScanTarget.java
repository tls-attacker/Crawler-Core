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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a target for TLS scanning operations.
 *
 * <p>Encapsulates network location (hostname/IP and port) and optional metadata (Tranco ranking).
 * Supports parsing various string formats: hostnames, IPs (IPv4/IPv6), ports, ranks, and URL
 * prefixes. Performs hostname resolution and denylist checking.
 *
 * @see JobStatus
 * @see IDenylistProvider
 */
public class ScanTarget implements Serializable {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates ScanTarget(s) from a target string with parsing and validation.
     *
     * <p>Parses various formats (rank,hostname, URLs, ports), performs hostname resolution, and
     * checks denylists. Creates separate targets for multi-homed hosts.
     *
     * @param targetString string to parse (hostname, IP, with optional rank/port)
     * @param defaultPort port to use when none specified
     * @param denylistProvider optional denylist checker (may be null)
     * @return list of (ScanTarget, JobStatus) pairs - multiple for multi-homed hosts
     * @throws NumberFormatException if port or rank parsing fails
     */
    public static List<Pair<ScanTarget, JobStatus>> fromTargetString(
            String targetString, int defaultPort, IDenylistProvider denylistProvider) {
        ScanTarget target = new ScanTarget();

        // check if targetString contains rank (e.g. "1,example.com")
        if (targetString.contains(",")) {
            if (targetString.split(",")[0].chars().allMatch(Character::isDigit)) {
                target.setTrancoRank(Integer.parseInt(targetString.split(",")[0]));
                targetString = targetString.split(",")[1];
            } else {
                targetString = "";
            }
        }

        // Formatting for MX hosts
        if (targetString.contains("//")) {
            targetString = targetString.split("//")[1];
        }
        if (targetString.startsWith("\"") && targetString.endsWith("\"")) {
            targetString = targetString.replace("\"", "");
        }

        // Parse port from target string, handling IPv6 addresses properly
        if (targetString.startsWith("[") && targetString.contains("]:")) {
            // IPv6 address with port: [2001:db8::1]:8080
            int bracketEnd = targetString.indexOf("]:") + 1;
            String portPart = targetString.substring(bracketEnd + 1);
            targetString = targetString.substring(1, bracketEnd - 1); // Remove brackets
            try {
                int port = Integer.parseInt(portPart);
                if (port > 0 && port <= 65535) {
                    target.setPort(port);
                } else {
                    target.setPort(defaultPort);
                }
            } catch (NumberFormatException e) {
                target.setPort(defaultPort);
            }
        } else if (targetString.contains(":")
                && !InetAddressValidator.getInstance().isValidInet6Address(targetString)) {
            // IPv4 address or hostname with port: www.example.com:8080 or 192.168.1.1:443
            String[] parts = targetString.split(":", 2);
            targetString = parts[0];
            try {
                int port = Integer.parseInt(parts[1]);
                if (port > 0 && port <= 65535) {
                    target.setPort(port);
                } else {
                    target.setPort(defaultPort);
                }
            } catch (NumberFormatException e) {
                target.setPort(defaultPort);
            }
        } else {
            // No port specified or IPv6 address without port
            target.setPort(defaultPort);
        }

        List<Pair<ScanTarget, JobStatus>> results = new ArrayList<>();

        if (InetAddressValidator.getInstance().isValid(targetString)) {
            // Direct IP address - create single target
            target.setIp(targetString);

            if (denylistProvider != null && denylistProvider.isDenylisted(target)) {
                LOGGER.error("IP {} is denylisted and will not be scanned.", targetString);

                // Store denylist rejection information
                target.setErrorMessage("Target blocked by denylist: IP address " + targetString);
                target.setErrorType("DenylistRejection");

                results.add(Pair.of(target, JobStatus.DENYLISTED));
            } else {
                results.add(Pair.of(target, JobStatus.TO_BE_EXECUTED));
            }
        } else {
            // Hostname - resolve to potentially multiple IPs
            target.setHostname(targetString);
            try {
                InetAddress[] addresses = InetAddress.getAllByName(targetString);
                LOGGER.debug(
                        "Resolved hostname {} to {} IP address(es)",
                        targetString,
                        addresses.length);

                for (InetAddress address : addresses) {
                    ScanTarget ipTarget = new ScanTarget();
                    ipTarget.setHostname(targetString);
                    ipTarget.setIp(address.getHostAddress());
                    ipTarget.setPort(target.getPort());
                    ipTarget.setTrancoRank(target.getTrancoRank());

                    if (denylistProvider != null && denylistProvider.isDenylisted(ipTarget)) {
                        LOGGER.error(
                                "IP {} for hostname {} is denylisted and will not be scanned.",
                                address.getHostAddress(),
                                targetString);

                        // Store detailed denylist rejection information
                        ipTarget.setErrorMessage(
                                "Target blocked by denylist: IP "
                                        + address.getHostAddress()
                                        + " for hostname "
                                        + targetString);
                        ipTarget.setErrorType("DenylistRejection");

                        results.add(Pair.of(ipTarget, JobStatus.DENYLISTED));
                    } else {
                        results.add(Pair.of(ipTarget, JobStatus.TO_BE_EXECUTED));
                    }
                }
            } catch (UnknownHostException e) {
                LOGGER.error(
                        "Host {} is unknown or can not be reached with error {}.", targetString, e);

                // Store detailed error information for debugging and analysis
                target.setErrorMessage("DNS resolution failed: " + e.getMessage());
                target.setErrorType("UnknownHostException");

                results.add(Pair.of(target, JobStatus.UNRESOLVABLE));
            }
        }

        return results;
    }

    /** The resolved IP address of the target host. */
    private String ip;

    /** The hostname of the target (may be null if target was specified as IP address). */
    private String hostname;

    /** The port number for the scan target. */
    private int port;

    /** The Tranco ranking of the target (0 if not available or not specified). */
    private int trancoRank;

    /** Error message for debugging when target processing fails (may be null). */
    private String errorMessage;

    /** Error type classification for debugging (may be null). */
    private String errorType;

    /**
     * Creates an empty ScanTarget.
     *
     * <p>All fields will be initialized to default values. This constructor is primarily used for
     * deserialization and testing purposes.
     */
    public ScanTarget() {}

    /**
     * Returns a string representation of the scan target.
     *
     * @return the hostname if available, otherwise the IP address
     */
    @Override
    public String toString() {
        return hostname != null ? hostname : ip;
    }

    /**
     * Gets the resolved IP address of the target.
     *
     * @return the IP address as a string
     */
    public String getIp() {
        return this.ip;
    }

    /**
     * Gets the hostname of the target.
     *
     * @return the hostname, or null if the target was specified as an IP address
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Gets the port number for the scan target.
     *
     * @return the port number (1-65534)
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Gets the Tranco ranking of the target.
     *
     * <p>The Tranco ranking is a research-oriented top sites ranking that provides a more stable
     * and transparent alternative to other web ranking services.
     *
     * @return the Tranco rank, or 0 if not available
     * @see <a href="https://tranco-list.eu/">Tranco: A Research-Oriented Top Sites Ranking</a>
     */
    public int getTrancoRank() {
        return this.trancoRank;
    }

    /**
     * Sets the IP address of the target.
     *
     * @param ip the IP address as a string (IPv4 or IPv6 format)
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Sets the hostname of the target.
     *
     * @param hostname the hostname (may be null if target is IP-only)
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets the port number for the scan target.
     *
     * @param port the port number (should be between 1 and 65534)
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets the Tranco ranking of the target.
     *
     * @param trancoRank the Tranco rank (use 0 if not available)
     */
    public void setTrancoRank(int trancoRank) {
        this.trancoRank = trancoRank;
    }

    /**
     * Gets the error message associated with this target.
     *
     * <p>The error message provides detailed information about why target processing failed,
     * including specific exception messages, DNS resolution failures, or parsing errors.
     *
     * @return the error message, or null if no error occurred
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * Sets the error message for this target.
     *
     * @param errorMessage the error message describing the failure
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets the error type classification for this target.
     *
     * <p>The error type provides a high-level classification of the failure type, such as
     * "UnknownHostException", "NumberFormatException", or "DenylistRejection".
     *
     * @return the error type, or null if no error occurred
     */
    public String getErrorType() {
        return this.errorType;
    }

    /**
     * Sets the error type classification for this target.
     *
     * @param errorType the error type classification
     */
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
}
