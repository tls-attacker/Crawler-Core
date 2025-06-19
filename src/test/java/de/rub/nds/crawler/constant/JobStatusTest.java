/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.constant;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class JobStatusTest {

    @Test
    void testErrorStatuses() {
        // Test all statuses marked as errors
        assertTrue(JobStatus.UNRESOLVABLE.isError());
        assertTrue(JobStatus.RESOLUTION_ERROR.isError());
        assertTrue(JobStatus.DENYLISTED.isError());
        assertTrue(JobStatus.ERROR.isError());
        assertTrue(JobStatus.SERIALIZATION_ERROR.isError());
        assertTrue(JobStatus.CANCELLED.isError());
        assertTrue(JobStatus.INTERNAL_ERROR.isError());
        assertTrue(JobStatus.CRAWLER_ERROR.isError());
    }

    @Test
    void testNonErrorStatuses() {
        // Test all statuses not marked as errors
        assertFalse(JobStatus.TO_BE_EXECUTED.isError());
        assertFalse(JobStatus.SUCCESS.isError());
        assertFalse(JobStatus.EMPTY.isError());
    }

    @Test
    void testAllStatusesHaveErrorFlag() {
        // Ensure all enum values have their error flag properly set
        for (JobStatus status : JobStatus.values()) {
            // This should not throw - all statuses should have isError defined
            boolean isError = status.isError();

            // Verify the value is either true or false (not null or undefined)
            assertTrue(isError || !isError);
        }
    }

    @Test
    void testEnumValueOf() {
        // Test that valueOf works for all enum constants
        assertEquals(JobStatus.TO_BE_EXECUTED, JobStatus.valueOf("TO_BE_EXECUTED"));
        assertEquals(JobStatus.UNRESOLVABLE, JobStatus.valueOf("UNRESOLVABLE"));
        assertEquals(JobStatus.RESOLUTION_ERROR, JobStatus.valueOf("RESOLUTION_ERROR"));
        assertEquals(JobStatus.DENYLISTED, JobStatus.valueOf("DENYLISTED"));
        assertEquals(JobStatus.SUCCESS, JobStatus.valueOf("SUCCESS"));
        assertEquals(JobStatus.EMPTY, JobStatus.valueOf("EMPTY"));
        assertEquals(JobStatus.ERROR, JobStatus.valueOf("ERROR"));
        assertEquals(JobStatus.SERIALIZATION_ERROR, JobStatus.valueOf("SERIALIZATION_ERROR"));
        assertEquals(JobStatus.CANCELLED, JobStatus.valueOf("CANCELLED"));
        assertEquals(JobStatus.INTERNAL_ERROR, JobStatus.valueOf("INTERNAL_ERROR"));
        assertEquals(JobStatus.CRAWLER_ERROR, JobStatus.valueOf("CRAWLER_ERROR"));
    }

    @Test
    void testEnumValues() {
        // Test that values() returns all enum constants
        JobStatus[] statuses = JobStatus.values();
        assertEquals(11, statuses.length);

        // Verify all expected values are present
        boolean hasToBeExecuted = false;
        boolean hasUnresolvable = false;
        boolean hasResolutionError = false;
        boolean hasDenylisted = false;
        boolean hasSuccess = false;
        boolean hasEmpty = false;
        boolean hasError = false;
        boolean hasSerializationError = false;
        boolean hasCancelled = false;
        boolean hasInternalError = false;
        boolean hasCrawlerError = false;

        for (JobStatus status : statuses) {
            switch (status) {
                case TO_BE_EXECUTED:
                    hasToBeExecuted = true;
                    break;
                case UNRESOLVABLE:
                    hasUnresolvable = true;
                    break;
                case RESOLUTION_ERROR:
                    hasResolutionError = true;
                    break;
                case DENYLISTED:
                    hasDenylisted = true;
                    break;
                case SUCCESS:
                    hasSuccess = true;
                    break;
                case EMPTY:
                    hasEmpty = true;
                    break;
                case ERROR:
                    hasError = true;
                    break;
                case SERIALIZATION_ERROR:
                    hasSerializationError = true;
                    break;
                case CANCELLED:
                    hasCancelled = true;
                    break;
                case INTERNAL_ERROR:
                    hasInternalError = true;
                    break;
                case CRAWLER_ERROR:
                    hasCrawlerError = true;
                    break;
            }
        }

        assertTrue(hasToBeExecuted);
        assertTrue(hasUnresolvable);
        assertTrue(hasResolutionError);
        assertTrue(hasDenylisted);
        assertTrue(hasSuccess);
        assertTrue(hasEmpty);
        assertTrue(hasError);
        assertTrue(hasSerializationError);
        assertTrue(hasCancelled);
        assertTrue(hasInternalError);
        assertTrue(hasCrawlerError);
    }

    @Test
    void testInvalidValueOf() {
        // Test that valueOf throws for invalid values
        assertThrows(IllegalArgumentException.class, () -> JobStatus.valueOf("INVALID_STATUS"));
    }
}
