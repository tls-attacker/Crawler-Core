/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler;

import com.beust.jcommander.JCommander;
import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.core.Controller;
import de.rub.nds.crawler.core.Worker;
import de.rub.nds.crawler.orchestration.RabbitMqOrchestrationProvider;
import de.rub.nds.crawler.persistence.MongoPersistenceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonMain {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(
            String[] args,
            ControllerCommandConfig controllerCommandConfig,
            WorkerCommandConfig workerCommandConfig) {

        JCommander jc = new JCommander();

        jc.addCommand("controller", controllerCommandConfig); // $NON-NLS-1$
        jc.addCommand("worker", workerCommandConfig); // $NON-NLS-1$

        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.error("Failed to parse command line arguments", e); // $NON-NLS-1$
            jc.usage();
            return;
        }
        if (jc.getParsedCommand() == null) {
            LOGGER.error("No command given"); // $NON-NLS-1$
            jc.usage();
            return;
        }

        switch (jc.getParsedCommand().toLowerCase()) {
            case "worker": //$NON-NLS-1$
                Worker worker =
                        new Worker(
                                workerCommandConfig,
                                new RabbitMqOrchestrationProvider(
                                        workerCommandConfig.getRabbitMqDelegate()),
                                new MongoPersistenceProvider(
                                        workerCommandConfig.getMongoDbDelegate()));
                worker.start();
                break;
            case "controller": //$NON-NLS-1$
                controllerCommandConfig.validate();
                Controller controller =
                        new Controller(
                                controllerCommandConfig,
                                new RabbitMqOrchestrationProvider(
                                        controllerCommandConfig.getRabbitMqDelegate()),
                                new MongoPersistenceProvider(
                                        controllerCommandConfig.getMongoDbDelegate()));
                controller.start();
                break;
            default:
                jc.usage();
        }
    }

    public static void main(String[] args, ControllerCommandConfig controllerConfig) {
        main(args, controllerConfig, new WorkerCommandConfig());
    }
}
