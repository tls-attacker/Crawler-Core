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
 * Predefined constants for commonly used CrUX (Chrome User Experience Report) list sizes. These
 * represent the top N domains from the CrUX dataset, which provides real-world usage statistics
 * from Chrome browsers.
 */
public enum CruxListNumber {
    /** Top 1,000 domains from CrUX dataset. */
    TOP_1k(1000),
    /** Top 5,000 domains from CrUX dataset. */
    TOP_5K(5000),
    /** Top 10,000 domains from CrUX dataset. */
    TOP_10K(10000),
    /** Top 50,000 domains from CrUX dataset. */
    TOP_50K(50000),
    /** Top 100,000 domains from CrUX dataset. */
    TOP_100K(100000),
    /** Top 500,000 domains from CrUX dataset. */
    TOP_500k(500000),
    /** Top 1,000,000 domains from CrUX dataset. */
    TOP_1M(1000000);

    private final int number;

    CruxListNumber(int number) {
        this.number = number;
    }

    /**
     * Gets the numeric value of this CrUX list size.
     *
     * @return the number of domains this constant represents
     */
    public int getNumber() {
        return number;
    }
}
