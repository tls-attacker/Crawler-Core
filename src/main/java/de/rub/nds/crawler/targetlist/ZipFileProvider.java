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
 * Abstract base class for target list providers that download and extract lists from zip files.
 * Handles downloading, unzipping, and processing of compressed target lists from remote sources.
 */
public abstract class ZipFileProvider implements ITargetListProvider {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected final int number;
    private final String sourceUrl;
    private final String zipFilename;
    private final String outputFile;
    private final String listName;

    /**
     * Creates a new ZipFileProvider with the specified configuration.
     *
     * @param number the number of targets to extract from the list
     * @param sourceUrl the URL to download the compressed list from
     * @param zipFilename the local filename to save the downloaded zip file
     * @param outputFile the filename for the extracted content
     * @param listName the name of the list for logging purposes
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
     * Downloads, extracts, and processes the target list. This method handles the complete workflow
     * of downloading the compressed list, extracting it, reading the targets, and cleaning up
     * temporary files.
     *
     * @return a list of target identifiers extracted from the downloaded list
     * @throws RuntimeException if the file cannot be loaded or processed
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

    private InflaterInputStream getZipInputStream(String filename) throws IOException {
        if (filename.contains(".gz")) {
            return new GZIPInputStream(new FileInputStream(filename));
        } else {
            return new ZipInputStream(new FileInputStream(filename));
        }
    }

    /**
     * Processes the lines from the extracted file to create the target list. Subclasses must
     * implement this method to handle their specific file format.
     *
     * @param lines a stream of lines from the extracted file
     * @return a list of target identifiers parsed from the lines
     */
    protected abstract List<String> getTargetListFromLines(Stream<String> lines);
}
