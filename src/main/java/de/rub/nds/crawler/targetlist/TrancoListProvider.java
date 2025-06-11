/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2022 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.targetlist;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tranco ranking target list provider for research-grade TLS scanning operations.
 *
 * <p>The TrancoListProvider downloads and processes the most recent Tranco ranking data to extract
 * popular website targets for TLS security scanning. Tranco provides a research-oriented
 * alternative to commercial rankings, designed specifically for security and privacy studies.
 *
 * <p>Key advantages:
 *
 * <ul>
 *   <li><strong>Research Focus</strong> - Designed for academic and security research
 *   <li><strong>Stable Rankings</strong> - Aggregates multiple sources for stability
 *   <li><strong>Manipulation Resistant</strong> - Protected against gaming and artificial inflation
 *   <li><strong>Regular Updates</strong> - Daily updated rankings reflecting current web usage
 * </ul>
 *
 * <p><strong>Data Source:</strong> Downloads the top 1 million domain ranking from tranco-list.eu,
 * which aggregates data from multiple sources including Alexa, Umbrella, Majestic, and Quantcast to
 * provide robust and manipulation-resistant rankings.
 *
 * <p><strong>Processing Characteristics:</strong>
 *
 * <ul>
 *   <li><strong>Simple Format</strong> - CSV format with rank,domain structure
 *   <li><strong>Direct Extraction</strong> - Domains are ready for scanning without preprocessing
 *   <li><strong>Configurable Limit</strong> - Supports any number up to 1 million targets
 *   <li><strong>Sequential Order</strong> - Maintains ranking order for top-N selection
 * </ul>
 *
 * <p><strong>Usage Scenarios:</strong>
 *
 * <ul>
 *   <li><strong>Academic Research</strong> - Security studies requiring stable rankings
 *   <li><strong>TLS Surveys</strong> - Large-scale protocol analysis and evaluation
 *   <li><strong>Vulnerability Research</strong> - Scanning popular sites for security issues
 *   <li><strong>Performance Studies</strong> - Protocol performance across diverse targets
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * TrancoListProvider provider = new TrancoListProvider(10000);
 * List<String> targets = provider.getTargetList();
 * // Returns top 10,000 domains from current Tranco ranking
 * }</pre>
 *
 * @see ZipFileProvider
 * @see ITargetListProvider
 * @see <a href="https://tranco-list.eu/">Tranco Ranking Project</a>
 */
public class TrancoListProvider extends ZipFileProvider {

    private static final String SOURCE = "https://tranco-list.eu/top-1m.csv.zip";
    private static final String ZIP_FILENAME = "tranco-1m.csv.zip";
    private static final String FILENAME = "tranco-1m.csv";

    /**
     * Creates a new Tranco list provider for the specified number of top-ranked domains.
     *
     * <p>The constructor configures the provider to download the current Tranco top 1 million
     * ranking and extract the specified number of highest-ranked domains for scanning.
     *
     * @param number the maximum number of domains to extract from the ranking (1 to 1,000,000)
     */
    public TrancoListProvider(int number) {
        super(number, SOURCE, ZIP_FILENAME, FILENAME, "Tranco");
    }

    @Override
    protected List<String> getTargetListFromLines(Stream<String> lines) {
        return lines.limit(this.number).collect(Collectors.toList());
    }
}
