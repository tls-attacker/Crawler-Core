/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2025 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import static org.junit.jupiter.api.Assertions.*;

import com.beust.jcommander.ParameterException;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;
import de.rub.nds.crawler.constant.CruxListNumber;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.targetlist.*;
import de.rub.nds.scanner.core.config.ScannerDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ControllerCommandConfigTest {

    private TestControllerCommandConfig config;

    class TestControllerCommandConfig extends ControllerCommandConfig {
        private ScanConfig testScanConfig = new TestScanConfig();
        private Class<?> testScannerClass = Object.class;

        @Override
        public ScanConfig getScanConfig() {
            return testScanConfig;
        }

        @Override
        public Class<?> getScannerClassForVersion() {
            return testScannerClass;
        }

        public void setTestScanConfig(ScanConfig scanConfig) {
            this.testScanConfig = scanConfig;
        }

        public void setTestScannerClass(Class<?> scannerClass) {
            this.testScannerClass = scannerClass;
        }
    }

    class TestScanConfig extends ScanConfig {
        public TestScanConfig() {
            super(ScannerDetail.NORMAL, 3, 2000);
        }

        @Override
        public de.rub.nds.crawler.core.BulkScanWorker<? extends ScanConfig> createWorker(
                String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
            return null; // For testing purposes only
        }
    }

    @BeforeEach
    void setUp() {
        config = new TestControllerCommandConfig();
    }

    @Test
    void testConstructorInitializesDelegates() {
        assertNotNull(config.getRabbitMqDelegate());
        assertNotNull(config.getMongoDbDelegate());
        assertTrue(config.getRabbitMqDelegate() instanceof RabbitMqDelegate);
        assertTrue(config.getMongoDbDelegate() instanceof MongoDbDelegate);
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

        config.setScanName("TestScan");
        assertEquals("TestScan", config.getScanName());

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
    void testValidateThrowsExceptionWhenNoTargetSpecified() {
        assertThrows(ParameterException.class, () -> config.validate());
    }

    @Test
    void testValidatePassesWithHostFile() {
        config.setHostFile("/path/to/hosts");
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidatePassesWithTranco() {
        config.setTranco(100);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidatePassesWithTrancoEmail() {
        config.setTrancoEmail(100);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidatePassesWithCrux() {
        config.setCrux(CruxListNumber.TOP_1k);
        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidateThrowsExceptionWhenNotifyUrlWithoutMonitoring() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("http://example.com");
        config.setMonitored(false);

        assertThrows(ParameterException.class, () -> config.validate());
    }

    @Test
    void testValidatePassesWithNotifyUrlAndMonitoring() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("http://example.com");
        config.setMonitored(true);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidateWithInvalidNotifyUrl() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("invalid-url");
        config.setMonitored(true);

        assertThrows(ParameterException.class, () -> config.validate());
    }

    @Test
    void testValidateWithEmptyNotifyUrl() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("");
        config.setMonitored(true);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidateWithBlankNotifyUrl() {
        config.setHostFile("/path/to/hosts");
        config.setNotifyUrl("   ");
        config.setMonitored(true);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testGetTargetListProviderWithHostFile() {
        config.setHostFile("/path/to/hosts");

        ITargetListProvider provider = config.getTargetListProvider();
        assertTrue(provider instanceof TargetFileProvider);
    }

    @Test
    void testGetTargetListProviderWithTrancoEmail() {
        config.setTrancoEmail(100);

        ITargetListProvider provider = config.getTargetListProvider();
        assertTrue(provider instanceof TrancoEmailListProvider);
    }

    @Test
    void testGetTargetListProviderWithCrux() {
        config.setCrux(CruxListNumber.TOP_5K);

        ITargetListProvider provider = config.getTargetListProvider();
        assertTrue(provider instanceof CruxListProvider);
    }

    @Test
    void testGetTargetListProviderWithTranco() {
        config.setTranco(1000);

        ITargetListProvider provider = config.getTargetListProvider();
        assertTrue(provider instanceof TrancoListProvider);
    }

    @Test
    void testGetTargetListProviderPriority() {
        config.setHostFile("/path/to/hosts");
        config.setTrancoEmail(100);
        config.setCrux(CruxListNumber.TOP_1k);
        config.setTranco(500);

        ITargetListProvider provider = config.getTargetListProvider();
        assertTrue(provider instanceof TargetFileProvider);
    }

    @Test
    void testCreateBulkScan() {
        config.setScanName("TestScan");
        config.setMonitored(true);
        config.setNotifyUrl("http://example.com");

        long beforeTime = System.currentTimeMillis();
        BulkScan bulkScan = config.createBulkScan();
        long afterTime = System.currentTimeMillis();

        // In test environment, package version might be null
        // The important thing is that the createBulkScan method works correctly
        assertEquals("TestScan", bulkScan.getName());
        assertNotNull(bulkScan.getScanConfig());
        assertTrue(bulkScan.getStartTime() >= beforeTime);
        assertTrue(bulkScan.getStartTime() <= afterTime);
        assertTrue(bulkScan.isMonitored());
        assertEquals("http://example.com", bulkScan.getNotifyUrl());
    }

    @Test
    void testGetCrawlerClassForVersion() {
        assertEquals(TestControllerCommandConfig.class, config.getCrawlerClassForVersion());
    }

    @Test
    void testPositiveIntegerValidator() {
        ControllerCommandConfig.PositiveInteger validator =
                new ControllerCommandConfig.PositiveInteger();

        assertDoesNotThrow(() -> validator.validate("test", "0"));
        assertDoesNotThrow(() -> validator.validate("test", "1"));
        assertDoesNotThrow(() -> validator.validate("test", "100"));
        assertDoesNotThrow(() -> validator.validate("test", String.valueOf(Integer.MAX_VALUE)));

        assertThrows(ParameterException.class, () -> validator.validate("test", "-1"));
        assertThrows(ParameterException.class, () -> validator.validate("test", "-100"));
        assertThrows(NumberFormatException.class, () -> validator.validate("test", "abc"));
        assertThrows(NumberFormatException.class, () -> validator.validate("test", ""));
    }

    @Test
    void testCronSyntaxValidator() {
        ControllerCommandConfig.CronSyntax validator = new ControllerCommandConfig.CronSyntax();

        // Quartz requires 6 or 7 fields, not 5
        assertDoesNotThrow(() -> validator.validate("test", "0 0 12 * * ?"));
        assertDoesNotThrow(() -> validator.validate("test", "0 15 10 * * ?"));
        assertDoesNotThrow(() -> validator.validate("test", "0 0/5 * * * ?"));
        assertDoesNotThrow(() -> validator.validate("test", "0 0 12 ? * MON"));

        assertThrows(Exception.class, () -> validator.validate("test", "invalid"));
        assertThrows(Exception.class, () -> validator.validate("test", ""));
        assertThrows(Exception.class, () -> validator.validate("test", "* * * *"));
        assertThrows(
                Exception.class, () -> validator.validate("test", "0 0 * * *")); // 5 fields invalid
    }

    @Test
    void testAbstractMethods() {
        assertNotNull(config.getScanConfig());
        assertNotNull(config.getScannerClassForVersion());
    }

    @Test
    void testDelegatesAreSameInstancesAfterConstruction() {
        RabbitMqDelegate rabbitMq = config.getRabbitMqDelegate();
        MongoDbDelegate mongoDb = config.getMongoDbDelegate();

        assertSame(rabbitMq, config.getRabbitMqDelegate());
        assertSame(mongoDb, config.getMongoDbDelegate());
    }

    @Test
    void testBoundaryValuesForPort() {
        config.setPort(0);
        assertEquals(0, config.getPort());

        config.setPort(65535);
        assertEquals(65535, config.getPort());

        config.setPort(-1);
        assertEquals(-1, config.getPort());

        config.setPort(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.getPort());
    }

    @Test
    void testBoundaryValuesForTimeoutAndReexecutions() {
        config.setScannerTimeout(0);
        assertEquals(0, config.getScannerTimeout());

        config.setScannerTimeout(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.getScannerTimeout());

        config.setReexecutions(0);
        assertEquals(0, config.getReexecutions());

        config.setReexecutions(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.getReexecutions());
    }

    @Test
    void testNullValues() {
        config.setScanCronInterval(null);
        assertNull(config.getScanCronInterval());

        config.setScanName(null);
        assertNull(config.getScanName());

        config.setHostFile(null);
        assertNull(config.getHostFile());

        config.setDenylistFile(null);
        assertNull(config.getDenylistFile());

        config.setNotifyUrl(null);
        assertNull(config.getNotifyUrl());

        config.setCrux(null);
        assertNull(config.getCrux());
    }

    @Test
    void testEmptyStringValues() {
        config.setScanCronInterval("");
        assertEquals("", config.getScanCronInterval());

        config.setScanName("");
        assertEquals("", config.getScanName());

        config.setHostFile("");
        assertEquals("", config.getHostFile());

        config.setDenylistFile("");
        assertEquals("", config.getDenylistFile());

        config.setNotifyUrl("");
        assertEquals("", config.getNotifyUrl());
    }

    @Test
    void testGetTargetListProviderWithMultipleOptionsSetPriorityOrder() {
        config.setHostFile("/hosts");
        config.setTrancoEmail(100);
        assertTrue(config.getTargetListProvider() instanceof TargetFileProvider);

        config.setHostFile(null);
        assertTrue(config.getTargetListProvider() instanceof TrancoEmailListProvider);

        config.setTrancoEmail(0);
        config.setCrux(CruxListNumber.TOP_1k);
        assertTrue(config.getTargetListProvider() instanceof CruxListProvider);

        config.setCrux(null);
        config.setTranco(500);
        assertTrue(config.getTargetListProvider() instanceof TrancoListProvider);
    }

    @Test
    void testValidateWithMultipleTargetSources() {
        config.setHostFile("/hosts");
        config.setTranco(100);
        config.setTrancoEmail(50);
        config.setCrux(CruxListNumber.TOP_1k);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testCreateBulkScanWithNullValues() {
        config.setScanName(null);
        config.setMonitored(false);
        config.setNotifyUrl(null);

        BulkScan bulkScan = config.createBulkScan();

        assertNull(bulkScan.getName());
        assertFalse(bulkScan.isMonitored());
        assertNull(bulkScan.getNotifyUrl());
    }

    @Test
    void testValidateWithValidHttpsUrl() {
        config.setHostFile("/hosts");
        config.setNotifyUrl("https://secure.example.com/notify");
        config.setMonitored(true);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testPositiveIntegerValidatorErrorMessage() {
        ControllerCommandConfig.PositiveInteger validator =
                new ControllerCommandConfig.PositiveInteger();

        ParameterException exception =
                assertThrows(ParameterException.class, () -> validator.validate("timeout", "-5"));
        assertTrue(exception.getMessage().contains("timeout"));
        assertTrue(exception.getMessage().contains("-5"));
        assertTrue(exception.getMessage().contains("positive"));
    }
}
