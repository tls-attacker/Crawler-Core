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
 * Enumeration of CrUX (Chrome User Experience Report) list sizes.
 *
 * <p>This enum defines the different sizes of top website lists available from CrUX, ranging from
 * the top 1,000 to the top 1 million most popular websites.
 */
public enum CruxListNumber {
    TOP_1k(1000),
    TOP_5K(5000),
    TOP_10K(10000),
    TOP_50K(50000),
    TOP_100K(100000),
    TOP_500k(500000),
    TOP_1M(1000000);

    private final int number;

    /**
     * Creates a CruxListNumber with the specified numeric value.
     *
     * @param number the numeric value representing the list size
     */
    CruxListNumber(int number) {
        this.number = number;
    }

    /**
     * Gets the numeric value of this CrUX list size.
     *
     * @return the number of websites in this CrUX list
     */
    public int getNumber() {
        return number;
    }
}
