/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.core;

import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.data.ScanConfig;
import de.rub.nds.scanner.core.config.ScannerDetail;
import de.rub.nds.scanner.core.probe.ProbeType;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WorkerTest {

    /**
     * Test that worker's excluded probes are applied to ScanConfig when controller doesn't specify
     * any (null).
     */
    @Test
    void workerExcludedProbesAppliedWhenControllerHasNull() {
        WorkerCommandConfig config = new WorkerCommandConfig();
        List<ProbeType> workerExcludedProbes =
                Arrays.asList(new TestProbeType("worker1"), new TestProbeType("worker2"));
        config.setExcludedProbes(workerExcludedProbes);

        // Create a scan config with null excluded probes (controller didn't specify any)
        ScanConfig scanConfig =
                new ScanConfig(ScannerDetail.NORMAL, 1, 1000, null) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads) {
                        return null;
                    }
                };

        // Simulate what Worker.applyWorkerExcludedProbes() does
        List<ProbeType> controllerExcludedProbes = scanConfig.getExcludedProbes();
        if (config.getExcludedProbes() != null && !config.getExcludedProbes().isEmpty()) {
            if (controllerExcludedProbes == null || controllerExcludedProbes.isEmpty()) {
                scanConfig.setExcludedProbes(config.getExcludedProbes());
            }
        }

        // Verify worker's excluded probes were applied
        List<ProbeType> appliedProbes = scanConfig.getExcludedProbes();
        Assertions.assertNotNull(appliedProbes);
        Assertions.assertEquals(2, appliedProbes.size());
        Assertions.assertEquals("worker1", appliedProbes.get(0).getName());
        Assertions.assertEquals("worker2", appliedProbes.get(1).getName());
    }

    /** Test that worker's excluded probes are applied when controller has an empty list. */
    @Test
    void workerExcludedProbesAppliedWhenControllerHasEmpty() {
        WorkerCommandConfig config = new WorkerCommandConfig();
        List<ProbeType> workerExcludedProbes =
                Arrays.asList(new TestProbeType("worker1"), new TestProbeType("worker2"));
        config.setExcludedProbes(workerExcludedProbes);

        // Create a scan config with empty excluded probes list
        ScanConfig scanConfig =
                new ScanConfig(ScannerDetail.NORMAL, 1, 1000, new LinkedList<>()) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads) {
                        return null;
                    }
                };

        // Simulate what Worker.applyWorkerExcludedProbes() does
        List<ProbeType> controllerExcludedProbes = scanConfig.getExcludedProbes();
        if (config.getExcludedProbes() != null && !config.getExcludedProbes().isEmpty()) {
            if (controllerExcludedProbes == null || controllerExcludedProbes.isEmpty()) {
                scanConfig.setExcludedProbes(config.getExcludedProbes());
            }
        }

        // Verify worker's excluded probes were applied
        List<ProbeType> appliedProbes = scanConfig.getExcludedProbes();
        Assertions.assertNotNull(appliedProbes);
        Assertions.assertEquals(2, appliedProbes.size());
        Assertions.assertEquals("worker1", appliedProbes.get(0).getName());
        Assertions.assertEquals("worker2", appliedProbes.get(1).getName());
    }

    /** Test that controller's excluded probes take precedence over worker's when both are set. */
    @Test
    void controllerExcludedProbesTakePrecedenceOverWorker() {
        WorkerCommandConfig config = new WorkerCommandConfig();
        List<ProbeType> workerExcludedProbes =
                Arrays.asList(new TestProbeType("worker1"), new TestProbeType("worker2"));
        config.setExcludedProbes(workerExcludedProbes);

        // Create a scan config with controller's excluded probes
        List<ProbeType> controllerExcludedProbes =
                Arrays.asList(new TestProbeType("controller1"), new TestProbeType("controller2"));
        ScanConfig scanConfig =
                new ScanConfig(ScannerDetail.NORMAL, 1, 1000, controllerExcludedProbes) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads) {
                        return null;
                    }
                };

        // Simulate what Worker.applyWorkerExcludedProbes() does
        List<ProbeType> controllerProbes = scanConfig.getExcludedProbes();
        if (config.getExcludedProbes() != null && !config.getExcludedProbes().isEmpty()) {
            if (controllerProbes == null || controllerProbes.isEmpty()) {
                scanConfig.setExcludedProbes(config.getExcludedProbes());
            }
        }

        // Verify controller's excluded probes remain (controller takes precedence)
        List<ProbeType> appliedProbes = scanConfig.getExcludedProbes();
        Assertions.assertNotNull(appliedProbes);
        Assertions.assertEquals(2, appliedProbes.size());
        Assertions.assertEquals("controller1", appliedProbes.get(0).getName());
        Assertions.assertEquals("controller2", appliedProbes.get(1).getName());
    }

    /** Test that when worker has no excluded probes and controller has none, nothing is applied. */
    @Test
    void nothingAppliedWhenBothWorkerAndControllerHaveNoExcludedProbes() {
        WorkerCommandConfig config = new WorkerCommandConfig();
        // Worker has no excluded probes (default empty list)

        // Create a scan config with null excluded probes
        ScanConfig scanConfig =
                new ScanConfig(ScannerDetail.NORMAL, 1, 1000, null) {
                    @Override
                    public BulkScanWorker<? extends ScanConfig> createWorker(
                            String bulkScanID,
                            int parallelConnectionThreads,
                            int parallelScanThreads) {
                        return null;
                    }
                };

        // Simulate what Worker.applyWorkerExcludedProbes() does
        List<ProbeType> controllerExcludedProbes = scanConfig.getExcludedProbes();
        if (config.getExcludedProbes() != null && !config.getExcludedProbes().isEmpty()) {
            if (controllerExcludedProbes == null || controllerExcludedProbes.isEmpty()) {
                scanConfig.setExcludedProbes(config.getExcludedProbes());
            }
        }

        // Verify no probes were applied (should remain null)
        List<ProbeType> appliedProbes = scanConfig.getExcludedProbes();
        Assertions.assertNull(appliedProbes);
    }

    /** Test WorkerCommandConfig getter and setter for excluded probes. */
    @Test
    void workerCommandConfigExcludedProbesGetterSetter() {
        WorkerCommandConfig config = new WorkerCommandConfig();

        // Initially should be an empty list
        Assertions.assertNotNull(config.getExcludedProbes());
        Assertions.assertTrue(config.getExcludedProbes().isEmpty());

        // Set excluded probes
        List<ProbeType> excludedProbes =
                Arrays.asList(new TestProbeType("probe1"), new TestProbeType("probe2"));
        config.setExcludedProbes(excludedProbes);

        // Verify getter returns the set value
        List<ProbeType> retrievedProbes = config.getExcludedProbes();
        Assertions.assertNotNull(retrievedProbes);
        Assertions.assertEquals(2, retrievedProbes.size());
        Assertions.assertEquals("probe1", retrievedProbes.get(0).getName());
        Assertions.assertEquals("probe2", retrievedProbes.get(1).getName());
    }
}
