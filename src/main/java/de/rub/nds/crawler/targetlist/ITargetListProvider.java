/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import java.util.List;

/**
 * Target list provider interface for supplying scan targets to TLS-Crawler operations.
 *
 * <p>The ITargetListProvider defines the contract for obtaining lists of scan targets from various
 * sources including files, web services, databases, and curated lists. It abstracts the target
 * acquisition mechanism and provides a consistent interface for controllers to obtain targets for
 * bulk scanning operations.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li><strong>Target Acquisition</strong> - Retrieves targets from the configured source
 *   <li><strong>Format Standardization</strong> - Provides targets in consistent string format
 *   <li><strong>Source Abstraction</strong> - Hides implementation details of target sources
 *   <li><strong>Error Handling</strong> - Manages source-specific failures gracefully
 * </ul>
 *
 * <p><strong>Target Format:</strong>
 *
 * <ul>
 *   <li><strong>Hostname Only</strong> - "example.com" (uses default port)
 *   <li><strong>Hostname with Port</strong> - "example.com:443" (explicit port)
 *   <li><strong>IP Address</strong> - "192.168.1.1" or "192.168.1.1:8443"
 *   <li><strong>IPv6 Address</strong> - "[::1]" or "[::1]:443"
 * </ul>
 *
 * <p><strong>Common Implementations:</strong>
 *
 * <ul>
 *   <li><strong>TargetFileProvider</strong> - Reads targets from local files
 *   <li><strong>TrancoListProvider</strong> - Fetches targets from Tranco web ranking
 *   <li><strong>CruxListProvider</strong> - Uses Google Chrome UX Report data
 *   <li><strong>TrancoEmailListProvider</strong> - Extracts MX records from Tranco data
 *   <li><strong>ZipFileProvider</strong> - Reads from compressed archive files
 * </ul>
 *
 * <p><strong>Implementation Guidelines:</strong>
 *
 * <ul>
 *   <li><strong>Error Resilience</strong> - Should handle network failures and missing sources
 *   <li><strong>Performance</strong> - Consider caching for expensive operations
 *   <li><strong>Memory Efficiency</strong> - Stream large lists when possible
 *   <li><strong>Format Validation</strong> - Ensure returned targets are well-formed
 * </ul>
 *
 * <p><strong>Usage Pattern:</strong> Target list providers are typically configured based on
 * command-line arguments and used by controllers during bulk scan initialization to obtain the
 * complete list of targets for processing.
 *
 * @see TargetFileProvider
 * @see TrancoListProvider
 * @see CruxListProvider Configured via ControllerCommandConfig.getTargetListProvider() method.
 */
public interface ITargetListProvider {

    /**
     * Retrieves the complete list of scan targets from the configured source.
     *
     * <p>This method fetches all available targets from the provider's source and returns them as a
     * list of string representations. The implementation should handle any necessary data
     * retrieval, parsing, and formatting to produce valid target strings.
     *
     * <p><strong>Target Format:</strong> Each string should represent a valid scan target in
     * hostname[:port] format, suitable for parsing by ScanTarget.fromTargetString().
     *
     * <p><strong>Error Handling:</strong> Implementations should handle source-specific errors
     * (network failures, file not found, etc.) and either throw appropriate exceptions or return
     * empty lists based on the error recovery strategy.
     *
     * <p><strong>Performance Considerations:</strong> This method may perform expensive operations
     * like network requests or large file parsing. Consider implementing caching or streaming
     * strategies for large target lists.
     *
     * @return a list of target strings in hostname[:port] format
     * @throws RuntimeException if targets cannot be retrieved (implementation-specific)
     */
    List<String> getTargetList();
}
