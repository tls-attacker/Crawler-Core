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
import de.rub.nds.crawler.denylist.IDenylistProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScanTargetTest {

    private IDenylistProvider testDenylistProvider;

    private static final int DEFAULT_PORT = 443;

    // Test implementation of IDenylistProvider
    private static class TestDenylistProvider implements IDenylistProvider {
        private boolean shouldDenylist;

        public TestDenylistProvider(boolean shouldDenylist) {
            this.shouldDenylist = shouldDenylist;
        }

        @Override
        public boolean isDenylisted(ScanTarget target) {
            return shouldDenylist;
        }

        public void setShouldDenylist(boolean shouldDenylist) {
            this.shouldDenylist = shouldDenylist;
        }
    }

    @BeforeEach
    void setUp() {
        testDenylistProvider = new TestDenylistProvider(false);
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
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

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
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

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
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

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
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertEquals(DEFAULT_PORT, target.getPort()); // Should use default port
    }

    @Test
    void testFromTargetStringWithRankAndHostname() {
        // This test needs to use a real hostname that resolves
        // Using localhost as it should always resolve
        String targetString = "1,localhost";

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("localhost", target.getHostname());
        assertNotNull(target.getIp());
        assertTrue(target.getIp().equals("127.0.0.1") || target.getIp().equals("::1"));
        assertEquals(DEFAULT_PORT, target.getPort());
        assertEquals(1, target.getTrancoRank());
    }

    @Test
    void testFromTargetStringWithRankAndHostnameAndPort() {
        String targetString = "100,localhost:8443";

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("localhost", target.getHostname());
        assertNotNull(target.getIp());
        assertTrue(target.getIp().equals("127.0.0.1") || target.getIp().equals("::1"));
        assertEquals(8443, target.getPort());
        assertEquals(100, target.getTrancoRank());
    }

    @Test
    void testFromTargetStringWithMxFormat() {
        String targetString = "//localhost";

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("localhost", target.getHostname());
        assertNotNull(target.getIp());
        assertTrue(target.getIp().equals("127.0.0.1") || target.getIp().equals("::1"));
    }

    @Test
    void testFromTargetStringWithQuotes() {
        String targetString = "\"localhost\"";

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("localhost", target.getHostname());
        assertNotNull(target.getIp());
        assertTrue(target.getIp().equals("127.0.0.1") || target.getIp().equals("::1"));
    }

    @Test
    void testFromTargetStringWithUnresolvableHost() {
        // Use a domain that is guaranteed not to resolve
        String targetString = "non.existent.domain.invalid";

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.UNRESOLVABLE, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("non.existent.domain.invalid", target.getHostname());
        assertNull(target.getIp());
    }

    @Test
    void testFromTargetStringWithDenylistedHost() {
        String targetString = "localhost";

        // Set the denylist provider to return true
        ((TestDenylistProvider) testDenylistProvider).setShouldDenylist(true);

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.DENYLISTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("localhost", target.getHostname());
        assertNotNull(target.getIp());
        assertTrue(target.getIp().equals("127.0.0.1") || target.getIp().equals("::1"));
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
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

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
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

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
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

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
        String targetString = "100,//\"localhost\":25";

        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("localhost", target.getHostname());
        assertNotNull(target.getIp());
        assertTrue(target.getIp().equals("127.0.0.1") || target.getIp().equals("::1"));
        assertEquals(25, target.getPort());
        assertEquals(100, target.getTrancoRank());
    }

    @Test
    void testFromTargetStringPortBoundaryValues() {
        // Test port = 1 (minimum valid)
        String targetString1 = "192.168.1.1:1";
        Pair<ScanTarget, JobStatus> result1 =
                ScanTarget.fromTargetString(targetString1, DEFAULT_PORT, testDenylistProvider);
        assertEquals(
                DEFAULT_PORT,
                result1.getLeft().getPort()); // Should use default as port 1 is not > 1

        // Test port = 2 (first valid)
        String targetString2 = "192.168.1.1:2";
        Pair<ScanTarget, JobStatus> result2 =
                ScanTarget.fromTargetString(targetString2, DEFAULT_PORT, testDenylistProvider);
        assertEquals(2, result2.getLeft().getPort());

        // Test port = 65534 (last valid)
        String targetString3 = "192.168.1.1:65534";
        Pair<ScanTarget, JobStatus> result3 =
                ScanTarget.fromTargetString(targetString3, DEFAULT_PORT, testDenylistProvider);
        assertEquals(65534, result3.getLeft().getPort());

        // Test port = 65535 (invalid - not < 65535)
        String targetString4 = "192.168.1.1:65535";
        Pair<ScanTarget, JobStatus> result4 =
                ScanTarget.fromTargetString(targetString4, DEFAULT_PORT, testDenylistProvider);
        assertEquals(DEFAULT_PORT, result4.getLeft().getPort());
    }
}
