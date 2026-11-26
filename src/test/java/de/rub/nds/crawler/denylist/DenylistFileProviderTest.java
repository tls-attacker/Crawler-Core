/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.denylist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.rub.nds.crawler.data.ScanTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DenylistFileProviderTest {

    private Path tempDenylistFile;

    @BeforeEach
    public void setUp() throws IOException {
        tempDenylistFile = Files.createTempFile("denylist", ".txt");
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (tempDenylistFile != null && Files.exists(tempDenylistFile)) {
            Files.delete(tempDenylistFile);
        }
    }

    @Test
    public void testEmptyDenylistFile() throws IOException {
        Files.write(tempDenylistFile, List.of());
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("192.168.1.1");

        assertFalse(provider.isDenylisted(target));
    }

    @Test
    public void testDomainDenylist() throws IOException {
        List<String> denylistEntries =
                List.of("blocked.example.com", "malware.example.org", "spam.example.net");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget blockedTarget = new ScanTarget();
        blockedTarget.setHostname("blocked.example.com");
        blockedTarget.setIp("10.0.0.1");
        assertTrue(provider.isDenylisted(blockedTarget));

        ScanTarget malwareTarget = new ScanTarget();
        malwareTarget.setHostname("malware.example.org");
        malwareTarget.setIp("10.0.0.2");
        assertTrue(provider.isDenylisted(malwareTarget));

        ScanTarget allowedTarget = new ScanTarget();
        allowedTarget.setHostname("allowed.example.com");
        allowedTarget.setIp("10.0.0.3");
        assertFalse(provider.isDenylisted(allowedTarget));
    }

    @Test
    public void testIpDenylist() throws IOException {
        List<String> denylistEntries = List.of("192.168.1.100", "10.0.0.50", "172.16.0.1");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget blockedIpTarget = new ScanTarget();
        blockedIpTarget.setHostname("example.com");
        blockedIpTarget.setIp("192.168.1.100");
        assertTrue(provider.isDenylisted(blockedIpTarget));

        ScanTarget allowedIpTarget = new ScanTarget();
        allowedIpTarget.setHostname("example.org");
        allowedIpTarget.setIp("192.168.1.101");
        assertFalse(provider.isDenylisted(allowedIpTarget));
    }

    @Test
    public void testSubnetDenylist() throws IOException {
        List<String> denylistEntries = List.of("192.168.1.0/24", "10.0.0.0/16", "172.16.0.0/12");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget blockedSubnetTarget1 = new ScanTarget();
        blockedSubnetTarget1.setHostname("host1.com");
        blockedSubnetTarget1.setIp("192.168.1.50");
        assertTrue(provider.isDenylisted(blockedSubnetTarget1));

        ScanTarget blockedSubnetTarget2 = new ScanTarget();
        blockedSubnetTarget2.setHostname("host2.com");
        blockedSubnetTarget2.setIp("10.0.100.200");
        assertTrue(provider.isDenylisted(blockedSubnetTarget2));

        ScanTarget blockedSubnetTarget3 = new ScanTarget();
        blockedSubnetTarget3.setHostname("host3.com");
        blockedSubnetTarget3.setIp("172.20.0.1");
        assertTrue(provider.isDenylisted(blockedSubnetTarget3));

        ScanTarget allowedSubnetTarget = new ScanTarget();
        allowedSubnetTarget.setHostname("allowed.com");
        allowedSubnetTarget.setIp("8.8.8.8");
        assertFalse(provider.isDenylisted(allowedSubnetTarget));
    }

    @Test
    public void testMixedDenylist() throws IOException {
        List<String> denylistEntries =
                List.of(
                        "blocked.example.com",
                        "192.168.1.100",
                        "10.0.0.0/8",
                        "evil.org",
                        "172.16.1.1");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget domainTarget = new ScanTarget();
        domainTarget.setHostname("blocked.example.com");
        domainTarget.setIp("1.2.3.4");
        assertTrue(provider.isDenylisted(domainTarget));

        ScanTarget ipTarget = new ScanTarget();
        ipTarget.setHostname("somehost.com");
        ipTarget.setIp("192.168.1.100");
        assertTrue(provider.isDenylisted(ipTarget));

        ScanTarget subnetTarget = new ScanTarget();
        subnetTarget.setHostname("internal.com");
        subnetTarget.setIp("10.20.30.40");
        assertTrue(provider.isDenylisted(subnetTarget));
    }

    @Test
    public void testInvalidEntries() throws IOException {
        List<String> denylistEntries =
                List.of(
                        "valid.example.com",
                        "invalid-entry-!@#",
                        "192.168.1.1",
                        "not-a-valid-ip-999.999.999.999",
                        "192.168.1.0/24",
                        "invalid/subnet/999");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget validDomainTarget = new ScanTarget();
        validDomainTarget.setHostname("valid.example.com");
        validDomainTarget.setIp("1.1.1.1");
        assertTrue(provider.isDenylisted(validDomainTarget));

        ScanTarget validIpTarget = new ScanTarget();
        validIpTarget.setHostname("host.com");
        validIpTarget.setIp("192.168.1.1");
        assertTrue(provider.isDenylisted(validIpTarget));

        ScanTarget validSubnetTarget = new ScanTarget();
        validSubnetTarget.setHostname("subnet.com");
        validSubnetTarget.setIp("192.168.1.50");
        assertTrue(provider.isDenylisted(validSubnetTarget));
    }

    @Test
    public void testNonExistentFile() {
        DenylistFileProvider provider = new DenylistFileProvider("/non/existent/file.txt");

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("192.168.1.1");

        assertFalse(provider.isDenylisted(target));
    }

    @Test
    public void testTargetWithNullValues() throws IOException {
        List<String> denylistEntries = List.of("blocked.example.com", "192.168.1.100");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget targetWithNullHostname = new ScanTarget();
        targetWithNullHostname.setIp("192.168.1.100");
        assertTrue(provider.isDenylisted(targetWithNullHostname));

        ScanTarget targetWithNullIp = new ScanTarget();
        targetWithNullIp.setHostname("blocked.example.com");
        assertTrue(provider.isDenylisted(targetWithNullIp));

        ScanTarget targetWithBothNull = new ScanTarget();
        assertFalse(provider.isDenylisted(targetWithBothNull));
    }

    @Test
    public void testIpv6Handling() throws IOException {
        List<String> denylistEntries = List.of("2001:db8::1", "192.168.1.0/24", "ipv6.example.com");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget ipv6Target = new ScanTarget();
        ipv6Target.setHostname("host.com");
        ipv6Target.setIp("2001:db8::1");
        assertTrue(provider.isDenylisted(ipv6Target));

        ScanTarget ipv6NotInIpv4Subnet = new ScanTarget();
        ipv6NotInIpv4Subnet.setHostname("host2.com");
        ipv6NotInIpv4Subnet.setIp("2001:db8::2");
        assertFalse(provider.isDenylisted(ipv6NotInIpv4Subnet));
    }

    @Test
    public void testConcurrentAccess() throws IOException, InterruptedException {
        List<String> denylistEntries = List.of("blocked.example.com", "192.168.1.0/24", "10.0.0.1");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        Thread[] threads = new Thread[10];
        boolean[] results = new boolean[threads.length];

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                ScanTarget target = new ScanTarget();
                                if (index % 2 == 0) {
                                    target.setHostname("blocked.example.com");
                                    target.setIp("1.1.1.1");
                                } else {
                                    target.setHostname("allowed.example.com");
                                    target.setIp("2.2.2.2");
                                }
                                results[index] = provider.isDenylisted(target);
                            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 0; i < results.length; i++) {
            if (i % 2 == 0) {
                assertTrue(results[i]);
            } else {
                assertFalse(results[i]);
            }
        }
    }

    @Test
    public void testSubnetBoundaries() throws IOException {
        List<String> denylistEntries = List.of("192.168.1.0/30");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget networkAddress = new ScanTarget();
        networkAddress.setIp("192.168.1.0");
        assertFalse(provider.isDenylisted(networkAddress));

        ScanTarget firstHost = new ScanTarget();
        firstHost.setIp("192.168.1.1");
        assertTrue(provider.isDenylisted(firstHost));

        ScanTarget lastHost = new ScanTarget();
        lastHost.setIp("192.168.1.2");
        assertTrue(provider.isDenylisted(lastHost));

        ScanTarget broadcastAddress = new ScanTarget();
        broadcastAddress.setIp("192.168.1.3");
        assertFalse(provider.isDenylisted(broadcastAddress));

        ScanTarget outsideSubnet = new ScanTarget();
        outsideSubnet.setIp("192.168.1.4");
        assertFalse(provider.isDenylisted(outsideSubnet));
    }

    @Test
    public void testLargeSubnet() throws IOException {
        List<String> denylistEntries = List.of("10.0.0.0/8");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget inSubnet1 = new ScanTarget();
        inSubnet1.setIp("10.0.0.1");
        assertTrue(provider.isDenylisted(inSubnet1));

        ScanTarget inSubnet2 = new ScanTarget();
        inSubnet2.setIp("10.255.255.254");
        assertTrue(provider.isDenylisted(inSubnet2));

        ScanTarget outsideSubnet = new ScanTarget();
        outsideSubnet.setIp("11.0.0.1");
        assertFalse(provider.isDenylisted(outsideSubnet));
    }

    @Test
    public void testFileWithEmptyLines() throws IOException {
        List<String> denylistEntries =
                List.of("", "blocked.example.com", "", "192.168.1.1", "", "10.0.0.0/16", "");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget blockedDomain = new ScanTarget();
        blockedDomain.setHostname("blocked.example.com");
        assertTrue(provider.isDenylisted(blockedDomain));

        ScanTarget blockedIp = new ScanTarget();
        blockedIp.setIp("192.168.1.1");
        assertTrue(provider.isDenylisted(blockedIp));

        ScanTarget blockedSubnet = new ScanTarget();
        blockedSubnet.setIp("10.0.100.50");
        assertTrue(provider.isDenylisted(blockedSubnet));
    }

    @Test
    public void testSpecialCharactersInFile() throws IOException {
        List<String> denylistEntries =
                List.of(
                        "example.com#comment",
                        "192.168.1.1 # another comment",
                        "test@example.com",
                        "sub.domain.example.com");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget subdomainTarget = new ScanTarget();
        subdomainTarget.setHostname("sub.domain.example.com");
        assertTrue(provider.isDenylisted(subdomainTarget));

        ScanTarget commentedDomain = new ScanTarget();
        commentedDomain.setHostname("example.com#comment");
        assertFalse(provider.isDenylisted(commentedDomain));
    }

    @Test
    public void testInvalidCidrNotation() throws IOException {
        List<String> denylistEntries = List.of("192.168.1.0/24", "192.168.2.0/abc", "10.0.0.0/8");
        Files.write(tempDenylistFile, denylistEntries);
        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget validSubnetTarget = new ScanTarget();
        validSubnetTarget.setIp("192.168.1.50");
        assertTrue(provider.isDenylisted(validSubnetTarget));

        ScanTarget validSubnetTarget2 = new ScanTarget();
        validSubnetTarget2.setIp("10.0.0.1");
        assertTrue(provider.isDenylisted(validSubnetTarget2));

        ScanTarget invalidSubnetTarget = new ScanTarget();
        invalidSubnetTarget.setIp("192.168.2.50");
        assertFalse(provider.isDenylisted(invalidSubnetTarget));
    }
}
