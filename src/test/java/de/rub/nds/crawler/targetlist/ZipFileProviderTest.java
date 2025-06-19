/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
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
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class ZipFileProviderTest {

    @TempDir Path tempDir;

    private TestZipFileProvider provider;

    private static class TestZipFileProvider extends ZipFileProvider {
        private final List<String> mockTargets;

        public TestZipFileProvider(
                int number,
                String sourceUrl,
                String zipFilename,
                String outputFile,
                String listName,
                List<String> mockTargets) {
            super(number, sourceUrl, zipFilename, outputFile, listName);
            this.mockTargets = mockTargets;
        }

        @Override
        protected List<String> getTargetListFromLines(Stream<String> lines) {
            return mockTargets != null ? mockTargets : lines.limit(number).toList();
        }
    }

    @BeforeEach
    void setUp() {
        provider =
                new TestZipFileProvider(
                        5,
                        "http://example.com/test.zip",
                        tempDir.resolve("test.zip").toString(),
                        tempDir.resolve("test.csv").toString(),
                        "TestList",
                        null);
    }

    @Test
    void testGetTargetListWithZipFile() throws Exception {
        // Create test data
        String csvContent = "host1.com\nhost2.com\nhost3.com\nhost4.com\nhost5.com\nhost6.com";

        // Create a zip file with the CSV content
        Path realZipFile = tempDir.resolve("real.zip");
        try (ZipOutputStream zos =
                new ZipOutputStream(new FileOutputStream(realZipFile.toFile()))) {
            ZipEntry entry = new ZipEntry("test.csv");
            zos.putNextEntry(entry);
            zos.write(csvContent.getBytes());
            zos.closeEntry();
        }

        // Mock URL download
        try (MockedConstruction<URL> mockedURL = mockConstruction(URL.class);
                MockedStatic<Channels> mockedChannels = mockStatic(Channels.class);
                MockedConstruction<FileOutputStream> mockedFOS =
                        mockConstruction(FileOutputStream.class)) {

            // Setup URL mock
            URL mockUrl = mockedURL.constructed().get(0);
            InputStream mockInputStream = new FileInputStream(realZipFile.toFile());
            when(mockUrl.openStream()).thenReturn(mockInputStream);

            // Setup channel mock
            ReadableByteChannel mockChannel = mock(ReadableByteChannel.class);
            mockedChannels
                    .when(() -> Channels.newChannel(any(InputStream.class)))
                    .thenReturn(mockChannel);

            // Setup FileOutputStream mock to actually write the file
            FileOutputStream realFos = new FileOutputStream(tempDir.resolve("test.zip").toFile());
            Files.copy(realZipFile, realFos);
            realFos.close();

            // Execute
            List<String> targets = provider.getTargetList();

            // Verify
            assertEquals(5, targets.size());
            assertEquals("host1.com", targets.get(0));
            assertEquals("host5.com", targets.get(4));

            // Verify files are deleted
            assertFalse(Files.exists(tempDir.resolve("test.zip")));
            assertFalse(Files.exists(tempDir.resolve("test.csv")));
        }
    }

    @Test
    void testGetTargetListWithGzipFile() throws Exception {
        // Create test data
        String csvContent = "host1.com\nhost2.com\nhost3.com";

        // Create a gzip file with the CSV content
        Path gzipFile = tempDir.resolve("test.csv.gz");
        try (GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(gzipFile.toFile()))) {
            gos.write(csvContent.getBytes());
        }

        // Create provider for gzip file
        TestZipFileProvider gzipProvider =
                new TestZipFileProvider(
                        3,
                        "http://example.com/test.csv.gz",
                        gzipFile.toString(),
                        tempDir.resolve("test.csv").toString(),
                        "TestList",
                        null);

        // Mock URL download
        try (MockedConstruction<URL> mockedURL = mockConstruction(URL.class);
                MockedStatic<Channels> mockedChannels = mockStatic(Channels.class)) {

            // Setup URL mock
            URL mockUrl = mockedURL.constructed().get(0);
            InputStream mockInputStream = new FileInputStream(gzipFile.toFile());
            when(mockUrl.openStream()).thenReturn(mockInputStream);

            // Setup channel mock to do nothing (file already exists)
            ReadableByteChannel mockChannel = mock(ReadableByteChannel.class);
            mockedChannels
                    .when(() -> Channels.newChannel(any(InputStream.class)))
                    .thenReturn(mockChannel);

            // Execute
            List<String> targets = gzipProvider.getTargetList();

            // Verify
            assertEquals(3, targets.size());
            assertEquals("host1.com", targets.get(0));
            assertEquals("host3.com", targets.get(2));
        }
    }

    @Test
    void testGetTargetListDownloadError() throws Exception {
        // Mock URL download to fail
        try (MockedConstruction<URL> mockedURL = mockConstruction(URL.class)) {
            URL mockUrl = mockedURL.constructed().get(0);
            when(mockUrl.openStream()).thenThrow(new IOException("Download failed"));

            // Create empty zip file so unzip phase can proceed
            Path zipFile = tempDir.resolve("test.zip");
            try (ZipOutputStream zos =
                    new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
                ZipEntry entry = new ZipEntry("test.csv");
                zos.putNextEntry(entry);
                zos.write("host1.com".getBytes());
                zos.closeEntry();
            }

            // Execute - should not throw
            List<String> targets = provider.getTargetList();

            // Should still return results from existing file
            assertNotNull(targets);
        }
    }

    @Test
    void testGetTargetListUnzipError() throws Exception {
        // Create invalid zip file
        Path invalidZipFile = tempDir.resolve("test.zip");
        Files.write(invalidZipFile, "This is not a zip file".getBytes());

        // Mock URL download
        try (MockedConstruction<URL> mockedURL = mockConstruction(URL.class);
                MockedStatic<Channels> mockedChannels = mockStatic(Channels.class)) {

            URL mockUrl = mockedURL.constructed().get(0);
            InputStream mockInputStream = new ByteArrayInputStream("dummy".getBytes());
            when(mockUrl.openStream()).thenReturn(mockInputStream);

            ReadableByteChannel mockChannel = mock(ReadableByteChannel.class);
            mockedChannels
                    .when(() -> Channels.newChannel(any(InputStream.class)))
                    .thenReturn(mockChannel);

            // Execute - should throw RuntimeException
            assertThrows(RuntimeException.class, () -> provider.getTargetList());
        }
    }

    @Test
    void testGetTargetListReadFileError() throws Exception {
        // Create provider with non-existent output file
        TestZipFileProvider errorProvider =
                new TestZipFileProvider(
                        5,
                        "http://example.com/test.zip",
                        tempDir.resolve("test.zip").toString(),
                        "/non/existent/path/test.csv",
                        "TestList",
                        null);

        // Create valid zip file
        Path zipFile = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry entry = new ZipEntry("test.csv");
            zos.putNextEntry(entry);
            zos.write("host1.com".getBytes());
            zos.closeEntry();
        }

        // Mock URL download
        try (MockedConstruction<URL> mockedURL = mockConstruction(URL.class);
                MockedStatic<Channels> mockedChannels = mockStatic(Channels.class)) {

            URL mockUrl = mockedURL.constructed().get(0);
            InputStream mockInputStream = new ByteArrayInputStream("dummy".getBytes());
            when(mockUrl.openStream()).thenReturn(mockInputStream);

            ReadableByteChannel mockChannel = mock(ReadableByteChannel.class);
            mockedChannels
                    .when(() -> Channels.newChannel(any(InputStream.class)))
                    .thenReturn(mockChannel);

            // Execute - should throw RuntimeException
            assertThrows(RuntimeException.class, () -> errorProvider.getTargetList());
        }
    }

    @Test
    void testDeleteFileErrors() throws Exception {
        // Create files that will fail to delete
        Path zipFile = tempDir.resolve("test.zip");
        Path csvFile = tempDir.resolve("test.csv");

        // Create zip file
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry entry = new ZipEntry("test.csv");
            zos.putNextEntry(entry);
            zos.write("host1.com".getBytes());
            zos.closeEntry();
        }

        // Mock file deletion to fail
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class, CALLS_REAL_METHODS);
                MockedConstruction<URL> mockedURL = mockConstruction(URL.class);
                MockedStatic<Channels> mockedChannels = mockStatic(Channels.class)) {

            // Mock file operations
            mockedFiles
                    .when(() -> Files.delete(zipFile))
                    .thenThrow(new IOException("Cannot delete zip"));
            mockedFiles
                    .when(() -> Files.delete(csvFile))
                    .thenThrow(new IOException("Cannot delete csv"));

            // Allow reading
            mockedFiles.when(() -> Files.lines(csvFile)).thenReturn(Stream.of("host1.com"));

            URL mockUrl = mockedURL.constructed().get(0);
            InputStream mockInputStream = new ByteArrayInputStream("dummy".getBytes());
            when(mockUrl.openStream()).thenReturn(mockInputStream);

            ReadableByteChannel mockChannel = mock(ReadableByteChannel.class);
            mockedChannels
                    .when(() -> Channels.newChannel(any(InputStream.class)))
                    .thenReturn(mockChannel);

            // Execute - should not throw despite delete errors
            List<String> targets = provider.getTargetList();
            assertNotNull(targets);
        }
    }
}
