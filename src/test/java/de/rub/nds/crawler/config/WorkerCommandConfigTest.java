/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2025 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config;

import static org.junit.jupiter.api.Assertions.*;

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
    void testConstructorInitializesDelegates() {
        assertNotNull(config.getRabbitMqDelegate());
        assertNotNull(config.getMongoDbDelegate());
        assertTrue(config.getRabbitMqDelegate() instanceof RabbitMqDelegate);
        assertTrue(config.getMongoDbDelegate() instanceof MongoDbDelegate);
    }

    @Test
    void testDefaultValues() {
        assertEquals(Runtime.getRuntime().availableProcessors(), config.getParallelScanThreads());
        assertEquals(20, config.getParallelConnectionThreads());
        assertEquals(840000, config.getScanTimeout());
    }

    @Test
    void testDefaultValuesMatchesRuntimeProcessors() {
        WorkerCommandConfig newConfig = new WorkerCommandConfig();
        assertEquals(
                Runtime.getRuntime().availableProcessors(), newConfig.getParallelScanThreads());
    }

    @Test
    void testSetAndGetParallelScanThreads() {
        config.setParallelScanThreads(16);
        assertEquals(16, config.getParallelScanThreads());

        config.setParallelScanThreads(1);
        assertEquals(1, config.getParallelScanThreads());

        config.setParallelScanThreads(0);
        assertEquals(0, config.getParallelScanThreads());

        config.setParallelScanThreads(-1);
        assertEquals(-1, config.getParallelScanThreads());

        config.setParallelScanThreads(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.getParallelScanThreads());
    }

    @Test
    void testSetAndGetParallelConnectionThreads() {
        config.setParallelConnectionThreads(50);
        assertEquals(50, config.getParallelConnectionThreads());

        config.setParallelConnectionThreads(1);
        assertEquals(1, config.getParallelConnectionThreads());

        config.setParallelConnectionThreads(0);
        assertEquals(0, config.getParallelConnectionThreads());

        config.setParallelConnectionThreads(-1);
        assertEquals(-1, config.getParallelConnectionThreads());

        config.setParallelConnectionThreads(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.getParallelConnectionThreads());
    }

    @Test
    void testSetAndGetScanTimeout() {
        config.setScanTimeout(900000);
        assertEquals(900000, config.getScanTimeout());

        config.setScanTimeout(1);
        assertEquals(1, config.getScanTimeout());

        config.setScanTimeout(0);
        assertEquals(0, config.getScanTimeout());

        config.setScanTimeout(-1);
        assertEquals(-1, config.getScanTimeout());

        config.setScanTimeout(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, config.getScanTimeout());
    }

    @Test
    void testDelegatesAreNotNull() {
        assertNotNull(config.getRabbitMqDelegate());
        assertNotNull(config.getMongoDbDelegate());
    }

    @Test
    void testDelegatesAreSameInstancesAfterConstruction() {
        RabbitMqDelegate rabbitMq = config.getRabbitMqDelegate();
        MongoDbDelegate mongoDb = config.getMongoDbDelegate();

        assertSame(rabbitMq, config.getRabbitMqDelegate());
        assertSame(mongoDb, config.getMongoDbDelegate());
    }

    @Test
    void testMultipleInstances() {
        WorkerCommandConfig config1 = new WorkerCommandConfig();
        WorkerCommandConfig config2 = new WorkerCommandConfig();

        assertNotSame(config1.getRabbitMqDelegate(), config2.getRabbitMqDelegate());
        assertNotSame(config1.getMongoDbDelegate(), config2.getMongoDbDelegate());
    }

    @Test
    void testOverwriteValues() {
        config.setParallelScanThreads(10);
        config.setParallelScanThreads(20);
        assertEquals(20, config.getParallelScanThreads());

        config.setParallelConnectionThreads(30);
        config.setParallelConnectionThreads(40);
        assertEquals(40, config.getParallelConnectionThreads());

        config.setScanTimeout(100000);
        config.setScanTimeout(200000);
        assertEquals(200000, config.getScanTimeout());
    }

    @Test
    void testDelegateModification() {
        config.getRabbitMqDelegate().setRabbitMqHost("localhost");
        assertEquals("localhost", config.getRabbitMqDelegate().getRabbitMqHost());

        config.getMongoDbDelegate().setMongoDbHost("mongodb.local");
        assertEquals("mongodb.local", config.getMongoDbDelegate().getMongoDbHost());
    }

    @Test
    void testBoundaryValuesForAllParameters() {
        config.setParallelScanThreads(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, config.getParallelScanThreads());

        config.setParallelConnectionThreads(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, config.getParallelConnectionThreads());

        config.setScanTimeout(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, config.getScanTimeout());
    }
}
