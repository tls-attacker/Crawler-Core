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
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

/** Tests for ScanTarget parsing functionality, particularly IPv6 address handling. */
class ScanTargetTest {

    private static final int DEFAULT_PORT = 443;

    @Test
    void testIPv4AddressWithPort() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("192.168.1.1:8080", DEFAULT_PORT, null);

        assertEquals(1, results.size());
        Pair<ScanTarget, JobStatus> result = results.get(0);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(8080, target.getPort());
    }

    @Test
    void testIPv4AddressWithoutPort() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("192.168.1.1", DEFAULT_PORT, null);

        assertEquals(1, results.size());
        Pair<ScanTarget, JobStatus> result = results.get(0);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
    }

    @Test
    void testIPv6AddressWithPort() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("[2001:db8::1]:8080", DEFAULT_PORT, null);

        assertEquals(1, results.size());
        Pair<ScanTarget, JobStatus> result = results.get(0);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(8080, target.getPort());
    }

    @Test
    void testIPv6AddressWithoutPort() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("2001:db8::1", DEFAULT_PORT, null);

        assertEquals(1, results.size());
        Pair<ScanTarget, JobStatus> result = results.get(0);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
    }

    @Test
    void testIPv6AddressWithPortAndDefaultPort() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("[::1]:443", DEFAULT_PORT, null);

        assertEquals(1, results.size());
        Pair<ScanTarget, JobStatus> result = results.get(0);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("::1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(443, target.getPort());
    }

    @Test
    void testHostnameWithPort() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("example.com:8080", DEFAULT_PORT, null);

        assertFalse(results.isEmpty());
        // Should have at least one result for example.com
        for (Pair<ScanTarget, JobStatus> result : results) {
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals(8080, target.getPort());
            assertNotNull(target.getIp());
        }
    }

    @Test
    void testHostnameWithoutPort() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("example.com", DEFAULT_PORT, null);

        assertFalse(results.isEmpty());
        // Should have at least one result for example.com
        for (Pair<ScanTarget, JobStatus> result : results) {
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals(DEFAULT_PORT, target.getPort());
            assertNotNull(target.getIp());
        }
    }

    @Test
    void testTrancoRankWithHostname() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("1,example.com", DEFAULT_PORT, null);

        assertFalse(results.isEmpty());
        // Should have at least one result for example.com
        for (Pair<ScanTarget, JobStatus> result : results) {
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals(1, target.getTrancoRank());
            assertEquals(DEFAULT_PORT, target.getPort());
            assertNotNull(target.getIp());
        }
    }

    @Test
    void testUrlPrefixRemoval() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("//example.com", DEFAULT_PORT, null);

        assertFalse(results.isEmpty());
        // Should have at least one result for example.com
        for (Pair<ScanTarget, JobStatus> result : results) {
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals(DEFAULT_PORT, target.getPort());
            assertNotNull(target.getIp());
        }
    }

    @Test
    void testQuotedHostname() {
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("\"example.com\"", DEFAULT_PORT, null);

        assertFalse(results.isEmpty());
        // Should have at least one result for example.com
        for (Pair<ScanTarget, JobStatus> result : results) {
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals(DEFAULT_PORT, target.getPort());
            assertNotNull(target.getIp());
        }
    }

    @Test
    void testInvalidPortHandling() {
        // Port out of range should default to defaultPort
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("[2001:db8::1]:99999", DEFAULT_PORT, null);

        assertEquals(1, results.size());
        Pair<ScanTarget, JobStatus> result = results.get(0);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::1", target.getIp());
        assertEquals(DEFAULT_PORT, target.getPort()); // Should use default port for invalid port
    }

    @Test
    void testMalformedPortHandling() {
        // Non-numeric port should default to defaultPort
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("[2001:db8::1]:abc", DEFAULT_PORT, null);

        assertEquals(1, results.size());
        Pair<ScanTarget, JobStatus> result = results.get(0);
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
            List<Pair<ScanTarget, JobStatus>> results =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, null);

            assertEquals(1, results.size());
            Pair<ScanTarget, JobStatus> result = results.get(0);
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals(ipv6, target.getIp());
            assertEquals(8080, target.getPort());
        }
    }

    @Test
    void testMultipleIPResolution() {
        // Test with google.com which typically has multiple A records
        List<Pair<ScanTarget, JobStatus>> results =
                ScanTarget.fromTargetString("google.com", DEFAULT_PORT, null);

        assertFalse(results.isEmpty());
        // All results should be successful
        for (Pair<ScanTarget, JobStatus> result : results) {
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("google.com", target.getHostname());
            assertEquals(DEFAULT_PORT, target.getPort());
            assertNotNull(target.getIp());
            // Verify it's a valid IP address format
            assertTrue(
                    target.getIp()
                            .matches(
                                    "^([0-9]{1,3}\\.){3}[0-9]{1,3}$|^([0-9a-fA-F]*:)+[0-9a-fA-F]*$"));
            // Error fields should be null for successful resolution
            assertNull(target.getErrorMessage());
            assertNull(target.getErrorType());
        }

        // Log the number of IPs found for debugging
        System.out.println("google.com resolved to " + results.size() + " IP address(es)");
    }

    @Test
    void testErrorInformationPreservation() {
        // Test that error information fields are properly initialized and preserved
        ScanTarget target = new ScanTarget();

        // Initially error fields should be null
        assertNull(target.getErrorMessage());
        assertNull(target.getErrorType());

        // Set error information
        target.setErrorMessage("Test error message");
        target.setErrorType("TestException");

        // Verify error information is preserved
        assertEquals("Test error message", target.getErrorMessage());
        assertEquals("TestException", target.getErrorType());
    }

    // Note: Testing unresolvable hostnames is environment-dependent and not reliable
    // for CI/CD environments, so we skip this test
}
