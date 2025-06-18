/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.denylist;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.data.ScanTarget;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;

class DenylistFileProviderTest {

    private Path tempFile;
    private DenylistFileProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("denylist", ".txt");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    @Test
    void testEmptyDenylist() throws IOException {
        // Given - empty file
        provider = new DenylistFileProvider(tempFile.toString());

        // When
        ScanTarget target = createTarget("example.com", "192.0.2.1");

        // Then
        assertFalse(provider.isDenylisted(target));
    }

    @Test
    void testDenylistWithDomain() throws IOException {
        // Given
        writeDenylist("badsite.com", "evil.org");
        provider = new DenylistFileProvider(tempFile.toString());

        // Then - denylisted domains
        assertTrue(provider.isDenylisted(createTarget("badsite.com", "192.0.2.1")));
        assertTrue(provider.isDenylisted(createTarget("evil.org", "192.0.2.2")));

        // Not denylisted
        assertFalse(provider.isDenylisted(createTarget("goodsite.com", "192.0.2.3")));
    }

    @Test
    void testDenylistWithIP() throws IOException {
        // Given
        writeDenylist("192.0.2.1", "10.0.0.1");
        provider = new DenylistFileProvider(tempFile.toString());

        // Then - denylisted IPs
        assertTrue(provider.isDenylisted(createTarget("example.com", "192.0.2.1")));
        assertTrue(provider.isDenylisted(createTarget("test.com", "10.0.0.1")));

        // Not denylisted
        assertFalse(provider.isDenylisted(createTarget("example.com", "192.0.2.2")));
    }

    @Test
    void testDenylistWithCIDR() throws IOException {
        // Given
        writeDenylist("192.0.2.0/24", "10.0.0.0/16");
        provider = new DenylistFileProvider(tempFile.toString());

        // Then - IPs in subnet are denylisted
        assertTrue(provider.isDenylisted(createTarget("example.com", "192.0.2.1")));
        assertTrue(provider.isDenylisted(createTarget("example.com", "192.0.2.255")));
        assertTrue(provider.isDenylisted(createTarget("example.com", "10.0.1.1")));
        assertTrue(provider.isDenylisted(createTarget("example.com", "10.0.255.255")));

        // IPs outside subnet are not denylisted
        assertFalse(provider.isDenylisted(createTarget("example.com", "192.0.3.1")));
        assertFalse(provider.isDenylisted(createTarget("example.com", "10.1.0.1")));
    }

    @Test
    void testDenylistWithMixedEntries() throws IOException {
        // Given
        writeDenylist("badsite.com", "192.0.2.1", "10.0.0.0/24", "evil.org", "172.16.0.1");
        provider = new DenylistFileProvider(tempFile.toString());

        // Then - all types work
        assertTrue(provider.isDenylisted(createTarget("badsite.com", "1.1.1.1")));
        assertTrue(provider.isDenylisted(createTarget("example.com", "192.0.2.1")));
        assertTrue(provider.isDenylisted(createTarget("example.com", "10.0.0.100")));
        assertTrue(provider.isDenylisted(createTarget("evil.org", "2.2.2.2")));
        assertTrue(provider.isDenylisted(createTarget("example.com", "172.16.0.1")));
    }

    @Test
    void testInvalidDenylistEntries() throws IOException {
        // Given - invalid entries should be ignored
        writeDenylist(
                "not-a-valid-domain-or-ip",
                "999.999.999.999", // invalid IP
                "192.0.2.0/999", // invalid CIDR
                "example.com" // valid
                );
        provider = new DenylistFileProvider(tempFile.toString());

        // Then - only valid entry works
        assertTrue(provider.isDenylisted(createTarget("example.com", "1.1.1.1")));
        assertFalse(provider.isDenylisted(createTarget("not-a-valid-domain-or-ip", "1.1.1.1")));
    }

    @Test
    void testNonExistentFile() {
        // Given - file that doesn't exist
        provider = new DenylistFileProvider("/path/that/does/not/exist/denylist.txt");

        // Then - should not crash and nothing is denylisted
        assertFalse(provider.isDenylisted(createTarget("example.com", "192.0.2.1")));
    }

    @Test
    void testTargetWithNullHostname() throws IOException {
        // Given
        writeDenylist("192.0.2.1");
        provider = new DenylistFileProvider(tempFile.toString());

        // When - target with null hostname
        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1");

        // Then
        assertTrue(provider.isDenylisted(target));
    }

    @Test
    void testTargetWithNullIP() throws IOException {
        // Given
        writeDenylist("example.com");
        provider = new DenylistFileProvider(tempFile.toString());

        // When - target with null IP
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");

        // Then
        assertTrue(provider.isDenylisted(target));
    }

    @Test
    void testCIDRBoundaries() throws IOException {
        // Given
        writeDenylist("192.0.2.0/30"); // .0, .1, .2, .3
        provider = new DenylistFileProvider(tempFile.toString());

        // Then
        assertTrue(provider.isDenylisted(createTarget("test.com", "192.0.2.0")));
        assertTrue(provider.isDenylisted(createTarget("test.com", "192.0.2.1")));
        assertTrue(provider.isDenylisted(createTarget("test.com", "192.0.2.2")));
        assertTrue(provider.isDenylisted(createTarget("test.com", "192.0.2.3")));
        assertFalse(provider.isDenylisted(createTarget("test.com", "192.0.2.4")));
    }

    @Test
    void testConcurrentAccess() throws IOException, InterruptedException {
        // Given
        writeDenylist("192.0.2.0/24", "badsite.com");
        provider = new DenylistFileProvider(tempFile.toString());

        // When - multiple threads access isDenylisted
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                ScanTarget target = createTarget("badsite.com", "192.0.2." + index);
                                results[index] = provider.isDenylisted(target);
                            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - all should return true (synchronized method)
        for (boolean result : results) {
            assertTrue(result);
        }
    }

    @Test
    void testIPv6Handling() throws IOException {
        // Given - IPv4 subnet
        writeDenylist("192.0.2.0/24");
        provider = new DenylistFileProvider(tempFile.toString());

        // When - checking IPv6 address against IPv4 subnet
        ScanTarget target = createTarget("example.com", "2001:db8::1");

        // Then - should not crash and return false
        assertFalse(provider.isDenylisted(target));
    }

    private void writeDenylist(String... entries) throws IOException {
        try (FileWriter writer = new FileWriter(tempFile.toFile())) {
            for (String entry : entries) {
                writer.write(entry + "\n");
            }
        }
    }

    private ScanTarget createTarget(String hostname, String ip) {
        ScanTarget target = new ScanTarget();
        target.setHostname(hostname);
        target.setIp(ip);
        return target;
    }
}
