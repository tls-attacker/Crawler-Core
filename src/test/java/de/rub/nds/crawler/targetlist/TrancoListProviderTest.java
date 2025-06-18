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

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class TrancoListProviderTest {

    @TempDir Path tempDir;

    private TrancoListProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TrancoListProvider(3);
    }

    @Test
    void testGetTargetList() throws Exception {
        // Create test data
        String csvContent =
                "1,google.com\n2,facebook.com\n3,amazon.com\n4,twitter.com\n5,youtube.com";

        // Create a zip file with the CSV content
        Path zipFile = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry entry = new ZipEntry("tranco-1m.csv");
            zos.putNextEntry(entry);
            zos.write(csvContent.getBytes());
            zos.closeEntry();
        }

        // Mock URL and file operations
        try (MockedStatic<Channels> mockedChannels = mockStatic(Channels.class);
                MockedConstruction<URL> mockedURL = mockConstruction(URL.class);
                MockedConstruction<FileOutputStream> mockedFOS =
                        mockConstruction(FileOutputStream.class)) {

            // Setup mocks
            URL mockUrl = mockedURL.constructed().get(0);
            InputStream mockInputStream = new FileInputStream(zipFile.toFile());
            when(mockUrl.openStream()).thenReturn(mockInputStream);

            ReadableByteChannel mockChannel = mock(ReadableByteChannel.class);
            mockedChannels
                    .when(() -> Channels.newChannel(any(InputStream.class)))
                    .thenReturn(mockChannel);

            FileOutputStream mockFos = mockedFOS.constructed().get(0);
            FileChannel mockFileChannel = mock(FileChannel.class);
            when(mockFos.getChannel()).thenReturn(mockFileChannel);
            when(mockFileChannel.transferFrom(any(), anyLong(), anyLong())).thenReturn(0L);

            // Execute
            List<String> targets = provider.getTargetList();

            // Verify
            assertEquals(3, targets.size());
            assertEquals("1,google.com", targets.get(0));
            assertEquals("2,facebook.com", targets.get(1));
            assertEquals("3,amazon.com", targets.get(2));
        }
    }

    @Test
    void testGetTargetListFromLines() {
        // Test the abstract method implementation
        List<String> lines =
                List.of(
                        "1,google.com",
                        "2,facebook.com",
                        "3,amazon.com",
                        "4,twitter.com",
                        "5,youtube.com");

        List<String> result = provider.getTargetListFromLines(lines.stream());

        assertEquals(3, result.size());
        assertEquals("1,google.com", result.get(0));
        assertEquals("2,facebook.com", result.get(1));
        assertEquals("3,amazon.com", result.get(2));
    }

    @Test
    void testGetTargetListFromLinesFewerThanRequested() {
        // Test when there are fewer lines than requested
        List<String> lines = List.of("1,google.com", "2,facebook.com");

        TrancoListProvider provider = new TrancoListProvider(5);
        List<String> result = provider.getTargetListFromLines(lines.stream());

        assertEquals(2, result.size());
        assertEquals("1,google.com", result.get(0));
        assertEquals("2,facebook.com", result.get(1));
    }
}
