/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beust.jcommander.JCommander;
import de.rub.nds.crawler.data.BulkScan;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import org.junit.jupiter.api.Test;

class ControllerCommandConfigTest {

    @Test
    void parallelProbesDefaultsToOneAndParses() {
        ControllerCommandConfig config = new DummyControllerCommandConfig();
        assertEquals(1, config.getParallelProbes());

        JCommander.newBuilder().addObject(config).build().parse("-parallelProbes", "7");
        assertEquals(7, config.getParallelProbes());
    }

    @Test
    void createBulkScanPropagatesParallelProbesToScanConfig() {
        DummyControllerCommandConfig config = new DummyControllerCommandConfig();
        config.setParallelProbes(4);

        BulkScan bulkScan = config.createBulkScan();
        assertEquals(4, bulkScan.getScanConfig().getParallelProbes());
    }
}
