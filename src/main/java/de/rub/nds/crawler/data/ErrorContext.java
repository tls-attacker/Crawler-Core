/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

/**
 * Utility class for creating structured error context information in scan results.
 *
 * <p>This class provides static methods to generate standardized error context strings that can be
 * used with {@link ScanResult#fromException(ScanJobDescription, Exception, String)} to provide
 * detailed debugging information for scan failures.
 *
 * <p>The error context strings follow a consistent format to facilitate parsing and analysis of
 * error patterns across large-scale scan operations. Each context type includes relevant
 * operational details and failure specifics.
 *
 * <p><strong>Context Categories:</strong>
 *
 * <ul>
 *   <li><strong>DNS Resolution Failures</strong> - Hostname resolution errors with target details
 *   <li><strong>Denylist Rejections</strong> - Blocking reasons with target and rule information
 *   <li><strong>Target Parsing Failures</strong> - Input format issues with problematic strings
 *   <li><strong>Network Connectivity</strong> - Connection and timeout failures
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * try {
 *     // Perform hostname resolution
 *     InetAddress.getAllByName(hostname);
 * } catch (UnknownHostException e) {
 *     String context = ErrorContext.dnsResolutionFailure(hostname, "A record lookup failed");
 *     ScanResult errorResult = ScanResult.fromException(jobDescription, e, context);
 * }
 * }</pre>
 *
 * @see ScanResult#fromException(ScanJobDescription, Exception, String)
 * @see de.rub.nds.crawler.constant.JobStatus
 */
public final class ErrorContext {

    private ErrorContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates error context for DNS resolution failures.
     *
     * @param hostname the hostname that failed to resolve
     * @param reason the specific DNS failure reason
     * @return formatted error context string
     */
    public static String dnsResolutionFailure(String hostname, String reason) {
        return String.format("DNS resolution failed for hostname '%s': %s", hostname, reason);
    }

    /**
     * Creates error context for denylist rejections.
     *
     * @param target the target that was rejected
     * @param ruleType the type of denylist rule that triggered (IP, domain, etc.)
     * @return formatted error context string
     */
    public static String denylistRejection(String target, String ruleType) {
        return String.format("Target '%s' rejected by %s denylist rule", target, ruleType);
    }

    /**
     * Creates error context for target string parsing failures.
     *
     * @param targetString the unparseable target string
     * @param parseStage the parsing stage where failure occurred
     * @return formatted error context string
     */
    public static String targetParsingFailure(String targetString, String parseStage) {
        return String.format(
                "Failed to parse target string '%s' during %s", targetString, parseStage);
    }

    /**
     * Creates error context for port parsing failures.
     *
     * @param portString the invalid port string
     * @param targetString the full target string for context
     * @return formatted error context string
     */
    public static String portParsingFailure(String portString, String targetString) {
        return String.format("Invalid port '%s' in target string '%s'", portString, targetString);
    }

    /**
     * Creates error context for general target processing failures.
     *
     * @param targetString the target string being processed
     * @param operation the operation that failed
     * @return formatted error context string
     */
    public static String targetProcessingFailure(String targetString, String operation) {
        return String.format(
                "Target processing failed for '%s' during %s", targetString, operation);
    }
}
