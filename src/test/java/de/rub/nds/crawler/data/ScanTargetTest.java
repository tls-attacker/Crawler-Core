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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import java.net.InetAddress;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.mockito.*;

class ScanTargetTest {

    @Mock private IDenylistProvider mockDenylistProvider;
    @Mock private InetAddress mockInetAddress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDefaultConstructor() {
        ScanTarget target = new ScanTarget();
        assertNull(target.getIp());
        assertNull(target.getHostname());
        assertEquals(0, target.getPort());
        assertEquals(0, target.getTrancoRank());
    }

    @Test
    void testSettersAndGetters() {
        ScanTarget target = new ScanTarget();

        target.setIp("192.0.2.1");
        target.setHostname("example.com");
        target.setPort(8443);
        target.setTrancoRank(100);

        assertEquals("192.0.2.1", target.getIp());
        assertEquals("example.com", target.getHostname());
        assertEquals(8443, target.getPort());
        assertEquals(100, target.getTrancoRank());
    }

    @Test
    void testToStringWithHostname() {
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("192.0.2.1");

        assertEquals("example.com", target.toString());
    }

    @Test
    void testToStringWithoutHostname() {
        ScanTarget target = new ScanTarget();
        target.setIp("192.0.2.1");

        assertEquals("192.0.2.1", target.toString());
    }

    @Test
    void testFromTargetStringWithSimpleHostname() {
        // When
        Pair<ScanTarget, JobStatus> result = ScanTarget.fromTargetString("example.com", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(443, target.getPort());
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
    }

    @Test
    void testFromTargetStringWithIP() {
        // When
        Pair<ScanTarget, JobStatus> result = ScanTarget.fromTargetString("192.0.2.1", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("192.0.2.1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(443, target.getPort());
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
    }

    @Test
    void testFromTargetStringWithPort() {
        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("example.com:8443", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(8443, target.getPort());
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
    }

    @Test
    void testFromTargetStringWithTrancoRank() {
        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("100,example.com", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(443, target.getPort());
        assertEquals(100, target.getTrancoRank());
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
    }

    @Test
    void testFromTargetStringWithTrancoRankAndPort() {
        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("100,example.com:8443", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(8443, target.getPort());
        assertEquals(100, target.getTrancoRank());
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
    }

    @Test
    void testFromTargetStringWithInvalidTrancoRank() {
        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("abc,example.com", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("", target.getHostname());
        assertEquals(443, target.getPort());
        assertEquals(0, target.getTrancoRank());
    }

    @Test
    void testFromTargetStringWithMxFormat() {
        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("mx://example.com", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(443, target.getPort());
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
    }

    @Test
    void testFromTargetStringWithQuotes() {
        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("\"example.com\"", 443, null);

        // Then
        ScanTarget target = result.getLeft();
        assertEquals("example.com", target.getHostname());
        assertEquals(443, target.getPort());
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
    }

    @Test
    void testFromTargetStringWithInvalidPort() {
        // When - port 1 is considered invalid
        Pair<ScanTarget, JobStatus> result1 =
                ScanTarget.fromTargetString("example.com:1", 443, null);

        // Then - port is not set (remains 0)
        assertEquals(0, result1.getLeft().getPort());

        // When - port > 65535 is invalid (parseInt will fail)
        try {
            Pair<ScanTarget, JobStatus> result2 =
                    ScanTarget.fromTargetString("example.com:70000", 443, null);
            // If it doesn't throw, port should be 0
            assertEquals(0, result2.getLeft().getPort());
        } catch (NumberFormatException e) {
            // This is expected for port > 65535
        }
    }

    @Test
    void testFromTargetStringWithValidPortBoundaries() {
        // Test port 2 (lowest valid)
        Pair<ScanTarget, JobStatus> result1 =
                ScanTarget.fromTargetString("example.com:2", 443, null);
        assertEquals(2, result1.getLeft().getPort());

        // Test port 65534 (highest valid < 65535)
        Pair<ScanTarget, JobStatus> result2 =
                ScanTarget.fromTargetString("example.com:65534", 443, null);
        assertEquals(65534, result2.getLeft().getPort());
    }

    @Test
    void testFromTargetStringUnknownHost() {
        // This test would normally require mocking static InetAddress.getByName()
        // Since we're testing the real behavior, we'll use a hostname that likely doesn't exist

        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(
                        "this-host-definitely-does-not-exist-xyz123.com", 443, null);

        // Then
        assertEquals(JobStatus.UNRESOLVABLE, result.getRight());
        assertNull(result.getLeft().getIp());
    }

    @Test
    void testFromTargetStringDenylisted() {
        // Given
        when(mockDenylistProvider.isDenylisted(any(ScanTarget.class))).thenReturn(true);

        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.0.2.1", 443, mockDenylistProvider);

        // Then
        assertEquals(JobStatus.DENYLISTED, result.getRight());
        verify(mockDenylistProvider).isDenylisted(any(ScanTarget.class));
    }

    @Test
    void testFromTargetStringNotDenylisted() {
        // Given
        when(mockDenylistProvider.isDenylisted(any(ScanTarget.class))).thenReturn(false);

        // When
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("192.0.2.1", 443, mockDenylistProvider);

        // Then
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        verify(mockDenylistProvider).isDenylisted(any(ScanTarget.class));
    }

    @Test
    void testFromTargetStringComplexScenarios() {
        // Test with everything combined
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString("50,mx://\"example.com\":8080", 443, null);

        ScanTarget target = result.getLeft();
        assertEquals(
                "\"example.com\"",
                target.getHostname()); // Quotes are kept when mixed with // processing
        assertEquals(8080, target.getPort());
        assertEquals(50, target.getTrancoRank());
        assertEquals(
                JobStatus.UNRESOLVABLE,
                result.getRight()); // Hostname with quotes can't be resolved
    }

    @Test
    void testFromTargetStringWithIPv4() {
        // Test various IPv4 formats
        String[] validIPs = {"192.0.2.1", "10.0.0.1", "172.16.0.1", "255.255.255.255"};

        for (String ip : validIPs) {
            Pair<ScanTarget, JobStatus> result = ScanTarget.fromTargetString(ip, 443, null);
            assertEquals(ip, result.getLeft().getIp());
            assertNull(result.getLeft().getHostname());
        }
    }
}
