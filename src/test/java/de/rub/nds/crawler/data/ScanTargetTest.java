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
import static org.mockito.Mockito.*;

import de.rub.nds.crawler.constant.JobStatus;
import de.rub.nds.crawler.denylist.IDenylistProvider;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScanTargetTest {

    @Mock private IDenylistProvider denylistProvider;

    private static final int DEFAULT_PORT = 443;

    @BeforeEach
    void setUp() {
        when(denylistProvider.isDenylisted(any())).thenReturn(false);
    }

    @Test
    void testConstructor() {
        ScanTarget target = new ScanTarget();
        assertNotNull(target);
        assertNull(target.getIp());
        assertNull(target.getHostname());
        assertEquals(0, target.getPort());
        assertEquals(0, target.getTrancoRank());
    }

    @Test
    void testGettersAndSetters() {
        ScanTarget target = new ScanTarget();

        target.setIp("192.168.1.1");
        assertEquals("192.168.1.1", target.getIp());

        target.setHostname("example.com");
        assertEquals("example.com", target.getHostname());

        target.setPort(8080);
        assertEquals(8080, target.getPort());

        target.setTrancoRank(100);
        assertEquals(100, target.getTrancoRank());
    }

    @Test
    void testToStringWithHostname() {
        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("192.168.1.1");
        assertEquals("example.com", target.toString());
    }

    @Test
    void testToStringWithoutHostname() {
        ScanTarget target = new ScanTarget();
        target.setIp("192.168.1.1");
        assertEquals("192.168.1.1", target.toString());
    }

    @Test
    void testFromTargetStringWithValidIp() {
        String targetString = "192.168.1.1";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
        assertEquals(0, target.getTrancoRank());
    }

    @Test
    void testFromTargetStringWithValidIpAndPort() {
        String targetString = "192.168.1.1:8080";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertNull(target.getHostname());
        assertEquals(8080, target.getPort());
        assertEquals(0, target.getTrancoRank());
    }

    @Test
    void testFromTargetStringWithInvalidPort() {
        String targetString = "192.168.1.1:0";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertEquals(DEFAULT_PORT, target.getPort()); // Should use default port
    }

    @Test
    void testFromTargetStringWithPortOutOfRange() {
        String targetString = "192.168.1.1:70000";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertEquals(DEFAULT_PORT, target.getPort()); // Should use default port
    }

    @Test
    void testFromTargetStringWithRankAndHostname() {
        String targetString = "1,example.com";

        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            InetAddress mockAddress = mock(InetAddress.class);
            when(mockAddress.getHostAddress()).thenReturn("93.184.216.34");
            mockedInetAddress
                    .when(() -> InetAddress.getByName("example.com"))
                    .thenReturn(mockAddress);

            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

            assertNotNull(result);
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals("93.184.216.34", target.getIp());
            assertEquals(DEFAULT_PORT, target.getPort());
            assertEquals(1, target.getTrancoRank());
        }
    }

    @Test
    void testFromTargetStringWithRankAndHostnameAndPort() {
        String targetString = "100,example.com:8443";

        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            InetAddress mockAddress = mock(InetAddress.class);
            when(mockAddress.getHostAddress()).thenReturn("93.184.216.34");
            mockedInetAddress
                    .when(() -> InetAddress.getByName("example.com"))
                    .thenReturn(mockAddress);

            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

            assertNotNull(result);
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals("93.184.216.34", target.getIp());
            assertEquals(8443, target.getPort());
            assertEquals(100, target.getTrancoRank());
        }
    }

    @Test
    void testFromTargetStringWithMxFormat() {
        String targetString = "//mail.example.com";

        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            InetAddress mockAddress = mock(InetAddress.class);
            when(mockAddress.getHostAddress()).thenReturn("93.184.216.35");
            mockedInetAddress
                    .when(() -> InetAddress.getByName("mail.example.com"))
                    .thenReturn(mockAddress);

            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

            assertNotNull(result);
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("mail.example.com", target.getHostname());
            assertEquals("93.184.216.35", target.getIp());
        }
    }

    @Test
    void testFromTargetStringWithQuotes() {
        String targetString = "\"example.com\"";

        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            InetAddress mockAddress = mock(InetAddress.class);
            when(mockAddress.getHostAddress()).thenReturn("93.184.216.34");
            mockedInetAddress
                    .when(() -> InetAddress.getByName("example.com"))
                    .thenReturn(mockAddress);

            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

            assertNotNull(result);
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("example.com", target.getHostname());
            assertEquals("93.184.216.34", target.getIp());
        }
    }

    @Test
    void testFromTargetStringWithUnresolvableHost() {
        String targetString = "non.existent.domain.xyz";

        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            mockedInetAddress
                    .when(() -> InetAddress.getByName("non.existent.domain.xyz"))
                    .thenThrow(new UnknownHostException("Host not found"));

            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

            assertNotNull(result);
            assertEquals(JobStatus.UNRESOLVABLE, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("non.existent.domain.xyz", target.getHostname());
            assertNull(target.getIp());
        }
    }

    @Test
    void testFromTargetStringWithDenylistedHost() {
        String targetString = "denylisted.com";

        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            InetAddress mockAddress = mock(InetAddress.class);
            when(mockAddress.getHostAddress()).thenReturn("10.0.0.1");
            mockedInetAddress
                    .when(() -> InetAddress.getByName("denylisted.com"))
                    .thenReturn(mockAddress);

            when(denylistProvider.isDenylisted(any())).thenReturn(true);

            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

            assertNotNull(result);
            assertEquals(JobStatus.DENYLISTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("denylisted.com", target.getHostname());
            assertEquals("10.0.0.1", target.getIp());
        }
    }

    @Test
    void testFromTargetStringWithNullDenylistProvider() {
        String targetString = "192.168.1.1";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, null);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
    }

    @Test
    void testFromTargetStringWithInvalidCommaFormat() {
        String targetString = "abc,def,ghi";

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("", target.getIp());
        assertNull(target.getHostname());
    }

    @Test
    void testFromTargetStringWithIpv6() {
        String targetString = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", target.getIp());
        assertNull(target.getHostname());
        assertEquals(DEFAULT_PORT, target.getPort());
    }

    @Test
    void testFromTargetStringWithCompressedIpv6() {
        String targetString = "2001:db8::8a2e:370:7334";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("2001:db8::8a2e:370:7334", target.getIp());
        assertNull(target.getHostname());
    }

    @Test
    void testSerializable() {
        ScanTarget target = new ScanTarget();
        assertTrue(java.io.Serializable.class.isAssignableFrom(target.getClass()));
    }

    @Test
    void testFromTargetStringWithComplexMxFormat() {
        String targetString = "100,//\"mail.example.com\":25";

        try (MockedStatic<InetAddress> mockedInetAddress = mockStatic(InetAddress.class)) {
            InetAddress mockAddress = mock(InetAddress.class);
            when(mockAddress.getHostAddress()).thenReturn("93.184.216.35");
            mockedInetAddress
                    .when(() -> InetAddress.getByName("mail.example.com"))
                    .thenReturn(mockAddress);

            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, denylistProvider);

            assertNotNull(result);
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("mail.example.com", target.getHostname());
            assertEquals("93.184.216.35", target.getIp());
            assertEquals(25, target.getPort());
            assertEquals(100, target.getTrancoRank());
        }
    }

    @Test
    void testFromTargetStringPortBoundaryValues() {
        // Test port = 1 (minimum valid)
        String targetString1 = "192.168.1.1:1";
        Pair<ScanTarget, JobStatus> result1 =
                ScanTarget.fromTargetString(targetString1, DEFAULT_PORT, denylistProvider);
        assertEquals(
                DEFAULT_PORT,
                result1.getLeft().getPort()); // Should use default as port 1 is not > 1

        // Test port = 2 (first valid)
        String targetString2 = "192.168.1.1:2";
        Pair<ScanTarget, JobStatus> result2 =
                ScanTarget.fromTargetString(targetString2, DEFAULT_PORT, denylistProvider);
        assertEquals(2, result2.getLeft().getPort());

        // Test port = 65534 (last valid)
        String targetString3 = "192.168.1.1:65534";
        Pair<ScanTarget, JobStatus> result3 =
                ScanTarget.fromTargetString(targetString3, DEFAULT_PORT, denylistProvider);
        assertEquals(65534, result3.getLeft().getPort());

        // Test port = 65535 (invalid - not < 65535)
        String targetString4 = "192.168.1.1:65535";
        Pair<ScanTarget, JobStatus> result4 =
                ScanTarget.fromTargetString(targetString4, DEFAULT_PORT, denylistProvider);
        assertEquals(DEFAULT_PORT, result4.getLeft().getPort());
    }
}
