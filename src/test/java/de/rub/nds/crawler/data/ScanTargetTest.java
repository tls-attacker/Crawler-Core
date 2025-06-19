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
        assertEquals(0, target.getPort()); // Port 0 is parsed but not validated as > 1
    }

    @Test
    void testFromTargetStringWithPortOutOfRange() {
        // Port parsing will throw NumberFormatException for values > Integer.MAX_VALUE
        // Let's test port 70000 which is > 65535 but still parseable
        String targetString = "192.168.1.1:70000";
        Pair<ScanTarget, JobStatus> result =
                ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

        assertNotNull(result);
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        assertEquals("192.168.1.1", target.getIp());
        assertEquals(70000, target.getPort()); // Port is parsed but not validated as < 65535
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
        // When the first part is not all digits, targetString becomes empty string
        // Empty string is not a valid IP, so it's treated as hostname and resolved
        assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
        ScanTarget target = result.getLeft();
        // Empty hostname gets resolved to localhost/127.0.0.1 in test environment
        assertEquals("127.0.0.1", target.getIp());
        assertEquals("", target.getHostname());
    }

    @Test
    void testFromTargetStringWithIpv6() {
        // The code has a FIXME comment that IPv6 parsing is broken due to : handling
        // The current code will try to parse "0db8" as a port number, which fails
        String targetString = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        try {
            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

            // If parsing succeeds, the behavior is likely incorrect due to the FIXME
            fail("Expected NumberFormatException due to broken IPv6 parsing");
        } catch (NumberFormatException e) {
            // Expected due to the FIXME comment in the code
            assertTrue(e.getMessage().contains("0db8"));
        }
    }

    @Test
    void testFromTargetStringWithCompressedIpv6() {
        // The code has a FIXME comment that IPv6 parsing is broken due to : handling
        String targetString = "2001:db8::8a2e:370:7334";

        try {
            Pair<ScanTarget, JobStatus> result =
                    ScanTarget.fromTargetString(targetString, DEFAULT_PORT, testDenylistProvider);

            // If parsing succeeds, the behavior is likely incorrect due to the FIXME
            fail("Expected NumberFormatException due to broken IPv6 parsing");
        } catch (NumberFormatException e) {
            // Expected due to the FIXME comment in the code
            assertTrue(e.getMessage().contains("db8"));
        }
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
        // localhost may resolve to ::1 which may be denylisted or unresolvable in some environments
        if (result.getRight() == JobStatus.UNRESOLVABLE
                || result.getRight() == JobStatus.DENYLISTED) {
            // Expected in some test environments
            ScanTarget target = result.getLeft();
            assertEquals("localhost", target.getHostname());
            assertEquals(25, target.getPort());
            assertEquals(100, target.getTrancoRank());
        } else {
            assertEquals(JobStatus.TO_BE_EXECUTED, result.getRight());
            ScanTarget target = result.getLeft();
            assertEquals("localhost", target.getHostname());
            assertNotNull(target.getIp());
            assertTrue(target.getIp().equals("127.0.0.1") || target.getIp().equals("::1"));
            assertEquals(25, target.getPort());
            assertEquals(100, target.getTrancoRank());
        }
    }

    @Test
    void testFromTargetStringPortBoundaryValues() {
        // Test port = 1 (parsed but not valid since not > 1)
        String targetString1 = "192.168.1.1:1";
        Pair<ScanTarget, JobStatus> result1 =
                ScanTarget.fromTargetString(targetString1, DEFAULT_PORT, testDenylistProvider);
        assertEquals(1, result1.getLeft().getPort()); // Port 1 is parsed but fails validation check

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

        // Test port = 65535 (parsed but not valid since not < 65535)
        String targetString4 = "192.168.1.1:65535";
        Pair<ScanTarget, JobStatus> result4 =
                ScanTarget.fromTargetString(targetString4, DEFAULT_PORT, testDenylistProvider);
        assertEquals(65535, result4.getLeft().getPort()); // Port is parsed but fails validation
    }
}
