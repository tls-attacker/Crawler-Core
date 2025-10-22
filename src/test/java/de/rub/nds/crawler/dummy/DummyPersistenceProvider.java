/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.dummy;

import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.data.ScanResult;
import de.rub.nds.crawler.persistence.IPersistenceProvider;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DummyPersistenceProvider implements IPersistenceProvider {
    public final List<ScanResult> results = new ArrayList<>();
    public final List<BulkScan> bulkScans = new ArrayList<>();

    @Override
    public void insertScanResult(ScanResult scanResult, ScanJobDescription job) {
        results.add(scanResult);
    }

    @Override
    public void insertBulkScan(BulkScan bulkScan) {
        bulkScans.add(bulkScan);
    }

    @Override
    public void updateBulkScan(BulkScan bulkScan) {}

    @Override
    public List<ScanResult> getScanResultsByTarget(
            String dbName, String collectionName, String target) {
        return new LinkedList<>();
    }

    @Override
    public ScanResult getScanResultById(String dbName, String collectionName, String id) {
        return results.stream()
                .filter(result -> result.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
