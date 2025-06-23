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
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

class ScanTargetTest {

    @Test
    void testFromTargetStringWithHostname() {
        // Test hostname resolution to multiple IPs
        Pair<ScanTarget, JobStatus> result = ScanTarget.fromTargetString("localhost", 443, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();

        assertEquals("localhost", target.getHostname());
        assertEquals(443, target.getPort());
        assertNotNull(target.getIps());
        assertFalse(target.getIps().isEmpty());
        // localhost should resolve to at least one IP
        assertTrue(target.getIps().size() >= 1);
        // The deprecated getIp() should return the first IP
        assertEquals(target.getIps().get(0), target.getIp());
    }

    @Test
    void testFromTargetStringWithIpAddress() {
        Pair<ScanTarget, JobStatus> result = ScanTarget.fromTargetString("127.0.0.1", 443, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();

        assertNull(target.getHostname());
        assertEquals("127.0.0.1", target.getIp());
        assertEquals(Arrays.asList("127.0.0.1"), target.getIps());
        assertEquals(443, target.getPort());
    }

    @Test
    void testFromTargetStringWithPort() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("127.0.0.1:8443", 443, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();

        assertEquals("127.0.0.1", target.getIp());
        assertEquals(Arrays.asList("127.0.0.1"), target.getIps());
        assertEquals(8443, target.getPort());
    }

    @Test
    void testFromTargetStringWithTrancoRank() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("100,127.0.0.1", 443, null);

        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();

        assertEquals(100, target.getTrancoRank());
        assertEquals("127.0.0.1", target.getIp());
        assertEquals(Arrays.asList("127.0.0.1"), target.getIps());
    }

    @Test
    void testFromTargetStringUnresolvableHost() {
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("this-host-should-not-exist-12345.invalid", 443, null);

        assertEquals(JobStatus.UNRESOLVABLE, result.getRight());
    }

    @Test
    void testSetIpsAndBackwardCompatibility() {
        ScanTarget target = new ScanTarget();
        List<String> ips = Arrays.asList("192.168.1.1", "192.168.1.2", "192.168.1.3");

        target.setIps(ips);

        // Check that all IPs are stored
        assertEquals(ips, target.getIps());
        // Check backward compatibility - getIp() should return the first IP
        assertEquals("192.168.1.1", target.getIp());
    }

    @Test
    void testSetIpBackwardCompatibility() {
        ScanTarget target = new ScanTarget();

        // Test deprecated setIp method
        target.setIp("10.0.0.1");

        assertEquals("10.0.0.1", target.getIp());
        assertEquals(Arrays.asList("10.0.0.1"), target.getIps());

        // Setting another IP should update the list
        target.setIp("10.0.0.2");
        assertEquals("10.0.0.2", target.getIp());
        assertEquals(Arrays.asList("10.0.0.2"), target.getIps());
    }

    @Test
    void testToStringWithMultipleIps() {
        ScanTarget target = new ScanTarget();

        // Test with hostname
        target.setHostname("example.com");
        target.setIps(Arrays.asList("192.168.1.1", "192.168.1.2"));
        assertEquals("example.com", target.toString());

        // Test with single IP (no hostname)
        target = new ScanTarget();
        target.setIps(Arrays.asList("192.168.1.1"));
        assertEquals("192.168.1.1", target.toString());

        // Test with multiple IPs (no hostname)
        target = new ScanTarget();
        target.setIps(Arrays.asList("192.168.1.1", "192.168.1.2", "192.168.1.3"));
        assertEquals("[192.168.1.1, 192.168.1.2, 192.168.1.3]", target.toString());
    }
}
