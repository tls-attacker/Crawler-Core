/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.constant;

/**
 * Enumeration of supported Chrome UX Report (CrUX) target list sizes for distributed TLS scanning.
 *
 * <p>The CruxListNumber enum defines predefined target list sizes available from the Chrome User
 * Experience Report dataset. These lists contain popular websites ranked by real user traffic
 * patterns, providing realistic target sets for TLS security evaluations.
 *
 * <p>Key characteristics:
 *
 * <ul>
 *   <li><strong>Real User Data</strong> - Based on actual Chrome browser usage statistics
 *   <li><strong>Multiple Scales</strong> - Supports different scanning scopes from 1K to 1M targets
 *   <li><strong>Performance Tiered</strong> - Larger lists provide broader coverage but require
 *       more resources
 *   <li><strong>Regular Updates</strong> - CrUX data is updated regularly to reflect current web
 *       usage
 * </ul>
 *
 * <p><strong>List Sizes:</strong>
 *
 * <ul>
 *   <li><strong>TOP_1k</strong> - Top 1,000 most popular websites for quick scans
 *   <li><strong>TOP_5K</strong> - Top 5,000 websites for balanced coverage and performance
 *   <li><strong>TOP_10K</strong> - Top 10,000 websites for comprehensive small-scale scanning
 *   <li><strong>TOP_50K</strong> - Top 50,000 websites for extensive scanning projects
 *   <li><strong>TOP_100K</strong> - Top 100,000 websites for large-scale research
 *   <li><strong>TOP_500k</strong> - Top 500,000 websites for comprehensive coverage
 *   <li><strong>TOP_1M</strong> - Top 1,000,000 websites for maximum coverage studies
 * </ul>
 *
 * <p><strong>Selection Guidelines:</strong>
 *
 * <ul>
 *   <li><strong>Development/Testing</strong> - Use TOP_1k or TOP_5K for quick validation
 *   <li><strong>Security Research</strong> - TOP_10K to TOP_100K provides good statistical
 *       significance
 *   <li><strong>Academic Studies</strong> - TOP_500k to TOP_1M for comprehensive coverage
 *   <li><strong>Performance Constraints</strong> - Smaller lists reduce scan time and resource
 *       usage
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 *
 * <pre>{@code
 * CruxListProvider provider = new CruxListProvider(CruxListNumber.TOP_10K);
 * List<String> targets = provider.getTargetList();
 * }</pre>
 *
 * Used by CruxListProvider to configure target list sizes. Part of the ITargetListProvider system
 * for scan target management.
 */
public enum CruxListNumber {
    /** Top 1,000 most popular websites from Chrome UX Report data. */
    TOP_1k(1000),
    /** Top 5,000 most popular websites from Chrome UX Report data. */
    TOP_5K(5000),
    /** Top 10,000 most popular websites from Chrome UX Report data. */
    TOP_10K(10000),
    /** Top 50,000 most popular websites from Chrome UX Report data. */
    TOP_50K(50000),
    /** Top 100,000 most popular websites from Chrome UX Report data. */
    TOP_100K(100000),
    /** Top 500,000 most popular websites from Chrome UX Report data. */
    TOP_500k(500000),
    /** Top 1,000,000 most popular websites from Chrome UX Report data. */
    TOP_1M(1000000);

    private final int number;

    CruxListNumber(int number) {
        this.number = number;
    }

    /**
     * Returns the numeric value representing the number of targets in this list size.
     *
     * @return the number of targets (e.g., 1000 for TOP_1k, 10000 for TOP_10K)
     */
    public int getNumber() {
        return number;
    }
}
