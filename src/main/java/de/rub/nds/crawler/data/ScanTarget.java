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

    private String ip;

    private String hostname;

    private int port;

    private int trancoRank;

    /** Creates a new scan target with default values. */
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
     * Gets the IP address of the scan target.
     *
     * @return the IP address
     */
    public String getIp() {
        return this.ip;
    }

    /**
     * Gets the hostname of the scan target.
     *
     * @return the hostname
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Gets the port number of the scan target.
     *
     * @return the port number
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Gets the Tranco rank of the scan target.
     *
     * @return the Tranco rank
     */
    public int getTrancoRank() {
        return this.trancoRank;
    }

    /**
     * Sets the IP address of the scan target.
     *
     * @param ip the IP address to set
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Sets the hostname of the scan target.
     *
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets the port number of the scan target.
     *
     * @param port the port number to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets the Tranco rank of the scan target.
     *
     * @param trancoRank the Tranco rank to set
     */
    public void setTrancoRank(int trancoRank) {
        this.trancoRank = trancoRank;
    }
}
