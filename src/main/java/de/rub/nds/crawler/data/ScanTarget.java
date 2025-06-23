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
import java.util.Collections;
import java.util.List;
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
            target.setIps(Collections.singletonList(targetString));
        } else {
            target.setHostname(targetString);
            try {
                // Resolve all IP addresses for the hostname
                InetAddress[] addresses = InetAddress.getAllByName(targetString);
                List<String> resolvedIps = new ArrayList<>();
                for (InetAddress address : addresses) {
                    resolvedIps.add(address.getHostAddress());
                }
                target.setIps(resolvedIps);
                // Set the first IP for backward compatibility
                if (!resolvedIps.isEmpty()) {
                    target.setIp(resolvedIps.get(0));
                }
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

    private List<String> ips;

    private String hostname;

    private int port;

    private int trancoRank;

    public ScanTarget() {
        this.ips = new ArrayList<>();
    }

    @Override
    public String toString() {
        if (hostname != null) {
            return hostname;
        } else if (!ips.isEmpty()) {
            // If multiple IPs, show all
            if (ips.size() > 1) {
                return "[" + String.join(", ", ips) + "]";
            } else {
                return ips.get(0);
            }
        } else {
            return ip;
        }
    }

    /**
     * @deprecated Use {@link #getIps()} instead to get all IP addresses. This method returns only
     *     the first IP for backward compatibility.
     */
    @Deprecated
    public String getIp() {
        return this.ip;
    }

    public List<String> getIps() {
        return new ArrayList<>(this.ips);
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

    /**
     * @deprecated Use {@link #setIps(List)} instead to set all IP addresses. This method is kept
     *     for backward compatibility.
     */
    @Deprecated
    public void setIp(String ip) {
        this.ip = ip;
        if (this.ips.isEmpty() || !this.ips.get(0).equals(ip)) {
            this.ips = new ArrayList<>();
            this.ips.add(ip);
        }
    }

    public void setIps(List<String> ips) {
        this.ips = new ArrayList<>(ips);
        // Set the first IP for backward compatibility
        if (!ips.isEmpty()) {
            this.ip = ips.get(0);
        }
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
