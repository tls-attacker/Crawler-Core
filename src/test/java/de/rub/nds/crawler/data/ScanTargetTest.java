/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.constant.JobStatus;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/** Tests for ScanTarget parsing functionality, particularly IPv6 address handling. */
class ScanTargetTest {

    private static final int DEFAULT_PORT = 443;

    @Test
    void testIPv4AddressWithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.168.1.1:8080", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(8080, target.getPort());
    }

    @Test
    void testIPv4AddressWithoutPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.168.1.1", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
    }

    @Test
    void testIPv6AddressWithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("[2001:db8::1]:8080", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(8080, target.getPort());
    }

    @Test
    void testIPv6AddressWithoutPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("2001:db8::1", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
    }

    @Test
    void testIPv6AddressWithPortAndDefaultPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("[::1]:443", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("::1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(443, target.getPort());
    }

    @Test
    void testHostnameWithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("example.com:8080", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(8080, target.getPort());
        // IP will be resolved, so we just check it's not null
        assertNotNull(target.getIp());
    }

    @Test
    void testHostnameWithoutPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("example.com", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
        // IP will be resolved, so we just check it's not null
        assertNotNull(target.getIp());
    }

    @Test
    void testTrancoRankWithHostname() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("1,example.com", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(1, target.getTrancoRank());
        assertEquals(DEFAULT_PORT, target.getPort());
        assertNotNull(target.getIp());
    }

    @Test
    void testUrlPrefixRemoval() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("//example.com", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
        assertNotNull(target.getIp());
    }

    @Test
    void testQuotedHostname() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("\"example.com\"", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
        assertNotNull(target.getIp());
    }

    @Test
    void testInvalidPortHandling() {
        // Port out of range should default to defaultPort
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("[2001:db8::1]:99999", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::1", target.getIp());
        assertEquals(DEFAULT_PORT, target.getPort()); // Should use default port for invalid port
    }

    @Test
    void testMalformedPortHandling() {
        // Non-numeric port should default to defaultPort
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("[2001:db8::1]:abc", DEFAULT_PORT, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::1", target.getIp());
        assertEquals(DEFAULT_PORT, target.getPort()); // Should use default port for invalid port
    }

    @Test
    void testComplexIPv6Addresses() {
        // Test various IPv6 address formats
        String[] ipv6Addresses = {
            "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
            "2001:db8:85a3::8a2e:370:7334",
            "::1",
            "::",
            "2001:db8::8a2e:370:7334"
        };

        for (String ipv6 : ipv6Addresses) {
            String targetString = "[" + ipv6 + "]:8080";
            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, null);

            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals(ipv6, target.getIp());
            assertEquals(8080, target.getPort());
        }
    }

    // Note: Testing unresolvable hostnames is environment-dependent and not reliable
    // for CI/CD environments, so we skip this test
}
