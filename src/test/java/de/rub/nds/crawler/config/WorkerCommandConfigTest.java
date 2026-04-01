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
import org.junit.jupiter.api.Test;

class WorkerCommandConfigTest {

    @Test
    void parallelProbesDefaultsToOneAndParsesWithThreadOptions() {
        WorkerCommandConfig config = new WorkerCommandConfig();
        assertEquals(1, config.getParallelProbes());

        JCommander.newBuilder()
                .addObject(config)
                .build()
                .parse(
                        "-numberOfThreads",
                        "4",
                        "-parallelProbeThreads",
                        "12",
                        "-parallelProbes",
                        "3");

        assertEquals(4, config.getParallelScanThreads());
        assertEquals(12, config.getParallelConnectionThreads());
        assertEquals(3, config.getParallelProbes());
    }
}
