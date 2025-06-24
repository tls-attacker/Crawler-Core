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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads the specified denylist file. Supports hostnames, ips and complete subnets as denylist
 * entries.
 */
public class DenylistFileProvider implements IDenylistProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Set<String> ipDenylistSet = new HashSet<>();
    private final List<SubnetUtils.SubnetInfo> cidrDenylist = new ArrayList<>();
    private final Set<String> domainDenylistSet = new HashSet<>();

    /**
     * Constructs a new DenylistFileProvider by reading and parsing a denylist file. The file should
     * contain one entry per line, supporting: - Domain names (e.g., example.com) - IP addresses
     * (e.g., 192.168.1.1) - CIDR subnet notations (e.g., 192.168.0.0/24)
     *
     * @param denylistFilename the path to the denylist file
     */
    public DenylistFileProvider(String denylistFilename) {
        List<String> denylist = List.of();
        try (Stream<String> lines = Files.lines(Paths.get(denylistFilename))) {
            denylist = lines.collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Could not read denylist {}", denylistFilename);
        }
        for (String denylistEntry : denylist) {
            if (DomainValidator.getInstance().isValid(denylistEntry)) {
                domainDenylistSet.add(denylistEntry);
            } else if (InetAddressValidator.getInstance().isValid(denylistEntry)) {
                ipDenylistSet.add(denylistEntry);
            } else if (denylistEntry.contains("/")
                    && InetAddressValidator.getInstance().isValid(denylistEntry.split("/")[0])
                    && IntegerValidator.getInstance().isValid(denylistEntry.split("/")[1])) {
                SubnetUtils utils = new SubnetUtils(denylistEntry);
                cidrDenylist.add(utils.getInfo());
            }
        }
    }

    private boolean isInSubnet(String ip, SubnetUtils.SubnetInfo subnetInfo) {
        try {
            return subnetInfo.isInRange(ip);
        } catch (IllegalArgumentException e) {
            // most likely we tried to check an IPv6 address against an IPv4 subnet
            return false;
        }
    }

    /**
     * Checks whether a scan target is denylisted based on its hostname, IP address, or if it falls
     * within a denylisted subnet.
     *
     * @param target the scan target to check
     * @return true if the target is denylisted, false otherwise
     */
    @Override
    public synchronized boolean isDenylisted(ScanTarget target) {
        return domainDenylistSet.contains(target.getHostname())
                || ipDenylistSet.contains(target.getIp())
                || cidrDenylist.stream()
                        .anyMatch(subnetInfo -> isInSubnet(target.getIp(), subnetInfo));
    }
}
