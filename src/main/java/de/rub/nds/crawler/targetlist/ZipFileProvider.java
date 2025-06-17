/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract base class for target list providers that download and extract targets from compressed
 * archives.
 *
 * <p>The ZipFileProvider provides a foundation for implementing target list providers that obtain
 * scan targets from remote compressed files (ZIP, GZIP). It handles the complete workflow of
 * downloading, extracting, parsing, and cleaning up temporary files, allowing subclasses to focus
 * on the specific target extraction logic.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li><strong>Remote Download</strong> - Downloads compressed files from HTTP/HTTPS URLs
 *   <li><strong>Format Support</strong> - Handles ZIP and GZIP compressed formats
 *   <li><strong>Stream Processing</strong> - Efficient processing of large target lists
 *   <li><strong>Automatic Cleanup</strong> - Removes temporary files after processing
 * </ul>
 *
 * <p><strong>Processing Workflow:</strong>
 *
 * <ol>
 *   <li><strong>Download</strong> - Fetch compressed file from remote URL
 *   <li><strong>Extract</strong> - Decompress file to temporary local storage
 *   <li><strong>Parse</strong> - Process extracted content via subclass implementation
 *   <li><strong>Cleanup</strong> - Remove temporary files to free disk space
 * </ol>
 *
 * <p><strong>Supported Formats:</strong>
 *
 * <ul>
 *   <li><strong>ZIP Files</strong> - Standard ZIP compression with single entry support
 *   <li><strong>GZIP Files</strong> - GNU ZIP compression for single file archives
 *   <li><strong>Format Detection</strong> - Automatic format detection based on filename
 * </ul>
 *
 * <p><strong>Error Handling:</strong>
 *
 * <ul>
 *   <li><strong>Download Failures</strong> - Logged but processing continues with cached data
 *   <li><strong>Extraction Errors</strong> - Logged and may cause runtime exceptions
 *   <li><strong>Cleanup Failures</strong> - Logged but don't prevent target list return
 * </ul>
 *
 * <p><strong>Performance Considerations:</strong>
 *
 * <ul>
 *   <li><strong>Temporary Storage</strong> - Requires disk space for compressed and extracted files
 *   <li><strong>Network I/O</strong> - Download time depends on file size and connection speed
 *   <li><strong>Memory Usage</strong> - Uses streaming for processing large target lists
 * </ul>
 *
 * <p><strong>Implementation Requirements:</strong> Subclasses must implement
 * getTargetListFromLines() to define how targets are extracted from the decompressed file content.
 *
 * <p><strong>Common Subclasses:</strong>
 *
 * <ul>
 *   <li><strong>TrancoListProvider</strong> - Processes Tranco web ranking data
 *   <li><strong>CruxListProvider</strong> - Handles Chrome UX Report target lists
 *   <li><strong>Custom Providers</strong> - Domain-specific compressed target sources
 * </ul>
 *
 * @see ITargetListProvider
 * @see TrancoListProvider
 * @see CruxListProvider
 */
public abstract class ZipFileProvider implements ITargetListProvider {

    /** Logger instance for tracking download and extraction operations. */
    protected static final Logger LOGGER = LogManager.getLogger();

    /** Maximum number of targets to extract from the target list. */
    protected final int number;

    private final String sourceUrl;
    private final String zipFilename;
    private final String outputFile;
    private final String listName;

    /**
     * Creates a new ZIP file provider with the specified configuration.
     *
     * @param number the maximum number of targets to extract from the list
     * @param sourceUrl the URL to download the compressed file from
     * @param zipFilename the local filename for the downloaded compressed file
     * @param outputFile the local filename for the extracted content
     * @param listName the human-readable name of the list for logging
     */
    protected ZipFileProvider(
            int number, String sourceUrl, String zipFilename, String outputFile, String listName) {
        this.number = number;
        this.sourceUrl = sourceUrl;
        this.zipFilename = zipFilename;
        this.outputFile = outputFile;
        this.listName = listName;
    }

    /**
     * Downloads, extracts, and processes the compressed target list file.
     *
     * <p>This method implements the complete workflow for obtaining targets from a remote
     * compressed file. It downloads the file, extracts the content, processes it through the
     * subclass implementation, and cleans up temporary files.
     *
     * <p><strong>Processing Steps:</strong>
     *
     * <ol>
     *   <li>Download compressed file from sourceUrl to zipFilename
     *   <li>Extract compressed content to outputFile
     *   <li>Process extracted content via getTargetListFromLines()
     *   <li>Delete temporary files (compressed and extracted)
     * </ol>
     *
     * <p><strong>Error Recovery:</strong> Download and extraction errors are logged but don't
     * prevent processing from continuing. Cleanup errors are logged but don't affect the returned
     * target list.
     *
     * @return a list of target strings extracted from the compressed file
     * @throws RuntimeException if the extracted file cannot be read
     */
    public List<String> getTargetList() {
        List<String> targetList;
        try {
            ReadableByteChannel readableByteChannel =
                    Channels.newChannel(new URL(sourceUrl).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(zipFilename);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileOutputStream.close();
        } catch (IOException e) {
            LOGGER.error("Could not download the current {} list with error ", listName, e);
        }
        LOGGER.info("Unzipping current {} list...", listName);
        try (InflaterInputStream zis = getZipInputStream(zipFilename)) {
            if (zis instanceof ZipInputStream) {
                ((ZipInputStream) zis).getNextEntry();
            }
            File newFile = new File(outputFile);
            // write file content
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            byte[] buffer = new byte[1024];
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
        } catch (IOException e) {
            LOGGER.error("Could not unzip the current {} list with error ", listName, e);
        }
        LOGGER.info("Reading first {} hosts from current {} list...", number, listName);
        // currently hosts are in order. e.g. top 1000 hosts come first but that does not have to be
        // the case. Therefore, we parse every line until we hit the specified number of hosts
        try (Stream<String> lines = Files.lines(Paths.get(outputFile))) {
            targetList = getTargetListFromLines(lines);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load " + outputFile, ex);
        }
        LOGGER.info("Deleting files...");
        try {
            Files.delete(Path.of(zipFilename));
        } catch (IOException e) {
            LOGGER.error("Could not delete {}: ", zipFilename, e);
        }
        try {
            Files.delete(Path.of(outputFile));
        } catch (IOException e) {
            LOGGER.error("Could not delete {}: ", outputFile, e);
        }
        return targetList;
    }

    /**
     * Creates an appropriate input stream for the compressed file based on filename.
     *
     * <p>This method automatically detects the compression format based on the filename and returns
     * the appropriate decompression stream. It supports GZIP and ZIP formats.
     *
     * @param filename the name of the compressed file to open
     * @return an InflaterInputStream for reading decompressed content
     * @throws IOException if the file cannot be opened
     */
    private InflaterInputStream getZipInputStream(String filename) throws IOException {
        if (filename.contains(".gz")) {
            return new GZIPInputStream(new FileInputStream(filename));
        } else {
            return new ZipInputStream(new FileInputStream(filename));
        }
    }

    /**
     * Extracts scan targets from the decompressed file content.
     *
     * <p>This abstract method must be implemented by subclasses to define how targets are extracted
     * from the decompressed file lines. Different target list formats require different parsing
     * logic.
     *
     * <p><strong>Implementation Guidelines:</strong>
     *
     * <ul>
     *   <li>Process the stream efficiently using stream operations
     *   <li>Limit results to the configured number of targets
     *   <li>Filter and format targets appropriately for scanning
     *   <li>Handle any format-specific parsing requirements
     * </ul>
     *
     * @param lines a stream of lines from the extracted file
     * @return a list of target strings formatted for scanning
     */
    protected abstract List<String> getTargetListFromLines(Stream<String> lines);
}
