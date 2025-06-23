/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.denylist;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.data.ScanTarget;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DenylistFileProviderTest {

    @TempDir File tempDir;

    private File denylistFile;
    private DenylistFileProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        denylistFile = new File(tempDir, "denylist.txt");
        writeDenylistFile();
        provider = new DenylistFileProvider(denylistFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        if (denylistFile.exists()) {
            denylistFile.delete();
        }
    }

    private void writeDenylistFile() throws IOException {
        try (FileWriter writer = new FileWriter(denylistFile)) {
            // Domain denylists
            writer.write("example.com\n");
            writer.write("blocked-domain.org\n");
            // IP denylists
            writer.write("192.168.1.100\n");
            writer.write("10.0.0.50\n");
            // CIDR denylists
            writer.write("172.16.0.0/16\n");
            writer.write("203.0.113.0/24\n");
        }
    }

    @Test
    void testIsDenylistedWithBlockedDomain() {
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIps(Arrays.asList("1.2.3.4", "5.6.7.8"));

        assertTrue(provider.isDenylisted(target));
    }

    @Test
    void testIsDenylistedWithAllowedDomain() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        target.setIps(Arrays.asList("1.2.3.4", "5.6.7.8"));

        assertFalse(provider.isDenylisted(target));
    }

    @Test
    void testIsDenylistedWithBlockedSingleIp() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        target.setIps(Arrays.asList("192.168.1.100"));

        assertTrue(provider.isDenylisted(target));
    }

    @Test
    void testIsDenylistedWithMultipleIpsOneBlocked() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        // One IP is blocked, others are not
        target.setIps(Arrays.asList("1.2.3.4", "192.168.1.100", "5.6.7.8"));

        assertTrue(provider.isDenylisted(target));
    }

    @Test
    void testIsDenylistedWithMultipleIpsNoneBlocked() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        target.setIps(Arrays.asList("1.2.3.4", "5.6.7.8", "9.10.11.12"));

        assertFalse(provider.isDenylisted(target));
    }

    @Test
    void testIsDenylistedWithCidrBlockedIp() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        // IP is in the 172.16.0.0/16 CIDR range
        target.setIps(Arrays.asList("172.16.5.10"));

        assertTrue(provider.isDenylisted(target));
    }

    @Test
    void testIsDenylistedWithMultipleIpsOneCidrBlocked() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        // One IP is in CIDR range, others are not
        target.setIps(Arrays.asList("1.2.3.4", "203.0.113.50", "5.6.7.8"));

        assertTrue(provider.isDenylisted(target));
    }

    @Test
    void testIsDenylistedWithEmptyIpsList() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        target.setIps(Arrays.asList());

        assertFalse(provider.isDenylisted(target));
    }

    @Test
    void testBackwardCompatibilityWithDeprecatedIpField() {
        ScanTarget target = new ScanTarget();
        target.setHostname("allowed-domain.com");
        // Use deprecated setIp method
        target.setIp("10.0.0.50");

        assertTrue(provider.isDenylisted(target));
    }
}
