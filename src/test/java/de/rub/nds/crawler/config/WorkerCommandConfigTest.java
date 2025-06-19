/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import static org.junit.jupiter.api.Assertions.*;

import com.beust.jcommander.JCommander;
import de.rub.nds.crawler.config.delegate.MongoDbDelegate;
import de.rub.nds.crawler.config.delegate.RabbitMqDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkerCommandConfigTest {

    private WorkerCommandConfig config;

    @BeforeEach
    void setUp() {
        config = new WorkerCommandConfig();
    }

    @Test
    void testDefaultValues() {
        assertEquals(Runtime.getRuntime().availableProcessors(), config.getParallelScanThreads());
        assertEquals(20, config.getParallelConnectionThreads());
        assertEquals(840000, config.getScanTimeout());
        assertNotNull(config.getRabbitMqDelegate());
        assertNotNull(config.getMongoDbDelegate());
    }

    @Test
    void testSettersAndGetters() {
        config.setParallelScanThreads(10);
        assertEquals(10, config.getParallelScanThreads());

        config.setParallelConnectionThreads(30);
        assertEquals(30, config.getParallelConnectionThreads());

        config.setScanTimeout(1000000);
        assertEquals(1000000, config.getScanTimeout());
    }

    @Test
    void testJCommanderParsing() {
        String[] args = {
            "-numberOfThreads", "8",
            "-parallelProbeThreads", "25",
            "-scanTimeout", "900000"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(config).build();
        jCommander.parse(args);

        assertEquals(8, config.getParallelScanThreads());
        assertEquals(25, config.getParallelConnectionThreads());
        assertEquals(900000, config.getScanTimeout());
    }

    @Test
    void testJCommanderParsingWithDelegates() {
        String[] args = {
            "-numberOfThreads", "4",
            "-rabbitMqHost", "rabbitmq.example.com",
            "-rabbitMqPort", "5673",
            "-mongoDbHost", "mongo.example.com",
            "-mongoDbPort", "27018"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(config).build();
        jCommander.parse(args);

        assertEquals(4, config.getParallelScanThreads());
        assertEquals("rabbitmq.example.com", config.getRabbitMqDelegate().getRabbitMqHost());
        assertEquals(5673, config.getRabbitMqDelegate().getRabbitMqPort());
        assertEquals("mongo.example.com", config.getMongoDbDelegate().getMongoDbHost());
        assertEquals(27018, config.getMongoDbDelegate().getMongoDbPort());
    }

    @Test
    void testDelegatesNotNull() {
        RabbitMqDelegate rabbitMqDelegate = config.getRabbitMqDelegate();
        MongoDbDelegate mongoDbDelegate = config.getMongoDbDelegate();

        assertNotNull(rabbitMqDelegate);
        assertNotNull(mongoDbDelegate);
        assertSame(rabbitMqDelegate, config.getRabbitMqDelegate());
        assertSame(mongoDbDelegate, config.getMongoDbDelegate());
    }
}
