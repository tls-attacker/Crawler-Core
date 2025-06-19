/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.constant.CruxListNumber;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CruxListProviderTest {

    private CruxListProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CruxListProvider(CruxListNumber.TOP_1k);
    }

    @Test
    void testGetTargetListFromLines() {
        // Test data with various formats
        List<String> lines =
                List.of(
                        "https://google.com,1",
                        "https://facebook.com,2",
                        "http://example.com,3", // Should be filtered out (not https)
                        "https://amazon.com,999",
                        "https://twitter.com,1000",
                        "https://youtube.com,1001", // Should be filtered out (rank > 1000)
                        "https://netflix.com,500");

        List<String> result = provider.getTargetListFromLines(lines.stream());

        assertEquals(5, result.size());
        assertTrue(result.contains("google.com"));
        assertTrue(result.contains("facebook.com"));
        assertTrue(result.contains("amazon.com"));
        assertTrue(result.contains("twitter.com"));
        assertTrue(result.contains("netflix.com"));
        assertFalse(result.contains("example.com")); // http filtered out
        assertFalse(result.contains("youtube.com")); // rank too high
    }

    @Test
    void testGetTargetListFromLinesTop5K() {
        CruxListProvider provider5k = new CruxListProvider(CruxListNumber.TOP_5K);

        List<String> lines =
                List.of(
                        "https://site1.com,1",
                        "https://site2.com,4999",
                        "https://site3.com,5000",
                        "https://site4.com,5001"); // Should be filtered out

        List<String> result = provider5k.getTargetListFromLines(lines.stream());

        assertEquals(3, result.size());
        assertTrue(result.contains("site1.com"));
        assertTrue(result.contains("site2.com"));
        assertTrue(result.contains("site3.com"));
        assertFalse(result.contains("site4.com"));
    }

    @Test
    void testGetTargetListFromLinesWithSubdomains() {
        List<String> lines =
                List.of(
                        "https://www.google.com,1",
                        "https://mail.google.com,2",
                        "https://subdomain.example.com,100");

        List<String> result = provider.getTargetListFromLines(lines.stream());

        assertEquals(3, result.size());
        assertEquals("www.google.com", result.get(0));
        assertEquals("mail.google.com", result.get(1));
        assertEquals("subdomain.example.com", result.get(2));
    }

    @Test
    void testGetTargetListFromLinesWithPorts() {
        List<String> lines = List.of("https://example.com:8443,1", "https://test.com:443,2");

        List<String> result = provider.getTargetListFromLines(lines.stream());

        assertEquals(2, result.size());
        assertEquals("example.com:8443", result.get(0));
        assertEquals("test.com:443", result.get(1));
    }

    @Test
    void testGetTargetListFromLinesWithPaths() {
        List<String> lines =
                List.of("https://example.com/path,1", "https://test.com/path/to/resource,2");

        List<String> result = provider.getTargetListFromLines(lines.stream());

        assertEquals(2, result.size());
        assertEquals("example.com/path", result.get(0));
        assertEquals("test.com/path/to/resource", result.get(1));
    }

    @Test
    void testGetTargetListFromLinesEmptyInput() {
        List<String> result = provider.getTargetListFromLines(Stream.empty());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetTargetListFromLinesInvalidFormat() {
        List<String> lines =
                List.of(
                        "invalid-line-without-comma",
                        "https://valid.com,1",
                        "another-invalid-line");

        // Should handle gracefully or throw exception
        assertThrows(Exception.class, () -> provider.getTargetListFromLines(lines.stream()));
    }

    @Test
    void testGetTargetListFromLinesInvalidRankFormat() {
        List<String> lines = List.of("https://example.com,not-a-number", "https://valid.com,1");

        // Should throw NumberFormatException
        assertThrows(
                NumberFormatException.class, () -> provider.getTargetListFromLines(lines.stream()));
    }

    @Test
    void testAllCruxListNumbers() {
        // Test each enum value
        for (CruxListNumber cruxNumber : CruxListNumber.values()) {
            CruxListProvider testProvider = new CruxListProvider(cruxNumber);

            List<String> lines =
                    List.of(
                            "https://site1.com,1",
                            String.format("https://site2.com,%d", cruxNumber.getNumber()),
                            String.format("https://site3.com,%d", cruxNumber.getNumber() + 1));

            List<String> result = testProvider.getTargetListFromLines(lines.stream());

            assertEquals(2, result.size());
            assertTrue(result.contains("site1.com"));
            assertTrue(result.contains("site2.com"));
            assertFalse(result.contains("site3.com"));
        }
    }
}
