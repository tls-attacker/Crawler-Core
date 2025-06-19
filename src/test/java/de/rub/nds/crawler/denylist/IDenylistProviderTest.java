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
import org.junit.jupiter.api.Test;

public class IDenylistProviderTest {

    @Test
    public void testInterfaceImplementation() {
        IDenylistProvider provider =
                new IDenylistProvider() {
                    @Override
                    public boolean isDenylisted(ScanTarget target) {
                        return "denied.example.com".equals(target.getHostname());
                    }
                };

        ScanTarget deniedTarget = new ScanTarget();
        deniedTarget.setHostname("denied.example.com");
        deniedTarget.setIp("192.168.1.1");

        ScanTarget allowedTarget = new ScanTarget();
        allowedTarget.setHostname("allowed.example.com");
        allowedTarget.setIp("192.168.1.2");

        assertTrue(provider.isDenylisted(deniedTarget));
        assertFalse(provider.isDenylisted(allowedTarget));
    }

    @Test
    public void testAnonymousClassImplementation() {
        IDenylistProvider alwaysDenyProvider =
                new IDenylistProvider() {
                    @Override
                    public boolean isDenylisted(ScanTarget target) {
                        return true;
                    }
                };

        IDenylistProvider neverDenyProvider =
                new IDenylistProvider() {
                    @Override
                    public boolean isDenylisted(ScanTarget target) {
                        return false;
                    }
                };

        ScanTarget target = new ScanTarget();
        target.setHostname("example.com");
        target.setIp("10.0.0.1");

        assertTrue(alwaysDenyProvider.isDenylisted(target));
        assertFalse(neverDenyProvider.isDenylisted(target));
    }

    @Test
    public void testNullHandling() {
        IDenylistProvider provider =
                new IDenylistProvider() {
                    @Override
                    public boolean isDenylisted(ScanTarget target) {
                        return target != null
                                && target.getHostname() != null
                                && target.getHostname().contains("blocked");
                    }
                };

        ScanTarget targetWithNullHostname = new ScanTarget();
        targetWithNullHostname.setIp("192.168.1.1");

        ScanTarget targetWithBlockedHostname = new ScanTarget();
        targetWithBlockedHostname.setHostname("blocked.example.com");
        targetWithBlockedHostname.setIp("192.168.1.2");

        assertFalse(provider.isDenylisted(targetWithNullHostname));
        assertTrue(provider.isDenylisted(targetWithBlockedHostname));
    }
}
