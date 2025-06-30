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

public class TargetFileProvider implements ITargetListProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private String filename;

    public TargetFileProvider(String filename) {
        this.filename = filename;
    }

    @Override
    public List<String> getTargetList() {
        LOGGER.info("Reading hostName list"); // $NON-NLS-1$
        List<String> targetList;
        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            // remove comments and empty lines
            targetList =
                    lines.filter(line -> !(line.startsWith("#") || line.isEmpty())) // $NON-NLS-1$
                            .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new RuntimeException("Could not load " + filename, ex); // $NON-NLS-1$
        }
        LOGGER.info("Read {} hosts", targetList.size()); // $NON-NLS-1$
        return targetList;
    }
}
