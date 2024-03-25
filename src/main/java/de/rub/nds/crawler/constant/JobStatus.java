/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.constant;

public enum JobStatus {
    /** Job is waiting to be executed. */
    TO_BE_EXECUTED(false),
    /** The domain was not resolvable. An empty result was written to DB. */
    UNRESOLVABLE(true),
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

    public boolean isError() {
        return isError;
    }
}
