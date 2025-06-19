/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2025 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config.delegate;

import static org.junit.jupiter.api.Assertions.*;

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
    void testSetAndGetRabbitMqHost() {
        String host = "rabbitmq.example.com";
        delegate.setRabbitMqHost(host);
        assertEquals(host, delegate.getRabbitMqHost());
    }

    @Test
    void testSetAndGetRabbitMqPort() {
        int port = 5672;
        delegate.setRabbitMqPort(port);
        assertEquals(port, delegate.getRabbitMqPort());
    }

    @Test
    void testSetAndGetRabbitMqUser() {
        String user = "rabbituser";
        delegate.setRabbitMqUser(user);
        assertEquals(user, delegate.getRabbitMqUser());
    }

    @Test
    void testSetAndGetRabbitMqPass() {
        String pass = "rabbitpass";
        delegate.setRabbitMqPass(pass);
        assertEquals(pass, delegate.getRabbitMqPass());
    }

    @Test
    void testSetAndGetRabbitMqPassFile() {
        String passFile = "/etc/rabbit/pass.txt";
        delegate.setRabbitMqPassFile(passFile);
        assertEquals(passFile, delegate.getRabbitMqPassFile());
    }

    @Test
    void testSetAndGetRabbitMqTLS() {
        delegate.setRabbitMqTLS(true);
        assertTrue(delegate.isRabbitMqTLS());

        delegate.setRabbitMqTLS(false);
        assertFalse(delegate.isRabbitMqTLS());
    }

    @Test
    void testNullValues() {
        delegate.setRabbitMqHost(null);
        assertNull(delegate.getRabbitMqHost());

        delegate.setRabbitMqUser(null);
        assertNull(delegate.getRabbitMqUser());

        delegate.setRabbitMqPass(null);
        assertNull(delegate.getRabbitMqPass());

        delegate.setRabbitMqPassFile(null);
        assertNull(delegate.getRabbitMqPassFile());
    }

    @Test
    void testEmptyStringValues() {
        delegate.setRabbitMqHost("");
        assertEquals("", delegate.getRabbitMqHost());

        delegate.setRabbitMqUser("");
        assertEquals("", delegate.getRabbitMqUser());

        delegate.setRabbitMqPass("");
        assertEquals("", delegate.getRabbitMqPass());

        delegate.setRabbitMqPassFile("");
        assertEquals("", delegate.getRabbitMqPassFile());
    }

    @Test
    void testPortBoundaryValues() {
        delegate.setRabbitMqPort(0);
        assertEquals(0, delegate.getRabbitMqPort());

        delegate.setRabbitMqPort(65535);
        assertEquals(65535, delegate.getRabbitMqPort());

        delegate.setRabbitMqPort(-1);
        assertEquals(-1, delegate.getRabbitMqPort());

        delegate.setRabbitMqPort(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, delegate.getRabbitMqPort());

        delegate.setRabbitMqPort(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, delegate.getRabbitMqPort());
    }

    @Test
    void testOverwriteValues() {
        delegate.setRabbitMqHost("host1");
        delegate.setRabbitMqHost("host2");
        assertEquals("host2", delegate.getRabbitMqHost());

        delegate.setRabbitMqPort(5672);
        delegate.setRabbitMqPort(5673);
        assertEquals(5673, delegate.getRabbitMqPort());

        delegate.setRabbitMqTLS(true);
        delegate.setRabbitMqTLS(false);
        assertFalse(delegate.isRabbitMqTLS());
    }

    @Test
    void testPasswordAndPasswordFile() {
        delegate.setRabbitMqPass("password");
        delegate.setRabbitMqPassFile("/path/to/file");

        assertEquals("password", delegate.getRabbitMqPass());
        assertEquals("/path/to/file", delegate.getRabbitMqPassFile());
    }

    @Test
    void testTLSPortCombination() {
        delegate.setRabbitMqPort(5671);
        delegate.setRabbitMqTLS(true);

        assertEquals(5671, delegate.getRabbitMqPort());
        assertTrue(delegate.isRabbitMqTLS());
    }

    @Test
    void testBooleanGetterNaming() {
        assertFalse(delegate.isRabbitMqTLS());

        delegate.setRabbitMqTLS(true);
        assertTrue(delegate.isRabbitMqTLS());
    }
}
