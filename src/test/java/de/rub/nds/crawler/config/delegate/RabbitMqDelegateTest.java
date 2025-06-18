/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config.delegate;

import static org.junit.jupiter.api.Assertions.*;

import com.beust.jcommander.JCommander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RabbitMqDelegateTest {

    private RabbitMqDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new RabbitMqDelegate();
    }

    @Test
    void testDefaultValues() {
        assertNull(delegate.getRabbitMqHost());
        assertEquals(0, delegate.getRabbitMqPort());
        assertNull(delegate.getRabbitMqUser());
        assertNull(delegate.getRabbitMqPass());
        assertNull(delegate.getRabbitMqPassFile());
        assertFalse(delegate.isRabbitMqTLS());
    }

    @Test
    void testSettersAndGetters() {
        delegate.setRabbitMqHost("localhost");
        assertEquals("localhost", delegate.getRabbitMqHost());

        delegate.setRabbitMqPort(5672);
        assertEquals(5672, delegate.getRabbitMqPort());

        delegate.setRabbitMqUser("user");
        assertEquals("user", delegate.getRabbitMqUser());

        delegate.setRabbitMqPass("pass");
        assertEquals("pass", delegate.getRabbitMqPass());

        delegate.setRabbitMqPassFile("/path/to/pass");
        assertEquals("/path/to/pass", delegate.getRabbitMqPassFile());

        delegate.setRabbitMqTLS(true);
        assertTrue(delegate.isRabbitMqTLS());
    }

    @Test
    void testJCommanderParsing() {
        String[] args = {
            "-rabbitMqHost", "rabbitmq.example.com",
            "-rabbitMqPort", "5673",
            "-rabbitMqUser", "testuser",
            "-rabbitMqPass", "testpass",
            "-rabbitMqPassFile", "/etc/rabbitmq/pass",
            "-rabbitMqTLS"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(delegate).build();
        jCommander.parse(args);

        assertEquals("rabbitmq.example.com", delegate.getRabbitMqHost());
        assertEquals(5673, delegate.getRabbitMqPort());
        assertEquals("testuser", delegate.getRabbitMqUser());
        assertEquals("testpass", delegate.getRabbitMqPass());
        assertEquals("/etc/rabbitmq/pass", delegate.getRabbitMqPassFile());
        assertTrue(delegate.isRabbitMqTLS());
    }

    @Test
    void testJCommanderParsingPartial() {
        String[] args = {
            "-rabbitMqHost", "localhost",
            "-rabbitMqPort", "5672"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(delegate).build();
        jCommander.parse(args);

        assertEquals("localhost", delegate.getRabbitMqHost());
        assertEquals(5672, delegate.getRabbitMqPort());
        assertNull(delegate.getRabbitMqUser());
        assertNull(delegate.getRabbitMqPass());
        assertNull(delegate.getRabbitMqPassFile());
        assertFalse(delegate.isRabbitMqTLS());
    }

    @Test
    void testJCommanderParsingEmpty() {
        String[] args = {};

        JCommander jCommander = JCommander.newBuilder().addObject(delegate).build();
        jCommander.parse(args);

        assertNull(delegate.getRabbitMqHost());
        assertEquals(0, delegate.getRabbitMqPort());
        assertNull(delegate.getRabbitMqUser());
        assertNull(delegate.getRabbitMqPass());
        assertNull(delegate.getRabbitMqPassFile());
        assertFalse(delegate.isRabbitMqTLS());
    }

    @Test
    void testPasswordAndPasswordFile() {
        // Test that both password and password file can be set
        delegate.setRabbitMqPass("directpass");
        delegate.setRabbitMqPassFile("/path/to/passfile");

        assertEquals("directpass", delegate.getRabbitMqPass());
        assertEquals("/path/to/passfile", delegate.getRabbitMqPassFile());
    }
}
