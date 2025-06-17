/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import de.rub.nds.crawler.dummy.DummyOrchestrationProvider;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ControllerTest {

    @Test
    void submitting() throws IOException, InterruptedException {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        ControllerCommandConfig config = new DummyControllerCommandConfig();

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("example.com\nexample.org:8000");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        Thread.sleep(1000);

        // With multi-IP hostname support, we expect at least 2 jobs (one per hostname)
        // but may get more if hostnames resolve to multiple IPs
        Assertions.assertTrue(
                orchestrationProvider.jobQueue.size() >= 2,
                "Expected at least 2 jobs but got " + orchestrationProvider.jobQueue.size());
        Assertions.assertEquals(0, orchestrationProvider.unackedJobs.size());
    }
}
