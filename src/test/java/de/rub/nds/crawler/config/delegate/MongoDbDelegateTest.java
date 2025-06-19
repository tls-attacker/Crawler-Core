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

class MongoDbDelegateTest {

    private MongoDbDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new MongoDbDelegate();
    }

    @Test
    void testDefaultValues() {
        assertNull(delegate.getMongoDbHost());
        assertEquals(0, delegate.getMongoDbPort());
        assertNull(delegate.getMongoDbUser());
        assertNull(delegate.getMongoDbPass());
        assertNull(delegate.getMongoDbPassFile());
        assertNull(delegate.getMongoDbAuthSource());
    }

    @Test
    void testSetAndGetMongoDbHost() {
        String host = "localhost";
        delegate.setMongoDbHost(host);
        assertEquals(host, delegate.getMongoDbHost());
    }

    @Test
    void testSetAndGetMongoDbPort() {
        int port = 27017;
        delegate.setMongoDbPort(port);
        assertEquals(port, delegate.getMongoDbPort());
    }

    @Test
    void testSetAndGetMongoDbUser() {
        String user = "testuser";
        delegate.setMongoDbUser(user);
        assertEquals(user, delegate.getMongoDbUser());
    }

    @Test
    void testSetAndGetMongoDbPass() {
        String pass = "testpass";
        delegate.setMongoDbPass(pass);
        assertEquals(pass, delegate.getMongoDbPass());
    }

    @Test
    void testSetAndGetMongoDbPassFile() {
        String passFile = "/path/to/passfile";
        delegate.setMongoDbPassFile(passFile);
        assertEquals(passFile, delegate.getMongoDbPassFile());
    }

    @Test
    void testSetAndGetMongoDbAuthSource() {
        String authSource = "admin";
        delegate.setMongoDbAuthSource(authSource);
        assertEquals(authSource, delegate.getMongoDbAuthSource());
    }

    @Test
    void testNullValues() {
        delegate.setMongoDbHost(null);
        assertNull(delegate.getMongoDbHost());

        delegate.setMongoDbUser(null);
        assertNull(delegate.getMongoDbUser());

        delegate.setMongoDbPass(null);
        assertNull(delegate.getMongoDbPass());

        delegate.setMongoDbPassFile(null);
        assertNull(delegate.getMongoDbPassFile());

        delegate.setMongoDbAuthSource(null);
        assertNull(delegate.getMongoDbAuthSource());
    }

    @Test
    void testEmptyStringValues() {
        delegate.setMongoDbHost("");
        assertEquals("", delegate.getMongoDbHost());

        delegate.setMongoDbUser("");
        assertEquals("", delegate.getMongoDbUser());

        delegate.setMongoDbPass("");
        assertEquals("", delegate.getMongoDbPass());

        delegate.setMongoDbPassFile("");
        assertEquals("", delegate.getMongoDbPassFile());

        delegate.setMongoDbAuthSource("");
        assertEquals("", delegate.getMongoDbAuthSource());
    }

    @Test
    void testPortBoundaryValues() {
        delegate.setMongoDbPort(0);
        assertEquals(0, delegate.getMongoDbPort());

        delegate.setMongoDbPort(65535);
        assertEquals(65535, delegate.getMongoDbPort());

        delegate.setMongoDbPort(-1);
        assertEquals(-1, delegate.getMongoDbPort());

        delegate.setMongoDbPort(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, delegate.getMongoDbPort());

        delegate.setMongoDbPort(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, delegate.getMongoDbPort());
    }

    @Test
    void testOverwriteValues() {
        delegate.setMongoDbHost("host1");
        delegate.setMongoDbHost("host2");
        assertEquals("host2", delegate.getMongoDbHost());

        delegate.setMongoDbPort(27017);
        delegate.setMongoDbPort(27018);
        assertEquals(27018, delegate.getMongoDbPort());
    }

    @Test
    void testPasswordAndPasswordFile() {
        delegate.setMongoDbPass("password");
        delegate.setMongoDbPassFile("/path/to/file");

        assertEquals("password", delegate.getMongoDbPass());
        assertEquals("/path/to/file", delegate.getMongoDbPassFile());
    }
}
