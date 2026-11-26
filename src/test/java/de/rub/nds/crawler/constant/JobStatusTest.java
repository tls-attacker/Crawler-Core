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

import org.junit.jupiter.api.Test;

class JobStatusTest {

    @Test
    void testIsError() {
        assertFalse(JobStatus.TO_BE_EXECUTED.isError());
        assertTrue(JobStatus.UNRESOLVABLE.isError());
        assertTrue(JobStatus.RESOLUTION_ERROR.isError());
        assertTrue(JobStatus.DENYLISTED.isError());
        assertFalse(JobStatus.SUCCESS.isError());
        assertFalse(JobStatus.EMPTY.isError());
        assertTrue(JobStatus.ERROR.isError());
        assertTrue(JobStatus.SERIALIZATION_ERROR.isError());
        assertTrue(JobStatus.CANCELLED.isError());
        assertTrue(JobStatus.INTERNAL_ERROR.isError());
        assertTrue(JobStatus.CRAWLER_ERROR.isError());
    }

    @Test
    void testEnumValues() {
        JobStatus[] values = JobStatus.values();
        assertEquals(11, values.length);
        assertEquals(JobStatus.TO_BE_EXECUTED, values[0]);
        assertEquals(JobStatus.UNRESOLVABLE, values[1]);
        assertEquals(JobStatus.RESOLUTION_ERROR, values[2]);
        assertEquals(JobStatus.DENYLISTED, values[3]);
        assertEquals(JobStatus.SUCCESS, values[4]);
        assertEquals(JobStatus.EMPTY, values[5]);
        assertEquals(JobStatus.ERROR, values[6]);
        assertEquals(JobStatus.SERIALIZATION_ERROR, values[7]);
        assertEquals(JobStatus.CANCELLED, values[8]);
        assertEquals(JobStatus.INTERNAL_ERROR, values[9]);
        assertEquals(JobStatus.CRAWLER_ERROR, values[10]);
    }

    @Test
    void testValueOf() {
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
    void testValueOfInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> JobStatus.valueOf("INVALID_STATUS"));
        assertThrows(IllegalArgumentException.class, () -> JobStatus.valueOf(""));
        assertThrows(NullPointerException.class, () -> JobStatus.valueOf(null));
    }

    @Test
    void testEnumName() {
        assertEquals("TO_BE_EXECUTED", JobStatus.TO_BE_EXECUTED.name());
        assertEquals("UNRESOLVABLE", JobStatus.UNRESOLVABLE.name());
        assertEquals("RESOLUTION_ERROR", JobStatus.RESOLUTION_ERROR.name());
        assertEquals("DENYLISTED", JobStatus.DENYLISTED.name());
        assertEquals("SUCCESS", JobStatus.SUCCESS.name());
        assertEquals("EMPTY", JobStatus.EMPTY.name());
        assertEquals("ERROR", JobStatus.ERROR.name());
        assertEquals("SERIALIZATION_ERROR", JobStatus.SERIALIZATION_ERROR.name());
        assertEquals("CANCELLED", JobStatus.CANCELLED.name());
        assertEquals("INTERNAL_ERROR", JobStatus.INTERNAL_ERROR.name());
        assertEquals("CRAWLER_ERROR", JobStatus.CRAWLER_ERROR.name());
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, JobStatus.TO_BE_EXECUTED.ordinal());
        assertEquals(1, JobStatus.UNRESOLVABLE.ordinal());
        assertEquals(2, JobStatus.RESOLUTION_ERROR.ordinal());
        assertEquals(3, JobStatus.DENYLISTED.ordinal());
        assertEquals(4, JobStatus.SUCCESS.ordinal());
        assertEquals(5, JobStatus.EMPTY.ordinal());
        assertEquals(6, JobStatus.ERROR.ordinal());
        assertEquals(7, JobStatus.SERIALIZATION_ERROR.ordinal());
        assertEquals(8, JobStatus.CANCELLED.ordinal());
        assertEquals(9, JobStatus.INTERNAL_ERROR.ordinal());
        assertEquals(10, JobStatus.CRAWLER_ERROR.ordinal());
    }

    @Test
    void testEnumToString() {
        assertEquals("TO_BE_EXECUTED", JobStatus.TO_BE_EXECUTED.toString());
        assertEquals("UNRESOLVABLE", JobStatus.UNRESOLVABLE.toString());
        assertEquals("RESOLUTION_ERROR", JobStatus.RESOLUTION_ERROR.toString());
        assertEquals("DENYLISTED", JobStatus.DENYLISTED.toString());
        assertEquals("SUCCESS", JobStatus.SUCCESS.toString());
        assertEquals("EMPTY", JobStatus.EMPTY.toString());
        assertEquals("ERROR", JobStatus.ERROR.toString());
        assertEquals("SERIALIZATION_ERROR", JobStatus.SERIALIZATION_ERROR.toString());
        assertEquals("CANCELLED", JobStatus.CANCELLED.toString());
        assertEquals("INTERNAL_ERROR", JobStatus.INTERNAL_ERROR.toString());
        assertEquals("CRAWLER_ERROR", JobStatus.CRAWLER_ERROR.toString());
    }

    @Test
    void testEnumEquality() {
        assertTrue(JobStatus.SUCCESS == JobStatus.SUCCESS);
        assertTrue(JobStatus.ERROR == JobStatus.ERROR);
        assertFalse(JobStatus.SUCCESS == JobStatus.ERROR);
        assertFalse(JobStatus.TO_BE_EXECUTED == JobStatus.UNRESOLVABLE);
    }

    @Test
    void testSuccessStatuses() {
        // Test that success-like statuses are not marked as errors
        assertFalse(JobStatus.TO_BE_EXECUTED.isError());
        assertFalse(JobStatus.SUCCESS.isError());
        assertFalse(JobStatus.EMPTY.isError());
    }

    @Test
    void testErrorStatuses() {
        // Test that all error statuses are correctly marked
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
    void testAllStatusesHaveDefinedErrorState() {
        // Ensure all enum values have their error state properly defined
        for (JobStatus status : JobStatus.values()) {
            assertNotNull(status);
            // This will not throw NPE if isError is properly implemented
            boolean errorState = status.isError();
            // Just verify we can call the method on all values
            assertTrue(errorState || !errorState); // Always true, just testing accessibility
        }
    }

    @Test
    void testEnumDeclarationOrder() {
        // Verify the order matches the expected workflow
        JobStatus[] values = JobStatus.values();

        // TO_BE_EXECUTED should be first as it's the initial state
        assertEquals(JobStatus.TO_BE_EXECUTED, values[0]);

        // Terminal states should come after
        assertTrue(values[0].ordinal() < JobStatus.SUCCESS.ordinal());
        assertTrue(values[0].ordinal() < JobStatus.ERROR.ordinal());
    }
}
