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

/**
 * Main entry point for the TLS-Crawler application.
 *
 * <p>This class provides the command-line interface for running the TLS-Crawler in two modes:
 *
 * <ul>
 *   <li><strong>Controller</strong> - Orchestrates scan jobs and manages the scanning workflow
 *   <li><strong>Worker</strong> - Executes individual scan tasks assigned by the controller
 * </ul>
 *
 * <p>The application uses RabbitMQ for communication between controllers and workers, and MongoDB
 * for persistence of scan results and job status.
 *
 * <p>Usage examples:
 *
 * <pre>
 * java -jar crawler-core.jar controller --config controller.properties
 * java -jar crawler-core.jar worker --config worker.properties
 * </pre>
 *
 * @see Controller
 * @see Worker
 * @see ControllerCommandConfig
 * @see WorkerCommandConfig
 */
public class CommonMain {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Main entry point for the TLS-Crawler application.
     *
     * <p>Parses command line arguments to determine whether to run as a controller or worker,
     * initializes the appropriate configuration and dependencies, and starts the selected mode.
     *
     * @param args command line arguments including the mode ("controller" or "worker") and
     *     configuration parameters
     * @param controllerCommandConfig configuration for controller mode
     * @param workerCommandConfig configuration for worker mode
     */
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
                                new RabbitMqOrchestrationProvider(
                                        workerCommandConfig.getRabbitMqDelegate()),
                                new MongoPersistenceProvider(
                                        workerCommandConfig.getMongoDbDelegate()));
                worker.start();
                break;
            case "controller":
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

    /**
     * Convenience method for running the application with only controller configuration.
     *
     * <p>Creates a default worker configuration and delegates to the main method. This is useful
     * when only controller functionality is needed.
     *
     * @param args command line arguments
     * @param controllerConfig configuration for controller mode
     */
    public static void main(String[] args, ControllerCommandConfig controllerConfig) {
        main(args, controllerConfig, new WorkerCommandConfig());
    }
}
