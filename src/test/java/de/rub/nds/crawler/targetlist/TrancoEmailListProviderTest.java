/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InitialDirContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

class TrancoEmailListProviderTest {

    @Mock private ITargetListProvider trancoList;

    private TrancoEmailListProvider provider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        provider = new TrancoEmailListProvider(trancoList);
    }

    @Test
    void testGetTargetList() throws NamingException {
        // Setup mock tranco list
        when(trancoList.getTargetList()).thenReturn(List.of("1,google.com", "2,facebook.com"));

        // Mock InitialDirContext
        try (MockedConstruction<InitialDirContext> mockedDirContext =
                mockConstruction(InitialDirContext.class)) {

            InitialDirContext mockDirContext = mockedDirContext.constructed().get(0);

            // Setup MX records for google.com
            Attributes googleAttributes = new BasicAttributes();
            Attribute googleMX = new BasicAttribute("MX");
            googleMX.add("10 aspmx.l.google.com.");
            googleMX.add("20 alt1.aspmx.l.google.com.");
            googleAttributes.put(googleMX);
            when(mockDirContext.getAttributes("dns:/google.com", new String[] {"MX"}))
                    .thenReturn(googleAttributes);

            // Setup MX records for facebook.com
            Attributes facebookAttributes = new BasicAttributes();
            Attribute facebookMX = new BasicAttribute("MX");
            facebookMX.add("10 smtpin.vvv.facebook.com.");
            facebookAttributes.put(facebookMX);
            when(mockDirContext.getAttributes("dns:/facebook.com", new String[] {"MX"}))
                    .thenReturn(facebookAttributes);

            // Execute
            List<String> result = provider.getTargetList();

            // Verify
            assertEquals(3, result.size());
            assertTrue(result.contains("aspmx.l.google.com."));
            assertTrue(result.contains("alt1.aspmx.l.google.com."));
            assertTrue(result.contains("smtpin.vvv.facebook.com."));
        }
    }

    @Test
    void testGetTargetListWithNoMXRecord() throws NamingException {
        // Setup mock tranco list
        when(trancoList.getTargetList()).thenReturn(List.of("1,example.com"));

        // Mock InitialDirContext
        try (MockedConstruction<InitialDirContext> mockedDirContext =
                mockConstruction(InitialDirContext.class)) {

            InitialDirContext mockDirContext = mockedDirContext.constructed().get(0);

            // Setup no MX records
            Attributes emptyAttributes = new BasicAttributes();
            when(mockDirContext.getAttributes("dns:/example.com", new String[] {"MX"}))
                    .thenReturn(emptyAttributes);

            // Execute
            List<String> result = provider.getTargetList();

            // Verify
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testGetTargetListWithNamingException() throws NamingException {
        // Setup mock tranco list
        when(trancoList.getTargetList()).thenReturn(List.of("1,badhost.com"));

        // Mock InitialDirContext
        try (MockedConstruction<InitialDirContext> mockedDirContext =
                mockConstruction(InitialDirContext.class)) {

            InitialDirContext mockDirContext = mockedDirContext.constructed().get(0);

            // Throw NamingException
            when(mockDirContext.getAttributes(anyString(), any(String[].class)))
                    .thenThrow(new NamingException("DNS lookup failed"));

            // Execute
            List<String> result = provider.getTargetList();

            // Verify - should handle exception gracefully
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testGetTargetListWithDuplicateMXRecords() throws NamingException {
        // Setup mock tranco list with multiple domains
        when(trancoList.getTargetList())
                .thenReturn(List.of("1,site1.com", "2,site2.com", "3,site3.com"));

        // Mock InitialDirContext
        try (MockedConstruction<InitialDirContext> mockedDirContext =
                mockConstruction(InitialDirContext.class)) {

            InitialDirContext mockDirContext = mockedDirContext.constructed().get(0);

            // All sites use the same MX server
            Attributes sameAttributes = new BasicAttributes();
            Attribute sameMX = new BasicAttribute("MX");
            sameMX.add("10 mail.shared.com.");
            sameAttributes.put(sameMX);

            when(mockDirContext.getAttributes(anyString(), any(String[].class)))
                    .thenReturn(sameAttributes);

            // Execute
            List<String> result = provider.getTargetList();

            // Verify - duplicates should be removed
            assertEquals(1, result.size());
            assertEquals("mail.shared.com.", result.get(0));
        }
    }

    @Test
    void testGetTargetListInitialDirContextCreationFails() throws NamingException {
        // Setup mock tranco list
        when(trancoList.getTargetList()).thenReturn(List.of("1,google.com"));

        // Mock InitialDirContext constructor to throw
        try (MockedConstruction<InitialDirContext> mockedDirContext =
                mockConstruction(
                        InitialDirContext.class,
                        (mock, context) -> {
                            throw new NamingException("Cannot create context");
                        })) {

            // Execute
            List<String> result = provider.getTargetList();

            // Verify - should handle exception gracefully
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testGetTargetListWithVariousHostFormats() throws NamingException {
        // Setup mock tranco list with various formats
        when(trancoList.getTargetList())
                .thenReturn(
                        List.of(
                                "example.com", // Without rank
                                "1,site.com", // With rank
                                "2,subdomain.site.com" // With subdomain
                                ));

        // Mock InitialDirContext
        try (MockedConstruction<InitialDirContext> mockedDirContext =
                mockConstruction(InitialDirContext.class)) {

            InitialDirContext mockDirContext = mockedDirContext.constructed().get(0);

            // Setup different MX records
            Attributes attr1 = new BasicAttributes();
            Attribute mx1 = new BasicAttribute("MX");
            mx1.add("10 mail1.example.com.");
            attr1.put(mx1);
            when(mockDirContext.getAttributes("dns:/example.com", new String[] {"MX"}))
                    .thenReturn(attr1);

            Attributes attr2 = new BasicAttributes();
            Attribute mx2 = new BasicAttribute("MX");
            mx2.add("10 mail2.site.com.");
            attr2.put(mx2);
            when(mockDirContext.getAttributes("dns:/site.com", new String[] {"MX"}))
                    .thenReturn(attr2);

            Attributes attr3 = new BasicAttributes();
            Attribute mx3 = new BasicAttribute("MX");
            mx3.add("10 mail3.subdomain.site.com.");
            attr3.put(mx3);
            when(mockDirContext.getAttributes("dns:/subdomain.site.com", new String[] {"MX"}))
                    .thenReturn(attr3);

            // Execute
            List<String> result = provider.getTargetList();

            // Verify
            assertEquals(3, result.size());
            assertTrue(result.contains("mail1.example.com."));
            assertTrue(result.contains("mail2.site.com."));
            assertTrue(result.contains("mail3.subdomain.site.com."));
        }
    }

    @Test
    void testGetTargetListWithMultiplePriorityMX() throws NamingException {
        // Setup mock tranco list
        when(trancoList.getTargetList()).thenReturn(List.of("1,example.com"));

        // Mock InitialDirContext
        try (MockedConstruction<InitialDirContext> mockedDirContext =
                mockConstruction(InitialDirContext.class)) {

            InitialDirContext mockDirContext = mockedDirContext.constructed().get(0);

            // Setup MX records with different priorities
            Attributes attributes = new BasicAttributes();
            Attribute mxRecords = new BasicAttribute("MX");
            mxRecords.add("10 primary.example.com.");
            mxRecords.add("20 secondary.example.com.");
            mxRecords.add("30 tertiary.example.com.");
            attributes.put(mxRecords);
            when(mockDirContext.getAttributes("dns:/example.com", new String[] {"MX"}))
                    .thenReturn(attributes);

            // Execute
            List<String> result = provider.getTargetList();

            // Verify - all MX records should be included
            assertEquals(3, result.size());
            assertTrue(result.contains("primary.example.com."));
            assertTrue(result.contains("secondary.example.com."));
            assertTrue(result.contains("tertiary.example.com."));
        }
    }
}
