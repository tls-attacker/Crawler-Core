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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonMain {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(
            String[] args,
            ControllerCommandConfig controllerCommandConfig,
            WorkerCommandConfig workerCommandConfig) {

        JCommander jc = new JCommander();

        jc.addCommand("controller", controllerCommandConfig);
        jc.addCommand("worker", workerCommandConfig);

        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.error("Failed to parse command line arguments", e);
            jc.usage();
            return;
        }
        if (jc.getParsedCommand() == null) {
            LOGGER.error("No command given");
            jc.usage();
            return;
        }

        switch (jc.getParsedCommand().toLowerCase()) {
            case "worker":
                Worker worker =
                        new Worker(
                                workerCommandConfig,
                                workerCommandConfig.createRabbitMqOrchestrationProvider(),
                                workerCommandConfig.createMongoPersistenceProvider());
                worker.start();
                break;
            case "controller":
                controllerCommandConfig.validate();
                Controller controller =
                        new Controller(
                                controllerCommandConfig,
                                controllerCommandConfig.createRabbitMqOrchestrationProvider(),
                                controllerCommandConfig.createMongoPersistenceProvider());
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
