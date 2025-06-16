/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.constant;

/**
 * Enumeration of possible scan job execution statuses in the TLS-Crawler distributed system.
 *
 * <p>The JobStatus enum categorizes the final outcome of scan job processing, providing detailed
 * status information for monitoring, debugging, and result analysis. Each status indicates both the
 * execution outcome and whether it represents an error condition.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li><strong>Status Classification</strong> - Distinguishes between successful and error states
 *   <li><strong>Error Categorization</strong> - Provides specific error types for troubleshooting
 *   <li><strong>Database Integration</strong> - Status determines what data is written to storage
 *   <li><strong>Progress Monitoring</strong> - Enables accurate completion and error rate tracking
 * </ul>
 *
 * <p><strong>Status Categories:</strong>
 *
 * <ul>
 *   <li><strong>Success States</strong> - TO_BE_EXECUTED, SUCCESS, EMPTY
 *   <li><strong>Infrastructure Errors</strong> - UNRESOLVABLE, RESOLUTION_ERROR, DENYLISTED
 *   <li><strong>Execution Errors</strong> - ERROR, SERIALIZATION_ERROR, CANCELLED
 *   <li><strong>System Errors</strong> - INTERNAL_ERROR, CRAWLER_ERROR
 * </ul>
 *
 * <p><strong>Database Behavior:</strong>
 *
 * <ul>
 *   <li><strong>Full Results</strong> - SUCCESS writes complete scan data
 *   <li><strong>Empty Results</strong> - UNRESOLVABLE, DENYLISTED, EMPTY write minimal data
 *   <li><strong>Error Results</strong> - All error states write error information and stack traces
 *   <li><strong>No Results</strong> - INTERNAL_ERROR prevents database writes
 * </ul>
 *
 * <p><strong>Usage in Monitoring:</strong>
 *
 * <pre>{@code
 * // Error rate calculation
 * long errorCount = results.stream()
 *     .map(ScanResult::getJobStatus)
 *     .filter(JobStatus::isError)
 *     .count();
 *
 * // Status-specific handling
 * switch (jobStatus) {
 *     case SUCCESS -> processResult(result);
 *     case UNRESOLVABLE -> logDNSIssue(target);
 *     case ERROR -> reportError(error);
 * }
 * }</pre>
 *
 * Used by ScanJobDescription.getStatus() and ScanResult.getJobStatus() methods. Set during
 * processing by Worker.handleScanJob(ScanJobDescription) method.
 */
public enum JobStatus {
    /** Job is waiting to be executed. */
    TO_BE_EXECUTED(false),
    /** The domain was not resolvable. An empty result was written to DB. */
    UNRESOLVABLE(true),
    /** An uncaught exception occurred while resolving the host. */
    RESOLUTION_ERROR(true),
    /** The domain was denylisted. An empty result was written to DB. */
    DENYLISTED(true),
    /** Job was successfully executed. Result was written to db. */
    SUCCESS(false),
    /** Job was successfully executed. No result was returned. An empty result was written to DB. */
    EMPTY(false),
    /** Job encountered an exception. Stacktrace was written to DB. */
    ERROR(true),
    /** Job encountered an exception during serialization. Stacktrace was written to DB. */
    SERIALIZATION_ERROR(true),
    /** Job was cancelled (due to timeout). A partial result was written to DB. */
    CANCELLED(true),
    /** An internal error occurred. Nothing was written to DB */
    INTERNAL_ERROR(true),
    /**
     * An internal error in the crawler occurred. This most likely indicates a bug in the crawler.
     * This was written to DB
     */
    CRAWLER_ERROR(true),
    ;

    private final boolean isError;

    JobStatus(boolean isError) {
        this.isError = isError;
    }

    /**
     * Determines whether this status represents an error condition.
     *
     * <p>This method categorizes job statuses into successful and error states for monitoring and
     * reporting purposes. Error states indicate problems that prevented normal scan completion,
     * while non-error states represent successful processing (even if no data was obtained).
     *
     * <p><strong>Error Status Classification:</strong>
     *
     * <ul>
     *   <li><strong>Non-Error</strong> - TO_BE_EXECUTED, SUCCESS, EMPTY
     *   <li><strong>Error</strong> - All other statuses indicate problems or failures
     * </ul>
     *
     * @return true if this status indicates an error condition, false for successful processing
     */
    public boolean isError() {
        return isError;
    }
}
