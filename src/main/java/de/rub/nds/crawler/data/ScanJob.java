/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.data;

import de.rub.nds.crawler.constant.Status;
import de.rub.nds.crawler.orchestration.IOrchestrationProvider;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import de.rub.nds.crawler.scans.Scan;
import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

public class ScanJob implements Serializable {

    private transient Optional<Long> deliveryTag = Optional.empty();

    private ScanTarget scanTarget;

    private Status status;

    // Data also present in bulk scan, we copy it here for the worker
    private String bulkScanId;

    private ScanConfig scanConfig;

    private boolean isMonitored;

    // data to write back results

    private String dbName;

    private String collectionName;

    public ScanJob(
            ScanTarget scanTarget,
            ScanConfig scanConfig,
            String bulkScanId,
            boolean isMonitored,
            String dbName,
            String collectionName,
            Status status) {
        this.scanTarget = scanTarget;
        this.scanConfig = scanConfig;
        this.bulkScanId = bulkScanId;
        this.isMonitored = isMonitored;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.status = status;
    }

    public Scan createRunnable(
            IOrchestrationProvider orchestrationProvider,
            IPersistenceProvider persistenceProvider,
            int parallelProbeThreads) {
        return scanConfig.createRunnable(
                this, orchestrationProvider, persistenceProvider, parallelProbeThreads);
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // handle deserialization, cf. https://stackoverflow.com/a/3960558
        in.defaultReadObject();
        deliveryTag = Optional.empty();
    }

    public ScanTarget getScanTarget() {
        return scanTarget;
    }

    public ScanConfig getScanConfig() {
        return scanConfig;
    }

    public String getBulkScanId() {
        return bulkScanId;
    }

    public boolean isMonitored() {
        return isMonitored;
    }

    public String getDbName() {
        return dbName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getDeliveryTag() {
        return deliveryTag.get();
    }

    public void setDeliveryTag(Long deliveryTag) {
        if (this.deliveryTag.isPresent()) {
            throw new IllegalStateException("Delivery tag already set");
        }
        this.deliveryTag = Optional.of(deliveryTag);
    }
}
