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
 * File-based denylist provider supporting hostnames, IP addresses, and CIDR subnet filtering.
 *
 * <p>The DenylistFileProvider implements IDenylistProvider by reading filtering rules from a local
 * text file. It supports multiple entry types to provide comprehensive target filtering
 * capabilities for compliance, security, and resource management requirements.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Multiple Formats</strong> - Hostnames, individual IPs, and CIDR subnet blocks
 *   <li><strong>Automatic Classification</strong> - Validates and categorizes entries by type
 *   <li><strong>Performance Optimized</strong> - Uses appropriate data structures for fast lookups
 *   <li><strong>Thread-Safe</strong> - Synchronized access for concurrent worker operations
 * </ul>
 *
 * <p><strong>Supported Entry Types:</strong>
 *
 * <ul>
 *   <li><strong>Domain Names</strong> - Exact hostname matching (e.g., "example.com")
 *   <li><strong>IP Addresses</strong> - Individual IPv4/IPv6 addresses (e.g., "192.168.1.1")
 *   <li><strong>CIDR Blocks</strong> - Subnet ranges (e.g., "192.168.0.0/16", "10.0.0.0/8")
 * </ul>
 *
 * <p><strong>File Format:</strong> Plain text file with one entry per line. Invalid entries are
 * silently ignored. Comments and empty lines are processed as invalid entries.
 *
 * <p><strong>Example Denylist File:</strong>
 *
 * <pre>
 * # Private networks
 * 192.168.0.0/16
 * 10.0.0.0/8
 * 172.16.0.0/12
 *
 * # Specific domains
 * government.gov
 * sensitive.internal
 *
 * # Individual IPs
 * 203.0.113.1
 * 2001:db8::1
 * </pre>
 *
 * <p><strong>Validation and Processing:</strong>
 *
 * <ul>
 *   <li><strong>Domain Validation</strong> - Uses Apache Commons validator for RFC compliance
 *   <li><strong>IP Validation</strong> - Supports both IPv4 and IPv6 address formats
 *   <li><strong>CIDR Validation</strong> - Validates subnet notation and creates SubnetUtils
 *       objects
 *   <li><strong>Error Handling</strong> - Invalid entries are logged and ignored
 * </ul>
 *
 * <p><strong>Performance Characteristics:</strong>
 *
 * <ul>
 *   <li><strong>Domain Lookup</strong> - O(1) HashSet lookup for exact hostname matches
 *   <li><strong>IP Lookup</strong> - O(1) HashSet lookup for individual IP addresses
 *   <li><strong>Subnet Lookup</strong> - O(n) linear search through CIDR blocks
 *   <li><strong>Memory Usage</strong> - Efficient storage with type-specific collections
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> The isDenylisted method is synchronized to ensure thread-safe
 * access during concurrent scanning operations.
 *
 * @see IDenylistProvider
 * @see ScanTarget
 * @see SubnetUtils
 */
public class DenylistFileProvider implements IDenylistProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Set<String> ipDenylistSet = new HashSet<>();
    private final List<SubnetUtils.SubnetInfo> cidrDenylist = new ArrayList<>();
    private final Set<String> domainDenylistSet = new HashSet<>();

    /**
     * Creates a new file-based denylist provider from the specified file.
     *
     * <p>The constructor reads and parses the denylist file, categorizing entries by type (domain,
     * IP, CIDR) and storing them in optimized data structures for fast lookup. File access errors
     * are logged but don't prevent provider creation.
     *
     * @param denylistFilename the path to the denylist file to read
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

    @Override
    public synchronized boolean isDenylisted(ScanTarget target) {
        return domainDenylistSet.contains(target.getHostname())
                || ipDenylistSet.contains(target.getIp())
                || cidrDenylist.stream()
                        .anyMatch(subnetInfo -> isInSubnet(target.getIp(), subnetInfo));
    }
}
