/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import static org.junit.jupiter.api.Assertions.*;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import de.rub.nds.crawler.constant.CruxListNumber;
import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.targetlist.*;
import de.rub.nds.scanner.core.config.ScannerDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ControllerCommandConfigTest {

    private TestControllerCommandConfig config;

    private static class TestControllerCommandConfig extends ControllerCommandConfig {
        @Override
        public ScanConfig getScanConfig() {
            return new ScanConfig(ScannerDetail.NORMAL, 3, 2000) {
                @Override
                public BulkScanWorker<? extends ScanConfig> createWorker(
                        String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
                    return null;
                }
            };
        }

        @Override
        public Class<?> getScannerClassForVersion() {
            return String.class; // Dummy class for testing
        }
    }

    @BeforeEach
    void setUp() {
        config = new TestControllerCommandConfig();
    }

    @Test
    void testDefaultValues() {
        assertEquals(443, config.getPort());
        assertEquals(ScannerDetail.NORMAL, config.getScanDetail());
        assertEquals(2000, config.getScannerTimeout());
        assertEquals(3, config.getReexecutions());
        assertNull(config.getScanCronInterval());
        assertNull(config.getScanName());
        assertNull(config.getHostFile());
        assertNull(config.getDenylistFile());
        assertFalse(config.isMonitored());
        assertNull(config.getNotifyUrl());
        assertEquals(0, config.getTranco());
        assertNull(config.getCrux());
        assertEquals(0, config.getTrancoEmail());
    }

    @Test
    void testSettersAndGetters() {
        config.setPort(8443);
        assertEquals(8443, config.getPort());

        config.setScanDetail(ScannerDetail.DETAILED);
        assertEquals(ScannerDetail.DETAILED, config.getScanDetail());

        config.setScannerTimeout(5000);
        assertEquals(5000, config.getScannerTimeout());

        config.setReexecutions(5);
        assertEquals(5, config.getReexecutions());

        config.setScanCronInterval("0 0 * * *");
        assertEquals("0 0 * * *", config.getScanCronInterval());

        config.setScanName("test-scan");
        assertEquals("test-scan", config.getScanName());

        config.setHostFile("/path/to/hosts");
        assertEquals("/path/to/hosts", config.getHostFile());

        config.setDenylistFile("/path/to/denylist");
        assertEquals("/path/to/denylist", config.getDenylistFile());

        config.setMonitored(true);
        assertTrue(config.isMonitored());

        config.setNotifyUrl("http://example.com/notify");
        assertEquals("http://example.com/notify", config.getNotifyUrl());

        config.setTranco(1000);
        assertEquals(1000, config.getTranco());

        config.setCrux(CruxListNumber.TOP_10K);
        assertEquals(CruxListNumber.TOP_10K, config.getCrux());

        config.setTrancoEmail(500);
        assertEquals(500, config.getTrancoEmail());
    }

    @Test
    void testValidateNoTargetListProvider() {
        // No host file, tranco, crux, or trancoEmail set
        assertThrows(ParameterException.class, () -> config.validate());
    }

    @Test
    void testValidateNotifyUrlWithoutMonitoring() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("http://example.com/notify");
        config.setMonitored(false);

        assertThrows(ParameterException.class, () -> config.validate());
    }

    @Test
    void testValidateInvalidNotifyUrl() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("not-a-valid-url");
        config.setMonitored(true);

        assertThrows(ParameterException.class, () -> config.validate());
    }

    @Test
    void testValidateSuccessful() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("http://example.com/notify");
        config.setMonitored(true);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidateWithTranco() {
        config.setTranco(1000);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidateWithCrux() {
        config.setCrux(CruxListNumber.TOP_5K);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidateWithTrancoEmail() {
        config.setTrancoEmail(500);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testGetTargetListProviderHostFile() {
        config.setHostFile("/path/to/hosts");
        ITargetListProvider provider = config.getTargetListProvider();
        assertInstanceOf(TargetFileProvider.class, provider);
    }

    @Test
    void testGetTargetListProviderTrancoEmail() {
        config.setTrancoEmail(500);
        ITargetListProvider provider = config.getTargetListProvider();
        assertInstanceOf(TrancoEmailListProvider.class, provider);
    }

    @Test
    void testGetTargetListProviderCrux() {
        config.setCrux(CruxListNumber.TOP_10K);
        ITargetListProvider provider = config.getTargetListProvider();
        assertInstanceOf(CruxListProvider.class, provider);
    }

    @Test
    void testGetTargetListProviderTranco() {
        config.setTranco(1000);
        ITargetListProvider provider = config.getTargetListProvider();
        assertInstanceOf(TrancoListProvider.class, provider);
    }

    @Test
    void testCreateBulkScan() {
        config.setScanName("test-scan");
        config.setMonitored(true);
        config.setNotifyUrl("http://example.com/notify");

        BulkScan bulkScan = config.createBulkScan();

        assertEquals("test-scan", bulkScan.getName());
        assertTrue(bulkScan.isMonitored());
        assertEquals("http://example.com/notify", bulkScan.getNotifyUrl());
        assertNotNull(bulkScan.getScannerVersion());
        assertNotNull(bulkScan.getCrawlerVersion());
        assertNotNull(bulkScan.getScanConfig());
        assertTrue(bulkScan.getStartTime() > 0);
    }

    @Test
    void testPositiveIntegerValidator() {
        ControllerCommandConfig.PositiveInteger validator =
                new ControllerCommandConfig.PositiveInteger();

        assertDoesNotThrow(() -> validator.validate("test", "0"));
        assertDoesNotThrow(() -> validator.validate("test", "100"));
        assertThrows(ParameterException.class, () -> validator.validate("test", "-1"));
    }

    @Test
    void testCronSyntaxValidator() {
        ControllerCommandConfig.CronSyntax validator = new ControllerCommandConfig.CronSyntax();

        assertDoesNotThrow(() -> validator.validate("test", "0 0 * * *"));
        assertDoesNotThrow(() -> validator.validate("test", "0 */5 * * *"));
        assertThrows(Exception.class, () -> validator.validate("test", "invalid cron"));
    }

    @Test
    void testJCommanderParsing() {
        String[] args = {
            "-portToBeScanned",
            "8443",
            "-scanDetail",
            "DETAILED",
            "-timeout",
            "5000",
            "-reexecutions",
            "5",
            "-scanCronInterval",
            "0 0 * * *",
            "-scanName",
            "my-scan",
            "-hostFile",
            "/path/to/hosts",
            "-denylist",
            "/path/to/denylist",
            "-monitorScan",
            "-notifyUrl",
            "http://example.com/notify",
            "-tranco",
            "1000"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(config).build();
        jCommander.parse(args);

        assertEquals(8443, config.getPort());
        assertEquals(ScannerDetail.DETAILED, config.getScanDetail());
        assertEquals(5000, config.getScannerTimeout());
        assertEquals(5, config.getReexecutions());
        assertEquals("0 0 * * *", config.getScanCronInterval());
        assertEquals("my-scan", config.getScanName());
        assertEquals("/path/to/hosts", config.getHostFile());
        assertEquals("/path/to/denylist", config.getDenylistFile());
        assertTrue(config.isMonitored());
        assertEquals("http://example.com/notify", config.getNotifyUrl());
        assertEquals(1000, config.getTranco());
    }

    @Test
    void testJCommanderParsingNegativeTimeout() {
        String[] args = {
            "-hostFile", "/path/to/hosts",
            "-timeout", "-1000"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(config).build();

        assertThrows(ParameterException.class, () -> jCommander.parse(args));
    }

    @Test
    void testJCommanderParsingInvalidCron() {
        String[] args = {
            "-hostFile", "/path/to/hosts",
            "-scanCronInterval", "invalid cron expression"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(config).build();

        assertThrows(Exception.class, () -> jCommander.parse(args));
    }

    @Test
    void testGetCrawlerClassForVersion() {
        assertEquals(TestControllerCommandConfig.class, config.getCrawlerClassForVersion());
    }

    @Test
    void testGetScannerClassForVersion() {
        assertEquals(String.class, config.getScannerClassForVersion());
    }
}
