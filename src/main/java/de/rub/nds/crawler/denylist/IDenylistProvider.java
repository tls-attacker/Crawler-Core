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

/**
 * Denylist provider interface for filtering prohibited scan targets in TLS-Crawler operations.
 *
 * <p>The IDenylistProvider defines the contract for target filtering and access control in the
 * TLS-Crawler system. It enables implementations to block specific hosts, IP ranges, or domains
 * from being scanned, supporting compliance requirements, ethical scanning practices, and resource
 * management policies.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Target Filtering</strong> - Determines if scan targets should be excluded
 *   <li><strong>Policy Enforcement</strong> - Implements organizational scanning policies
 *   <li><strong>Compliance Support</strong> - Ensures adherence to legal and ethical guidelines
 *   <li><strong>Resource Protection</strong> - Prevents scanning of sensitive or protected systems
 * </ul>
 *
 * <p><strong>Filtering Criteria:</strong>
 *
 * <ul>
 *   <li><strong>Hostname Patterns</strong> - Exact matches, wildcards, or domain suffixes
 *   <li><strong>IP Address Ranges</strong> - CIDR blocks, subnet ranges, or individual IPs
 *   <li><strong>Port Restrictions</strong> - Specific ports or port ranges to avoid
 *   <li><strong>Protocol Considerations</strong> - Protocol-specific filtering rules
 * </ul>
 *
 * <p><strong>Common Use Cases:</strong>
 *
 * <ul>
 *   <li><strong>Internal Networks</strong> - Block private IP ranges (RFC 1918)
 *   <li><strong>Government Domains</strong> - Exclude .gov, .mil, or country-specific domains
 *   <li><strong>Critical Infrastructure</strong> - Protect essential services and utilities
 *   <li><strong>Legal Compliance</strong> - Honor legal restrictions and opt-out requests
 * </ul>
 *
 * <p><strong>Implementation Guidelines:</strong>
 *
 * <ul>
 *   <li><strong>Performance</strong> - Optimize for fast lookups with large denylists
 *   <li><strong>Memory Efficiency</strong> - Use appropriate data structures for scale
 *   <li><strong>Thread Safety</strong> - Support concurrent access from multiple workers
 *   <li><strong>Dynamic Updates</strong> - Consider support for runtime denylist updates
 * </ul>
 *
 * <p><strong>Common Implementations:</strong>
 *
 * <ul>
 *   <li><strong>DenylistFileProvider</strong> - File-based denylist with various formats
 *   <li><strong>CIDR Block Providers</strong> - IP range filtering with subnet support
 *   <li><strong>Domain Pattern Providers</strong> - Regex or wildcard domain matching
 *   <li><strong>Composite Providers</strong> - Multiple filtering criteria combined
 * </ul>
 *
 * <p><strong>Integration Points:</strong> Denylist providers are typically used during target
 * processing in PublishBulkScanJob and ScanTarget.fromTargetString() to filter targets before scan
 * job creation.
 *
 * @see ScanTarget
 * @see ScanTarget#fromTargetString(String, int, IDenylistProvider)
 * @see DenylistFileProvider
 */
public interface IDenylistProvider {

    /**
     * Determines if a scan target should be excluded from scanning based on denylist rules.
     *
     * <p>This method evaluates the provided scan target against the configured denylist criteria
     * and returns true if the target should be blocked from scanning. The implementation should
     * consider all relevant target attributes including hostname, IP address, and port when making
     * the determination.
     *
     * <p><strong>Evaluation Criteria:</strong>
     *
     * <ul>
     *   <li><strong>Hostname Matching</strong> - Check hostname against domain patterns
     *   <li><strong>IP Address Filtering</strong> - Evaluate IP against CIDR blocks or ranges
     *   <li><strong>Port Restrictions</strong> - Consider port-specific filtering rules
     *   <li><strong>Combined Rules</strong> - Apply multiple criteria as configured
     * </ul>
     *
     * <p><strong>Performance Considerations:</strong> This method may be called frequently during
     * target processing, so implementations should optimize for fast evaluation, especially with
     * large denylists.
     *
     * <p><strong>Thread Safety:</strong> This method must be thread-safe as it will be called
     * concurrently during parallel target processing.
     *
     * @param target the scan target to evaluate against denylist rules
     * @return true if the target is denylisted and should not be scanned, false otherwise
     */
    boolean isDenylisted(ScanTarget target);
}
