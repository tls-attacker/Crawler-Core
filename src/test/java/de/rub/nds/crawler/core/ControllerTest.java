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
import de.rub.nds.crawler.data.ScanJobDescription;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import de.rub.nds.crawler.dummy.DummyOrchestrationProvider;
import de.rub.nds.crawler.dummy.DummyPersistenceProvider;
import de.rub.nds.scanner.core.probe.ProbeType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
        writer.write("127.0.0.10\n127.0.0.11:8000");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        Assertions.assertTrue(
                orchestrationProvider.waitForJobs(2, 5, TimeUnit.SECONDS),
                "Timed out waiting for jobs to be submitted");
        Assertions.assertEquals(2, orchestrationProvider.jobQueue.size());
        Assertions.assertEquals(0, orchestrationProvider.unackedJobs.size());
    }

    @Test
    void submittingWithExcludedProbes() throws IOException, InterruptedException {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        ControllerCommandConfig config = new DummyControllerCommandConfig();

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("127.0.0.20\n127.0.0.21:443");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        List<ProbeType> excludedProbes =
                Arrays.asList(new TestProbeType("probe1"), new TestProbeType("probe2"));
        config.setExcludedProbes(excludedProbes);

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        Assertions.assertTrue(
                orchestrationProvider.waitForJobs(2, 5, TimeUnit.SECONDS),
                "Timed out waiting for jobs to be submitted");
        Assertions.assertEquals(2, orchestrationProvider.jobQueue.size());
        Assertions.assertEquals(0, orchestrationProvider.unackedJobs.size());

        for (ScanJobDescription job : orchestrationProvider.jobQueue) {
            List<ProbeType> jobExcludedProbes =
                    job.getBulkScanInfo().getScanConfig().getExcludedProbes();
            Assertions.assertNotNull(jobExcludedProbes);
            Assertions.assertEquals(2, jobExcludedProbes.size());
            Assertions.assertEquals("probe1", jobExcludedProbes.get(0).getName());
            Assertions.assertEquals("probe2", jobExcludedProbes.get(1).getName());
        }
    }

    @Test
    void submittingWithoutExcludedProbes() throws IOException, InterruptedException {
        var persistenceProvider = new DummyPersistenceProvider();
        var orchestrationProvider = new DummyOrchestrationProvider();
        ControllerCommandConfig config = new DummyControllerCommandConfig();

        File hostlist = File.createTempFile("hosts", "txt");
        hostlist.deleteOnExit();
        FileWriter writer = new FileWriter(hostlist);
        writer.write("127.0.0.30");
        writer.flush();
        writer.close();

        config.setHostFile(hostlist.getAbsolutePath());

        Controller controller = new Controller(config, orchestrationProvider, persistenceProvider);
        controller.start();

        Assertions.assertTrue(
                orchestrationProvider.waitForJobs(1, 5, TimeUnit.SECONDS),
                "Timed out waiting for jobs to be submitted");
        Assertions.assertEquals(1, orchestrationProvider.jobQueue.size());

        ScanJobDescription job = orchestrationProvider.jobQueue.peek();
        List<ProbeType> jobExcludedProbes =
                job.getBulkScanInfo().getScanConfig().getExcludedProbes();
        Assertions.assertTrue(
                jobExcludedProbes == null || jobExcludedProbes.isEmpty(),
                "Expected excluded probes to be null or empty");
    }
}
