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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongoDbDelegateTest {

    private MongoDbDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new MongoDbDelegate();
    }

    @Test
    void testGetMongoDbHost() {
        assertNull(delegate.getMongoDbHost());
    }

    @Test
    void testSetMongoDbHost() {
        String host = "localhost";
        delegate.setMongoDbHost(host);
        assertEquals(host, delegate.getMongoDbHost());
    }

    @Test
    void testGetMongoDbPort() {
        assertEquals(0, delegate.getMongoDbPort());
    }

    @Test
    void testSetMongoDbPort() {
        int port = 27017;
        delegate.setMongoDbPort(port);
        assertEquals(port, delegate.getMongoDbPort());
    }

    @Test
    void testGetMongoDbUser() {
        assertNull(delegate.getMongoDbUser());
    }

    @Test
    void testSetMongoDbUser() {
        String user = "testuser";
        delegate.setMongoDbUser(user);
        assertEquals(user, delegate.getMongoDbUser());
    }

    @Test
    void testGetMongoDbPass() {
        assertNull(delegate.getMongoDbPass());
    }

    @Test
    void testSetMongoDbPass() {
        String pass = "testpass";
        delegate.setMongoDbPass(pass);
        assertEquals(pass, delegate.getMongoDbPass());
    }

    @Test
    void testGetMongoDbPassFile() {
        assertNull(delegate.getMongoDbPassFile());
    }

    @Test
    void testSetMongoDbPassFile() {
        String passFile = "/path/to/passfile";
        delegate.setMongoDbPassFile(passFile);
        assertEquals(passFile, delegate.getMongoDbPassFile());
    }

    @Test
    void testGetMongoDbAuthSource() {
        assertNull(delegate.getMongoDbAuthSource());
    }

    @Test
    void testSetMongoDbAuthSource() {
        String authSource = "admin";
        delegate.setMongoDbAuthSource(authSource);
        assertEquals(authSource, delegate.getMongoDbAuthSource());
    }

    @Test
    void testAllPropertiesTogether() {
        String host = "mongodb.example.com";
        int port = 27018;
        String user = "appuser";
        String pass = "apppass";
        String passFile = "/etc/mongodb/pass";
        String authSource = "mydb";

        delegate.setMongoDbHost(host);
        delegate.setMongoDbPort(port);
        delegate.setMongoDbUser(user);
        delegate.setMongoDbPass(pass);
        delegate.setMongoDbPassFile(passFile);
        delegate.setMongoDbAuthSource(authSource);

        assertEquals(host, delegate.getMongoDbHost());
        assertEquals(port, delegate.getMongoDbPort());
        assertEquals(user, delegate.getMongoDbUser());
        assertEquals(pass, delegate.getMongoDbPass());
        assertEquals(passFile, delegate.getMongoDbPassFile());
        assertEquals(authSource, delegate.getMongoDbAuthSource());
    }
}
