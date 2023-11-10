/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.dummy;

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.core.BulkScanWorker;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.scanner.core.config.ScannerDetail;

public class DummyControllerCommandConfig extends ControllerCommandConfig {

    @Override
    public ScanConfig getScanConfig() {
        return new ScanConfig(ScannerDetail.NORMAL, 1, 1) {
            @Override
            public BulkScanWorker<? extends ScanConfig> createWorker(
                    String bulkScanID, int parallelConnectionThreads, int parallelScanThreads) {
                return null;
            }
        };
    }

    @Override
    public Class<?> getScannerClassForVersion() {
        return this.getClass();
    }
}
