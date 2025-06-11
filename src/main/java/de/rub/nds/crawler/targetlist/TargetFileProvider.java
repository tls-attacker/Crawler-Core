/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * File-based target list provider for reading scan targets from local text files.
 *
 * <p>The TargetFileProvider implements ITargetListProvider to supply scan targets by reading from a
 * local text file. It supports common file formats with comment filtering and empty line handling,
 * making it suitable for managing static target lists in development, testing, and production
 * environments.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>File-Based Storage</strong> - Reads targets from local filesystem files
 *   <li><strong>Comment Support</strong> - Filters out lines starting with '#' character
 *   <li><strong>Empty Line Handling</strong> - Automatically removes empty lines
 *   <li><strong>Stream Processing</strong> - Uses Java streams for efficient file processing
 * </ul>
 *
 * <p><strong>File Format:</strong>
 *
 * <ul>
 *   <li><strong>One Target Per Line</strong> - Each line contains a single target specification
 *   <li><strong>Comment Lines</strong> - Lines starting with '#' are ignored
 *   <li><strong>Empty Lines</strong> - Blank lines are automatically filtered out
 *   <li><strong>Target Format</strong> - hostname[:port] format (e.g., "example.com:443")
 * </ul>
 *
 * <p><strong>Example File Content:</strong>
 *
 * <pre>
 * # TLS Crawler Target List
 * # Production servers
 * example.com:443
 * api.example.com
 * secure.example.org:8443
 *
 * # Test servers
 * test.example.com:443
 * </pre>
 *
 * <p><strong>Error Handling:</strong> File access errors (file not found, permission denied, I/O
 * errors) are wrapped in RuntimeException with descriptive messages for troubleshooting.
 *
 * <p><strong>Performance Characteristics:</strong>
 *
 * <ul>
 *   <li><strong>Memory Efficient</strong> - Uses streams to process large files
 *   <li><strong>Fast Processing</strong> - Efficient filtering and collection operations
 *   <li><strong>One-Time Read</strong> - File is read completely on each getTargetList() call
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * TargetFileProvider provider = new TargetFileProvider("/path/to/targets.txt");
 * List<String> targets = provider.getTargetList();
 * }</pre>
 *
 * @see ITargetListProvider Configured via ControllerCommandConfig.getTargetListProvider() method.
 */
public class TargetFileProvider implements ITargetListProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private String filename;

    /**
     * Creates a new target file provider for the specified file path.
     *
     * <p>The constructor stores the file path for later use when getTargetList() is called. The
     * file is not validated or accessed during construction, allowing for flexible deployment
     * scenarios where the file may be created after the provider is instantiated.
     *
     * @param filename the path to the target list file to read
     */
    public TargetFileProvider(String filename) {
        this.filename = filename;
    }

    /**
     * Reads and returns the complete list of scan targets from the configured file.
     *
     * <p>This method opens the file, reads all lines, and filters out comments (lines starting with
     * '#') and empty lines. The remaining lines are returned as scan targets in the order they
     * appear in the file.
     *
     * <p><strong>Processing Steps:</strong>
     *
     * <ol>
     *   <li>Open file using Java NIO Files.lines() for stream processing
     *   <li>Filter out comment lines (starting with '#')
     *   <li>Filter out empty lines
     *   <li>Collect remaining lines into a list
     *   <li>Log the number of targets read
     * </ol>
     *
     * <p><strong>File Format Requirements:</strong>
     *
     * <ul>
     *   <li>One target per line in hostname[:port] format
     *   <li>Comment lines start with '#' character
     *   <li>Empty lines are automatically ignored
     *   <li>No additional whitespace trimming is performed
     * </ul>
     *
     * @return a list of target strings read from the file
     * @throws RuntimeException if the file cannot be read (file not found, I/O error, etc.)
     */
    @Override
    public List<String> getTargetList() {
        LOGGER.info("Reading hostName list");
        List<String> targetList;
        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            // remove comments and empty lines
            targetList =
                    lines.filter(line -> !(line.startsWith("#") || line.isEmpty()))
                            .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new RuntimeException("Could not load " + filename, ex);
        }
        LOGGER.info("Read {} hosts", targetList.size());
        return targetList;
    }
}
