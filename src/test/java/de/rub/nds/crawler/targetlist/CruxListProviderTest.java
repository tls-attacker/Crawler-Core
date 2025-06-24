/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.constant.CruxListNumber;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class CruxListProviderTest {

    @Test
    public void testConstructor() {
        CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_1k);
        assertNotNull(provider);
    }

    @Test
    public void testGetTargetListFromLines() {
        CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_1k);

        // Test with valid HTTPS entries
        Stream<String> lines =
                Stream.of(
                        "https://example.com,1",
                        "https://test.com,2",
                        "http://insecure.com,3",
                        "https://third.com,3",
                        "https://outofrange.com,1001");

        List<String> result = provider.getTargetListFromLines(lines);

        assertEquals(3, result.size());
        assertTrue(result.contains("example.com"));
        assertTrue(result.contains("test.com"));
        assertTrue(result.contains("third.com"));
        assertFalse(result.contains("insecure.com")); // HTTP should be filtered out
        assertFalse(result.contains("outofrange.com")); // Above rank 1000
    }

    @Test
    public void testGetTargetListFromLinesWithTop5K() {
        CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_5K);

        Stream<String> lines =
                Stream.of(
                        "https://example.com,1",
                        "https://test.com,4999",
                        "https://borderline.com,5000",
                        "https://outofrange.com,5001");

        List<String> result = provider.getTargetListFromLines(lines);

        assertEquals(3, result.size());
        assertTrue(result.contains("example.com"));
        assertTrue(result.contains("test.com"));
        assertTrue(result.contains("borderline.com"));
        assertFalse(result.contains("outofrange.com"));
    }

    @Test
    public void testGetTargetListFromLinesEmptyStream() {
        CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_1k);
        Stream<String> lines = Stream.empty();

        List<String> result = provider.getTargetListFromLines(lines);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetTargetListFromLinesWithInvalidFormat() {
        CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_1k);

        Stream<String> lines =
                Stream.of(
                        "https://example.com,1",
                        "invalid-format",
                        "https://test.com,2",
                        "https://norank.com");

        // This should throw an exception due to invalid format
        assertThrows(
                Exception.class,
                () -> {
                    provider.getTargetListFromLines(lines);
                });
    }

    @Test
    public void testGetTargetListFromLinesWithComplexUrls() {
        CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_1k);

        Stream<String> lines =
                Stream.of(
                        "https://subdomain.example.com,1",
                        "https://example.com:8443,2",
                        "https://test.com/path,3");

        List<String> result = provider.getTargetListFromLines(lines);

        assertEquals(3, result.size());
        assertTrue(result.contains("subdomain.example.com"));
        assertTrue(result.contains("example.com:8443"));
        assertTrue(result.contains("test.com/path"));
    }

    @Test
    public void testAllCruxListNumbers() {
        // Test that provider can be created with all enum values
        for (CruxListNumber number : CruxListNumber.values()) {
            CruxListProvider provider = new CruxListProvider(number);
            assertNotNull(provider);
        }
    }
}
