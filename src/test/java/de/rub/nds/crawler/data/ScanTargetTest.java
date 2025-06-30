/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.constant.JobStatus;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class ScanTargetTest {

    @Test
    void testIPv4WithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.168.1.1:8080", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("192.168.1.1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(8080, result.getLeft().getPort());
        assertNull(result.getLeft().getHostname());
    }

    @Test
    void testIPv4WithoutPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.168.1.1", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("192.168.1.1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(443, result.getLeft().getPort());
        assertNull(result.getLeft().getHostname());
    }

    @Test
    void testIPv6WithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("[2001:db8::1]:8080", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("2001:db8::1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(8080, result.getLeft().getPort());
        assertNull(result.getLeft().getHostname());
    }

    @Test
    void testIPv6WithoutPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("2001:db8::1", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("2001:db8::1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(443, result.getLeft().getPort());
        assertNull(result.getLeft().getHostname());
    }

    @Test
    void testIPv6FullAddressWithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(
                        "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8443", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals(
                "2001:0db8:85a3:0000:0000:8a2e:0370:7334", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(8443, result.getLeft().getPort());
        assertNull(result.getLeft().getHostname());
    }

    @Test
    void testIPv6CompressedAddress() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("::1", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("::1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(443, result.getLeft().getPort());
        assertNull(result.getLeft().getHostname());
    }

    @Test
    void testHostnameWithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("example.com:8080", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("example.com", result.getLeft().getHostname()); // $NON-NLS-1$
        assertEquals(8080, result.getLeft().getPort());
        // IP will be resolved by DNS lookup, so we can't test the exact value
        assertNotNull(result.getLeft().getIp());
    }

    @Test
    void testHostnameWithoutPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("example.com", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("example.com", result.getLeft().getHostname()); // $NON-NLS-1$
        assertEquals(443, result.getLeft().getPort());
        assertNotNull(result.getLeft().getIp());
    }

    @Test
    void testInvalidPortRangeHigh() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.168.1.1:70000", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("192.168.1.1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(443, result.getLeft().getPort()); // Should use default port
    }

    @Test
    void testInvalidPortRangeLow() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.168.1.1:0", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("192.168.1.1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(443, result.getLeft().getPort()); // Should use default port
    }

    @Test
    void testInvalidPortFormat() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("[2001:db8::1]:abc", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("2001:db8::1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(443, result.getLeft().getPort()); // Should use default port
    }

    @Test
    void testTrancoRankWithIPv4() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("100,192.168.1.1:8080", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("192.168.1.1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(8080, result.getLeft().getPort());
        assertEquals(100, result.getLeft().getTrancoRank());
    }

    @Test
    void testTrancoRankWithIPv6() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("200,[2001:db8::1]:8080", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        assertEquals("2001:db8::1", result.getLeft().getIp()); // $NON-NLS-1$
        assertEquals(8080, result.getLeft().getPort());
        assertEquals(200, result.getLeft().getTrancoRank());
    }

    @Test
    void testUnknownHost() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(
                        "this-host-should-not-exist-12345.com", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.UNRESOLVABLE, result.getRight());
        assertEquals(
                "this-host-should-not-exist-12345.com",
                result.getLeft().getHostname()); // $NON-NLS-1$
        assertNull(result.getLeft().getIp());
    }

    @Test
    void testMalformedIPv6Bracket() {
        // Missing closing bracket - should result in UNRESOLVABLE
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("[2001:db8::1:8080", 443, null); // $NON-NLS-1$
        assertEquals(JobStatus.UNRESOLVABLE, result.getRight());
        // Should use default port
        assertEquals(443, result.getLeft().getPort());
    }
}
