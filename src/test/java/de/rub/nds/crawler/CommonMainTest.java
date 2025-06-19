/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler;

import static org.junit.jupiter.api.Assertions.*;

import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.core.Controller;
import de.rub.nds.crawler.core.Worker;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import de.rub.nds.crawler.orchestration.RabbitMqOrchestrationProvider;
import de.rub.nds.crawler.persistence.MongoPersistenceProvider;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class CommonMainTest {

    @Test
    public void testMainWithNoCommand() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();

        // Test no command given - should return early without starting controller/worker
        CommonMain.main(new String[] {}, controllerConfig, workerConfig);
        // The test passes if no exception is thrown
    }

    @Test
    public void testMainWithInvalidCommand() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();

        // Test invalid command - should return early without starting controller/worker
        CommonMain.main(new String[] {"invalid"}, controllerConfig, workerConfig);
        // The test passes if no exception is thrown
    }

    @Test
    public void testMainWithControllerCommand() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        controllerConfig.setHostFile("src/test/resources/targets.txt");

        WorkerCommandConfig workerConfig = new WorkerCommandConfig();

        try (MockedConstruction<Controller> controllerMock =
                        Mockito.mockConstruction(
                                Controller.class,
                                (mock, context) -> {
                                    Mockito.doNothing().when(mock).start();
                                });
                MockedConstruction<RabbitMqOrchestrationProvider> rabbitMock =
                        Mockito.mockConstruction(RabbitMqOrchestrationProvider.class);
                MockedConstruction<MongoPersistenceProvider> mongoMock =
                        Mockito.mockConstruction(MongoPersistenceProvider.class)) {

            CommonMain.main(new String[] {"controller"}, controllerConfig, workerConfig);

            assertEquals(1, controllerMock.constructed().size());
            Controller controller = controllerMock.constructed().get(0);
            Mockito.verify(controller).start();
        }
    }

    @Test
    public void testMainWithWorkerCommand() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();

        try (MockedConstruction<Worker> workerMock =
                        Mockito.mockConstruction(
                                Worker.class,
                                (mock, context) -> {
                                    Mockito.doNothing().when(mock).start();
                                });
                MockedConstruction<RabbitMqOrchestrationProvider> rabbitMock =
                        Mockito.mockConstruction(RabbitMqOrchestrationProvider.class);
                MockedConstruction<MongoPersistenceProvider> mongoMock =
                        Mockito.mockConstruction(MongoPersistenceProvider.class)) {

            CommonMain.main(new String[] {"worker"}, controllerConfig, workerConfig);

            assertEquals(1, workerMock.constructed().size());
            Worker worker = workerMock.constructed().get(0);
            Mockito.verify(worker).start();
        }
    }

    @Test
    public void testMainWithSingleConfigParam() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();

        // Test the overloaded method
        CommonMain.main(new String[] {}, controllerConfig);
        // The test passes if no exception is thrown
    }

    @Test
    public void testMainDefaultCase() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();

        // Test an unrecognized command - should call usage()
        CommonMain.main(new String[] {"unknown"}, controllerConfig, workerConfig);
        // The test passes if no exception is thrown
    }
}
