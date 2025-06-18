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
    void testSettersAndGetters() {
        delegate.setMongoDbHost("localhost");
        assertEquals("localhost", delegate.getMongoDbHost());

        delegate.setMongoDbPort(27017);
        assertEquals(27017, delegate.getMongoDbPort());

        delegate.setMongoDbUser("user");
        assertEquals("user", delegate.getMongoDbUser());

        delegate.setMongoDbPass("pass");
        assertEquals("pass", delegate.getMongoDbPass());

        delegate.setMongoDbPassFile("/path/to/pass");
        assertEquals("/path/to/pass", delegate.getMongoDbPassFile());

        delegate.setMongoDbAuthSource("admin");
        assertEquals("admin", delegate.getMongoDbAuthSource());
    }

    @Test
    void testJCommanderParsing() {
        String[] args = {
            "-mongoDbHost", "mongo.example.com",
            "-mongoDbPort", "27018",
            "-mongoDbUser", "testuser",
            "-mongoDbPass", "testpass",
            "-mongoDbPassFile", "/etc/mongodb/pass",
            "-mongoDbAuthSource", "testdb"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(delegate).build();
        jCommander.parse(args);

        assertEquals("mongo.example.com", delegate.getMongoDbHost());
        assertEquals(27018, delegate.getMongoDbPort());
        assertEquals("testuser", delegate.getMongoDbUser());
        assertEquals("testpass", delegate.getMongoDbPass());
        assertEquals("/etc/mongodb/pass", delegate.getMongoDbPassFile());
        assertEquals("testdb", delegate.getMongoDbAuthSource());
    }

    @Test
    void testJCommanderParsingPartial() {
        String[] args = {
            "-mongoDbHost", "localhost",
            "-mongoDbPort", "27017",
            "-mongoDbAuthSource", "admin"
        };

        JCommander jCommander = JCommander.newBuilder().addObject(delegate).build();
        jCommander.parse(args);

        assertEquals("localhost", delegate.getMongoDbHost());
        assertEquals(27017, delegate.getMongoDbPort());
        assertNull(delegate.getMongoDbUser());
        assertNull(delegate.getMongoDbPass());
        assertNull(delegate.getMongoDbPassFile());
        assertEquals("admin", delegate.getMongoDbAuthSource());
    }

    @Test
    void testJCommanderParsingEmpty() {
        String[] args = {};

        JCommander jCommander = JCommander.newBuilder().addObject(delegate).build();
        jCommander.parse(args);

        assertNull(delegate.getMongoDbHost());
        assertEquals(0, delegate.getMongoDbPort());
        assertNull(delegate.getMongoDbUser());
        assertNull(delegate.getMongoDbPass());
        assertNull(delegate.getMongoDbPassFile());
        assertNull(delegate.getMongoDbAuthSource());
    }

    @Test
    void testPasswordAndPasswordFile() {
        // Test that both password and password file can be set
        delegate.setMongoDbPass("directpass");
        delegate.setMongoDbPassFile("/path/to/passfile");

        assertEquals("directpass", delegate.getMongoDbPass());
        assertEquals("/path/to/passfile", delegate.getMongoDbPassFile());
    }

    @Test
    void testAuthSourceConfiguration() {
        // Test different auth source configurations
        delegate.setMongoDbAuthSource("admin");
        assertEquals("admin", delegate.getMongoDbAuthSource());

        delegate.setMongoDbAuthSource("myappdb");
        assertEquals("myappdb", delegate.getMongoDbAuthSource());

        delegate.setMongoDbAuthSource("$external");
        assertEquals("$external", delegate.getMongoDbAuthSource());
    }
}
