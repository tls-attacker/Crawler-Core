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
 * Target list provider that reads target hostnames from a local file. The file can contain comments
 * (lines starting with #) and empty lines which are ignored.
 */
public class TargetFileProvider implements ITargetListProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private String filename;

    /**
     * Constructs a new TargetFileProvider for the specified file.
     *
     * @param filename The path to the file containing target hostnames
     */
    public TargetFileProvider(String filename) {
        this.filename = filename;
    }

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
