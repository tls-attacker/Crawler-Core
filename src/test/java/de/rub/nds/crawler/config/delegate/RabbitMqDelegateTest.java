/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.config.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RabbitMqDelegateTest {

    private RabbitMqDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new RabbitMqDelegate();
    }

    @Test
    void testGetRabbitMqHost() {
        assertNull(delegate.getRabbitMqHost());
    }

    @Test
    void testSetRabbitMqHost() {
        String host = "rabbitmq.example.com";
        delegate.setRabbitMqHost(host);
        assertEquals(host, delegate.getRabbitMqHost());
    }

    @Test
    void testGetRabbitMqPort() {
        assertEquals(0, delegate.getRabbitMqPort());
    }

    @Test
    void testSetRabbitMqPort() {
        int port = 5672;
        delegate.setRabbitMqPort(port);
        assertEquals(port, delegate.getRabbitMqPort());
    }

    @Test
    void testGetRabbitMqUser() {
        assertNull(delegate.getRabbitMqUser());
    }

    @Test
    void testSetRabbitMqUser() {
        String user = "rabbituser";
        delegate.setRabbitMqUser(user);
        assertEquals(user, delegate.getRabbitMqUser());
    }

    @Test
    void testGetRabbitMqPass() {
        assertNull(delegate.getRabbitMqPass());
    }

    @Test
    void testSetRabbitMqPass() {
        String pass = "rabbitpass";
        delegate.setRabbitMqPass(pass);
        assertEquals(pass, delegate.getRabbitMqPass());
    }

    @Test
    void testGetRabbitMqPassFile() {
        assertNull(delegate.getRabbitMqPassFile());
    }

    @Test
    void testSetRabbitMqPassFile() {
        String passFile = "/etc/rabbitmq/pass";
        delegate.setRabbitMqPassFile(passFile);
        assertEquals(passFile, delegate.getRabbitMqPassFile());
    }

    @Test
    void testIsRabbitMqTLS() {
        assertFalse(delegate.isRabbitMqTLS());
    }

    @Test
    void testSetRabbitMqTLS() {
        delegate.setRabbitMqTLS(true);
        assertTrue(delegate.isRabbitMqTLS());

        delegate.setRabbitMqTLS(false);
        assertFalse(delegate.isRabbitMqTLS());
    }

    @Test
    void testAllPropertiesTogether() {
        String host = "rabbitmq.secure.com";
        int port = 5671;
        String user = "secureuser";
        String pass = "securepass";
        String passFile = "/secure/rabbitmq/pass";
        boolean tls = true;

        delegate.setRabbitMqHost(host);
        delegate.setRabbitMqPort(port);
        delegate.setRabbitMqUser(user);
        delegate.setRabbitMqPass(pass);
        delegate.setRabbitMqPassFile(passFile);
        delegate.setRabbitMqTLS(tls);

        assertEquals(host, delegate.getRabbitMqHost());
        assertEquals(port, delegate.getRabbitMqPort());
        assertEquals(user, delegate.getRabbitMqUser());
        assertEquals(pass, delegate.getRabbitMqPass());
        assertEquals(passFile, delegate.getRabbitMqPassFile());
        assertTrue(delegate.isRabbitMqTLS());
    }
}
