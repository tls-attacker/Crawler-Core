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
 * Enumeration of different Crux list sizes available for scanning. Each enum constant represents a
 * specific list of top websites, with the value indicating the number of entries in that list.
 */
public enum CruxListNumber {
    /** Top 1,000 websites */
    TOP_1k(1000),
    /** Top 5,000 websites */
    TOP_5K(5000),
    /** Top 10,000 websites */
    TOP_10K(10000),
    /** Top 50,000 websites */
    TOP_50K(50000),
    /** Top 100,000 websites */
    TOP_100K(100000),
    /** Top 500,000 websites */
    TOP_500k(500000),
    /** Top 1,000,000 websites */
    TOP_1M(1000000);

    private final int number;

    /**
     * Constructor for the enum constants.
     *
     * @param number The number of entries in the list
     */
    CruxListNumber(int number) {
        this.number = number;
    }

    /**
     * Gets the number of entries in this list.
     *
     * @return The number of entries
     */
    public int getNumber() {
        return number;
    }
}
