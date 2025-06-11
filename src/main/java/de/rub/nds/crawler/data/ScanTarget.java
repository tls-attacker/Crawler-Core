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

/**
 * Represents a target for TLS scanning operations.
 *
 * <p>A scan target encapsulates the network location (hostname/IP address and port) and optional
 * metadata (such as Tranco ranking) for a host to be scanned. This class provides parsing
 * functionality to extract target information from various string formats commonly found in target
 * lists and rankings.
 *
 * <p>Supported target string formats:
 *
 * <ul>
 *   <li><code>example.com</code> - hostname only
 *   <li><code>192.168.1.1</code> - IP address only
 *   <li><code>example.com:8080</code> - hostname with port
 *   <li><code>192.168.1.1:443</code> - IP address with port
 *   <li><code>1,example.com</code> - Tranco rank with hostname
 *   <li><code>//example.com</code> - hostname with URL prefix
 * </ul>
 *
 * <p>The class performs hostname resolution and denylist checking during target creation. IPv6
 * addresses are currently not fully supported due to port parsing limitations.
 *
 * @see JobStatus
 * @see IDenylistProvider
 */
public class ScanTarget implements Serializable {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a ScanTarget from a target string with comprehensive parsing and validation.
     *
     * <p>This method parses various target string formats, performs hostname resolution, and checks
     * against denylists. The parsing handles multiple formats including Tranco-ranked entries,
     * URLs, and port specifications.
     *
     * <p>Parsing logic:
     *
     * <ol>
     *   <li>Extract Tranco rank if present (format: "rank,hostname")
     *   <li>Remove URL prefixes ("//hostname")
     *   <li>Remove quotes around hostnames
     *   <li>Extract port number if specified ("hostname:port")
     *   <li>Determine if target is IP address or hostname
     *   <li>Resolve hostname to IP address if needed
     *   <li>Check against denylist if provider is available
     * </ol>
     *
     * <p><strong>Known limitations:</strong>
     *
     * <ul>
     *   <li>IPv6 addresses with ports are not correctly parsed due to colon conflicts
     *   <li>Only the first resolved IP address is used for multi-homed hosts
     * </ul>
     *
     * @param targetString the string to parse (supports various formats as documented in class
     *     description)
     * @param defaultPort the port to use when none is specified in the target string
     * @param denylistProvider optional provider for checking if targets are denylisted (may be
     *     null)
     * @return a pair containing the created ScanTarget and its status (TO_BE_EXECUTED,
     *     UNRESOLVABLE, or DENYLISTED)
     * @throws NumberFormatException if port or rank parsing fails
     * @see JobStatus
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
                targetString = "";
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

        // check if targetString contains port (e.g. "www.example.com:8080")
        // FIXME I guess this breaks any IPv6 parsing
        if (targetString.contains(":")) {
            int port = Integer.parseInt(targetString.split(":")[1]);
            targetString = targetString.split(":")[0];
            if (port > 1 && port < 65535) {
                target.setPort(port);
            }
        } else {
            target.setPort(defaultPort);
        }

        if (InetAddressValidator.getInstance().isValid(targetString)) {
            target.setIp(targetString);
        } else {
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

    /** The resolved IP address of the target host. */
    private String ip;

    /** The hostname of the target (may be null if target was specified as IP address). */
    private String hostname;

    /** The port number for the scan target. */
    private int port;

    /** The Tranco ranking of the target (0 if not available or not specified). */
    private int trancoRank;

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
}
