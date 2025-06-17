/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import de.rub.nds.crawler.constant.CruxListNumber;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Chrome UX Report (CrUX) target list provider for distributed TLS scanning operations.
 *
 * <p>The CruxListProvider downloads and processes the most recent Chrome User Experience Report
 * data to extract popular website targets for TLS security scanning. It provides access to
 * real-world web traffic patterns based on actual Chrome browser usage statistics.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Real User Data</strong> - Based on actual Chrome browser navigation patterns
 *   <li><strong>Current Rankings</strong> - Downloads the most recent CrUX data available
 *   <li><strong>Configurable Size</strong> - Supports various list sizes from 1K to 1M targets
 *   <li><strong>HTTPS Focus</strong> - Filters for HTTPS-enabled websites only
 * </ul>
 *
 * <p><strong>Data Source:</strong> The provider downloads compressed CSV data from the official
 * CrUX Top Lists repository maintained by zakird on GitHub. This data is updated regularly to
 * reflect current web usage patterns.
 *
 * <p><strong>Processing Pipeline:</strong>
 *
 * <ol>
 *   <li><strong>Download</strong> - Fetch current.csv.gz from GitHub repository
 *   <li><strong>Extract</strong> - Decompress GZIP data to CSV format
 *   <li><strong>Filter</strong> - Select only HTTPS websites within rank threshold
 *   <li><strong>Transform</strong> - Extract hostnames by removing protocol prefixes
 * </ol>
 *
 * <p><strong>CSV Format:</strong> Each line contains "protocol://domain, crux_rank" where the rank
 * indicates popularity based on Chrome usage statistics.
 *
 * <p><strong>Target Selection:</strong> Only HTTPS websites with ranks &lt;= configured number are
 * included, ensuring TLS-capable targets for security scanning.
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_10K);
 * List<String> targets = provider.getTargetList();
 * // Returns up to 10,000 popular HTTPS-enabled hostnames
 * }</pre>
 *
 * @see ZipFileProvider
 * @see CruxListNumber
 * @see ITargetListProvider
 */
public class CruxListProvider extends ZipFileProvider {

    private static final String SOURCE =
            "https://raw.githubusercontent.com/zakird/crux-top-lists/main/data/global/current.csv.gz";
    private static final String ZIP_FILENAME = "current.csv.gz";
    private static final String FILENAME = "current.csv";

    /**
     * Creates a new CrUX list provider for the specified target list size.
     *
     * <p>The constructor configures the provider to download and process the current CrUX data,
     * extracting up to the specified number of top-ranked HTTPS websites for TLS scanning
     * operations.
     *
     * @param cruxListNumber the desired list size determining maximum number of targets
     */
    public CruxListProvider(CruxListNumber cruxListNumber) {
        super(cruxListNumber.getNumber(), SOURCE, ZIP_FILENAME, FILENAME, "Crux");
    }

    @Override
    protected List<String> getTargetListFromLines(Stream<String> lines) {
        // Line format is <protocol>://<domain>, <crux rank>
        // filter...
        return
        // ... ignore all none http
        lines.filter(line -> line.contains("https://"))
                // ... limit to names with correct crux rank
                .filter(line -> Integer.parseInt(line.split(",")[1]) <= number)
                // ... ignore crux rank and protocol
                .map(line -> line.split(",")[0].split("://")[1])
                .collect(Collectors.toList());
    }
}
