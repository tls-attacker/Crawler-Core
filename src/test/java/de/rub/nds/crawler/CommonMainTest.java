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

import com.beust.jcommander.ParameterException;
import de.rub.nds.crawler.config.ControllerCommandConfig;
import de.rub.nds.crawler.config.WorkerCommandConfig;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;
import de.rub.nds.crawler.core.Controller;
import de.rub.nds.crawler.core.Worker;
import de.rub.nds.crawler.dummy.DummyControllerCommandConfig;
import de.rub.nds.crawler.orchestration.RabbitMqOrchestrationProvider;
import de.rub.nds.crawler.persistence.MongoPersistenceProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class CommonMainTest {
    
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    public void setUpStreams() {
        System.setErr(new PrintStream(errContent));
    }
    
    @AfterEach
    public void restoreStreams() {
        System.setErr(originalErr);
    }
    
    @Test
    public void testMainWithNoCommand() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();
        
        CommonMain.main(new String[]{}, controllerConfig, workerConfig);
        
        assertTrue(errContent.toString().contains("No command given"));
    }
    
    @Test
    public void testMainWithInvalidCommand() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();
        
        CommonMain.main(new String[]{"invalid"}, controllerConfig, workerConfig);
        
        assertTrue(errContent.toString().contains("Failed to parse command line arguments"));
    }
    
    @Test
    public void testMainWithControllerCommand() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        controllerConfig.setTargetListFile("src/test/resources/targets.txt");
        controllerConfig.setWorkerScanDefinition("src/test/resources/scanDefinition.json");
        
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();
        
        try (MockedConstruction<Controller> controllerMock = 
                Mockito.mockConstruction(Controller.class, (mock, context) -> {
                    Mockito.doNothing().when(mock).start();
                });
             MockedConstruction<RabbitMqOrchestrationProvider> rabbitMock = 
                Mockito.mockConstruction(RabbitMqOrchestrationProvider.class);
             MockedConstruction<MongoPersistenceProvider> mongoMock = 
                Mockito.mockConstruction(MongoPersistenceProvider.class)) {
            
            CommonMain.main(new String[]{"controller"}, controllerConfig, workerConfig);
            
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
                Mockito.mockConstruction(Worker.class, (mock, context) -> {
                    Mockito.doNothing().when(mock).start();
                });
             MockedConstruction<RabbitMqOrchestrationProvider> rabbitMock = 
                Mockito.mockConstruction(RabbitMqOrchestrationProvider.class);
             MockedConstruction<MongoPersistenceProvider> mongoMock = 
                Mockito.mockConstruction(MongoPersistenceProvider.class)) {
            
            CommonMain.main(new String[]{"worker"}, controllerConfig, workerConfig);
            
            assertEquals(1, workerMock.constructed().size());
            Worker worker = workerMock.constructed().get(0);
            Mockito.verify(worker).start();
        }
    }
    
    @Test
    public void testMainWithSingleConfigParam() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        
        CommonMain.main(new String[]{}, controllerConfig);
        
        assertTrue(errContent.toString().contains("No command given"));
    }
    
    @Test
    public void testMainWithControllerCommandUpperCase() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        controllerConfig.setTargetListFile("src/test/resources/targets.txt");
        controllerConfig.setWorkerScanDefinition("src/test/resources/scanDefinition.json");
        
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();
        
        try (MockedConstruction<Controller> controllerMock = 
                Mockito.mockConstruction(Controller.class, (mock, context) -> {
                    Mockito.doNothing().when(mock).start();
                });
             MockedConstruction<RabbitMqOrchestrationProvider> rabbitMock = 
                Mockito.mockConstruction(RabbitMqOrchestrationProvider.class);
             MockedConstruction<MongoPersistenceProvider> mongoMock = 
                Mockito.mockConstruction(MongoPersistenceProvider.class)) {
            
            CommonMain.main(new String[]{"CONTROLLER"}, controllerConfig, workerConfig);
            
            assertEquals(1, controllerMock.constructed().size());
        }
    }
    
    @Test
    public void testMainWithWorkerCommandUpperCase() {
        ControllerCommandConfig controllerConfig = new DummyControllerCommandConfig();
        WorkerCommandConfig workerConfig = new WorkerCommandConfig();
        
        try (MockedConstruction<Worker> workerMock = 
                Mockito.mockConstruction(Worker.class, (mock, context) -> {
                    Mockito.doNothing().when(mock).start();
                });
             MockedConstruction<RabbitMqOrchestrationProvider> rabbitMock = 
                Mockito.mockConstruction(RabbitMqOrchestrationProvider.class);
             MockedConstruction<MongoPersistenceProvider> mongoMock = 
                Mockito.mockConstruction(MongoPersistenceProvider.class)) {
            
            CommonMain.main(new String[]{"WORKER"}, controllerConfig, workerConfig);
            
            assertEquals(1, workerMock.constructed().size());
        }
    }
}