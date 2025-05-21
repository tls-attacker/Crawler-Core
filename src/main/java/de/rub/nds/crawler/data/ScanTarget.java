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
 * Represents a target to be scanned by the crawler. Contains information about the hostname, IP
 * address, port, and ranking information. This class is used to track targets throughout the
 * scanning process.
 */
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

    /** The IP address of the target. */
    private String ip;

    /** The hostname of the target. */
    private String hostname;

    /** The port number to connect to. */
    private int port;

    /** The Tranco rank of the target (if applicable). */
    private int trancoRank;

    /** Creates a new empty scan target. Fields should be set using the setter methods. */
    public ScanTarget() {}

    /**
     * Returns a string representation of this scan target. Uses the hostname if available,
     * otherwise uses the IP address.
     *
     * @return The string representation
     */
    @Override
    public String toString() {
        return hostname != null ? hostname : ip;
    }

    /**
     * Gets the IP address of this target.
     *
     * @return The IP address
     */
    public String getIp() {
        return this.ip;
    }

    /**
     * Gets the hostname of this target.
     *
     * @return The hostname
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Gets the port number to connect to.
     *
     * @return The port number
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Gets the Tranco rank of this target (if applicable).
     *
     * @return The Tranco rank
     */
    public int getTrancoRank() {
        return this.trancoRank;
    }

    /**
     * Sets the IP address of this target.
     *
     * @param ip The IP address
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Sets the hostname of this target.
     *
     * @param hostname The hostname
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets the port number to connect to.
     *
     * @param port The port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets the Tranco rank of this target.
     *
     * @param trancoRank The Tranco rank
     */
    public void setTrancoRank(int trancoRank) {
        this.trancoRank = trancoRank;
    }
}
