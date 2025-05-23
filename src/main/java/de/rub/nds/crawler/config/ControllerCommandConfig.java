/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;
import de.rub.nds.crawler.constant.CruxListNumber;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.crawler.targetlist.*;
import de.rub.nds.scanner.core.config.ScannerDetail;
import org.apache.commons.validator.routines.UrlValidator;
import org.quartz.CronScheduleBuilder;

public abstract class ControllerCommandConfig {

    @ParametersDelegate private final RabbitMqDelegate rabbitMqDelegate;

    @ParametersDelegate private final MongoDbDelegate mongoDbDelegate;

    @Parameter(names = "-portToBeScanned", description = "The port that should be scanned.")
    private int port = 443;

    @Parameter(names = "-scanDetail")
    private ScannerDetail scanDetail = ScannerDetail.NORMAL;

    @Parameter(
            names = "-timeout",
            validateWith = PositiveInteger.class,
            description = "The timeout to use inside the TLS-Scanner.")
    private int scannerTimeout = 2000;

    @Parameter(
            names = "-reexecutions",
            validateWith = PositiveInteger.class,
            description = "Number of reexecutions to use in the TLS-Scanner.")
    private int reexecutions = 3;

    @Parameter(
            names = "-scanCronInterval",
            validateWith = CronSyntax.class,
            description =
                    "A cron expression which defines the interval of when scans are started. Leave empty to only start one scan immediately.")
    private String scanCronInterval;

    @Parameter(names = "-scanName", description = "The name of the scan")
    private String scanName;

    @Parameter(
            names = "-hostFile",
            description = "A file of a list of servers which should be scanned.")
    private String hostFile;

    @Parameter(
            names = "-denylist",
            description = "A file with a list of IP-Ranges or domains which should not be scanned.")
    private String denylistFile;

    @Parameter(
            names = "-monitorScan",
            description = "If set the progress of the scans is monitored and logged.")
    private boolean monitored;

    @Parameter(
            names = "-notifyUrl",
            description =
                    "If set the controller sends a HTTP Post request including the BulkScan object in JSON after a BulkScan is finished to the specified URL.")
    private String notifyUrl;

    @Parameter(
            names = "-tranco",
            description = "Number of top x hosts of the tranco list that should be scanned")
    private int tranco;

    @Parameter(
            names = "-crux",
            description = "Number of top x hosts of the crux list that should be scanned")
    private CruxListNumber crux;

    @Parameter(names = "-trancoEmail", description = "MX record for number of top x hosts")
    private int trancoEmail;

    public ControllerCommandConfig() {
        rabbitMqDelegate = new RabbitMqDelegate();
        mongoDbDelegate = new MongoDbDelegate();
    }

    public void validate() {
        if (hostFile == null && tranco == 0 && trancoEmail == 0 && crux == null) {
            throw new ParameterException(
                    "You have to either pass a hostFile, specify a number of tranco hosts or specify a number of crux hosts");
        }
        if (notifyUrl != null && !notifyUrl.isEmpty() && !notifyUrl.isBlank() && !monitored) {
            throw new ParameterException(
                    "If a notify message should be sent the scan has to be monitored (-monitorScan)");
        }
        if (notifyUrl != null
                && !notifyUrl.isEmpty()
                && !notifyUrl.isBlank()
                && !new UrlValidator().isValid(notifyUrl)) {
            throw new ParameterException("Provided notify URI is not a valid URI");
        }
    }

    public static class PositiveInteger implements IParameterValidator {
        public void validate(String name, String value) throws ParameterException {
            int n = Integer.parseInt(value);
            if (n < 0) {
                throw new ParameterException(
                        "Parameter " + name + " should be positive (found " + value + ")");
            }
        }
    }

    public static class CronSyntax implements IParameterValidator {
        public void validate(String name, String value) throws ParameterException {
            CronScheduleBuilder.cronSchedule(value);
        }
    }

    public RabbitMqDelegate getRabbitMqDelegate() {
        return rabbitMqDelegate;
    }

    public MongoDbDelegate getMongoDbDelegate() {
        return mongoDbDelegate;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ScannerDetail getScanDetail() {
        return scanDetail;
    }

    public int getScannerTimeout() {
        return scannerTimeout;
    }

    public int getReexecutions() {
        return reexecutions;
    }

    public String getScanCronInterval() {
        return scanCronInterval;
    }

    public String getScanName() {
        return scanName;
    }

    public String getHostFile() {
        return hostFile;
    }

    public void setHostFile(String hostFile) {
        this.hostFile = hostFile;
    }

    public String getDenylistFile() {
        return denylistFile;
    }

    public boolean isMonitored() {
        return monitored;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public int getTranco() {
        return tranco;
    }

    public CruxListNumber getCrux() {
        return crux;
    }

    public int getTrancoEmail() {
        return trancoEmail;
    }

    public ITargetListProvider getTargetListProvider() {
        if (getHostFile() != null) {
            return new TargetFileProvider(getHostFile());
        }
        if (getTrancoEmail() != 0) {
            return new TrancoEmailListProvider(new TrancoListProvider(getTrancoEmail()));
        }
        if (getCrux() != null) {
            return new CruxListProvider(getCrux());
        }
        return new TrancoListProvider(getTranco());
    }

    public abstract ScanConfig getScanConfig();

    public BulkScan createBulkScan() {
        return new BulkScan(
                getScannerClassForVersion(),
                getCrawlerClassForVersion(),
                getScanName(),
                getScanConfig(),
                System.currentTimeMillis(),
                isMonitored(),
                getNotifyUrl());
    }

    public Class<?> getCrawlerClassForVersion() {
        return this.getClass();
    }

    public abstract Class<?> getScannerClassForVersion();

    public void setScanDetail(ScannerDetail scanDetail) {
        this.scanDetail = scanDetail;
    }

    public void setScannerTimeout(int scannerTimeout) {
        this.scannerTimeout = scannerTimeout;
    }

    public void setReexecutions(int reexecutions) {
        this.reexecutions = reexecutions;
    }

    public void setScanCronInterval(String scanCronInterval) {
        this.scanCronInterval = scanCronInterval;
    }

    public void setScanName(String scanName) {
        this.scanName = scanName;
    }

    public void setDenylistFile(String denylistFile) {
        this.denylistFile = denylistFile;
    }

    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public void setTranco(int tranco) {
        this.tranco = tranco;
    }

    public void setCrux(CruxListNumber crux) {
        this.crux = crux;
    }

    public void setTrancoEmail(int trancoEmail) {
        this.trancoEmail = trancoEmail;
    }
}
