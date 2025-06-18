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
 * Abstract base class for target list providers that download and extract target lists from
 * compressed files. Supports .gz and .zip file formats.
 */
public abstract class ZipFileProvider implements ITargetListProvider {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected final int number;
    private final String sourceUrl;
    private final String zipFilename;
    private final String outputFile;
    private final String listName;

    /**
     * Constructs a new ZipFileProvider with the specified parameters.
     *
     * @param number The number of entries to extract from the list
     * @param sourceUrl The URL to download the compressed file from
     * @param zipFilename The local filename to save the downloaded compressed file
     * @param outputFile The filename for the extracted content
     * @param listName The name of the list for logging purposes
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
     * Downloads the compressed target list, extracts it, and returns the processed entries.
     * Automatically cleans up the downloaded and extracted files after processing.
     *
     * @return A list of target hostnames extracted from the downloaded file
     * @throws RuntimeException If the file cannot be loaded or processed
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
     * Creates an appropriate input stream based on the file extension.
     *
     * @param filename The filename to determine the compression type
     * @return A GZIPInputStream for .gz files or ZipInputStream for other files
     * @throws IOException If the file cannot be opened
     */
    private InflaterInputStream getZipInputStream(String filename) throws IOException {
        if (filename.contains(".gz")) {
            return new GZIPInputStream(new FileInputStream(filename));
        } else {
            return new ZipInputStream(new FileInputStream(filename));
        }
    }

    /**
     * Processes the lines from the extracted file to create the target list. Subclasses must
     * implement this method to define how to extract targets from the file content.
     *
     * @param lines A stream of lines from the extracted file
     * @return A list of target hostnames
     */
    protected abstract List<String> getTargetListFromLines(Stream<String> lines);
}
