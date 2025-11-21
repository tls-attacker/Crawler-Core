/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.denylist;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.data.ScanTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
        Files.deleteIfExists(tempDenylistFile);
    }

    @Test
    public void testEmptyDenylist() throws IOException {
        Files.write(tempDenylistFile, Arrays.asList());

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("192.168.1.1");

        assertFalse(provider.isDenylisted(target));
    }

    @Test
    public void testDomainDenylist() throws IOException {
        Files.write(tempDenylistFile, Arrays.asList("badsite.com", "malicious.org"));

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget denylisted = new ScanTarget();
        denylisted.setHostname("badsite.com");
        denylisted.setIp("10.0.0.1");
        assertTrue(provider.isDenylisted(denylisted));

        ScanTarget allowed = new ScanTarget();
        allowed.setHostname("goodsite.com");
        allowed.setIp("10.0.0.2");
        assertFalse(provider.isDenylisted(allowed));
    }

    @Test
    public void testIpDenylist() throws IOException {
        Files.write(tempDenylistFile, Arrays.asList("192.168.1.1", "10.0.0.1"));

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget denylisted = new ScanTarget();
        denylisted.setHostname("anysite.com");
        denylisted.setIp("192.168.1.1");
        assertTrue(provider.isDenylisted(denylisted));

        ScanTarget allowed = new ScanTarget();
        allowed.setHostname("anothersite.com");
        allowed.setIp("192.168.1.2");
        assertFalse(provider.isDenylisted(allowed));
    }

    @Test
    public void testSubnetDenylist() throws IOException {
        Files.write(tempDenylistFile, Arrays.asList("192.168.1.0/24", "10.0.0.0/16"));

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        ScanTarget denylisted1 = new ScanTarget();
        denylisted1.setHostname("host1.com");
        denylisted1.setIp("192.168.1.100");
        assertTrue(provider.isDenylisted(denylisted1));

        ScanTarget denylisted2 = new ScanTarget();
        denylisted2.setHostname("host2.com");
        denylisted2.setIp("10.0.50.1");
        assertTrue(provider.isDenylisted(denylisted2));

        ScanTarget allowed = new ScanTarget();
        allowed.setHostname("host3.com");
        allowed.setIp("172.16.0.1");
        assertFalse(provider.isDenylisted(allowed));
    }

    @Test
    public void testMixedDenylist() throws IOException {
        Files.write(
                tempDenylistFile,
                Arrays.asList("badsite.com", "192.168.1.1", "10.0.0.0/8", "evil.org"));

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        // Test domain denylist
        ScanTarget domainTarget = new ScanTarget();
        domainTarget.setHostname("badsite.com");
        domainTarget.setIp("1.2.3.4");
        assertTrue(provider.isDenylisted(domainTarget));

        // Test IP denylist
        ScanTarget ipTarget = new ScanTarget();
        ipTarget.setHostname("somesite.com");
        ipTarget.setIp("192.168.1.1");
        assertTrue(provider.isDenylisted(ipTarget));

        // Test subnet denylist
        ScanTarget subnetTarget = new ScanTarget();
        subnetTarget.setHostname("anothersite.com");
        subnetTarget.setIp("10.5.5.5");
        assertTrue(provider.isDenylisted(subnetTarget));
    }

    @Test
    public void testInvalidEntries() throws IOException {
        Files.write(
                tempDenylistFile,
                Arrays.asList(
                        "validsite.com",
                        "invalid-entry-no-validation",
                        "192.168.1.1",
                        "invalid/subnet",
                        "192.168.1.0/24"));

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        // Valid entries should still work
        ScanTarget validTarget = new ScanTarget();
        validTarget.setHostname("validsite.com");
        validTarget.setIp("1.1.1.1");
        assertTrue(provider.isDenylisted(validTarget));

        // Invalid entries should be ignored
        ScanTarget testTarget = new ScanTarget();
        testTarget.setHostname("invalid-entry-no-validation");
        testTarget.setIp("192.168.2.1");
        assertFalse(provider.isDenylisted(testTarget));
    }

    @Test
    public void testNonExistentFile() {
        String nonExistentFile = "/tmp/nonexistent_denylist_file_" + System.currentTimeMillis();
        DenylistFileProvider provider = new DenylistFileProvider(nonExistentFile);

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("192.168.1.1");

        // Should not crash and should return false
        assertFalse(provider.isDenylisted(target));
    }

    @Test
    public void testIpv6SubnetHandling() throws IOException {
        Files.write(tempDenylistFile, Arrays.asList("192.168.1.0/24"));

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        // Test IPv6 address against IPv4 subnet (should not match)
        ScanTarget ipv6Target = new ScanTarget();
        ipv6Target.setHostname("ipv6site.com");
        ipv6Target.setIp("2001:db8::1");
        assertFalse(provider.isDenylisted(ipv6Target));
    }

    @Test
    public void testConcurrentAccess() throws IOException, InterruptedException {
        Files.write(tempDenylistFile, Arrays.asList("badsite.com", "192.168.1.0/24"));

        DenylistFileProvider provider = new DenylistFileProvider(tempDenylistFile.toString());

        // Test thread safety of isDenylisted
        Thread t1 =
                new Thread(
                        () -> {
                            for (int i = 0; i < 100; i++) {
                                ScanTarget target = new ScanTarget();
                                target.setHostname("test" + i + ".com");
                                target.setIp("192.168.1." + i);
                                provider.isDenylisted(target);
                            }
                        });

        Thread t2 =
                new Thread(
                        () -> {
                            for (int i = 0; i < 100; i++) {
                                ScanTarget target = new ScanTarget();
                                target.setHostname("badsite.com");
                                target.setIp("10.0.0." + i);
                                provider.isDenylisted(target);
                            }
                        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // If we reach here without exceptions, the concurrent access worked fine
        assertTrue(true);
    }
}
