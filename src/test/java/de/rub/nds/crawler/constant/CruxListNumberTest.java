/*
 * TLS-Crawler - A TLS scanning tool to perform large scale scans with the TLS-Scanner
 *
 * Copyright 2018-2023 Ruhr University Bochum, Paderborn University, and Hackmanit GmbH
 *
 * Licensed under Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package de.rub.nds.crawler.constant;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CruxListNumberTest {

    @Test
    void testGetNumber() {
        assertEquals(1000, CruxListNumber.TOP_1k.getNumber());
        assertEquals(5000, CruxListNumber.TOP_5K.getNumber());
        assertEquals(10000, CruxListNumber.TOP_10K.getNumber());
        assertEquals(50000, CruxListNumber.TOP_50K.getNumber());
        assertEquals(100000, CruxListNumber.TOP_100K.getNumber());
        assertEquals(500000, CruxListNumber.TOP_500k.getNumber());
        assertEquals(1000000, CruxListNumber.TOP_1M.getNumber());
    }

    @Test
    void testEnumValues() {
        CruxListNumber[] values = CruxListNumber.values();
        assertEquals(7, values.length);
        assertEquals(CruxListNumber.TOP_1k, values[0]);
        assertEquals(CruxListNumber.TOP_5K, values[1]);
        assertEquals(CruxListNumber.TOP_10K, values[2]);
        assertEquals(CruxListNumber.TOP_50K, values[3]);
        assertEquals(CruxListNumber.TOP_100K, values[4]);
        assertEquals(CruxListNumber.TOP_500k, values[5]);
        assertEquals(CruxListNumber.TOP_1M, values[6]);
    }

    @Test
    void testValueOf() {
        assertEquals(CruxListNumber.TOP_1k, CruxListNumber.valueOf("TOP_1k"));
        assertEquals(CruxListNumber.TOP_5K, CruxListNumber.valueOf("TOP_5K"));
        assertEquals(CruxListNumber.TOP_10K, CruxListNumber.valueOf("TOP_10K"));
        assertEquals(CruxListNumber.TOP_50K, CruxListNumber.valueOf("TOP_50K"));
        assertEquals(CruxListNumber.TOP_100K, CruxListNumber.valueOf("TOP_100K"));
        assertEquals(CruxListNumber.TOP_500k, CruxListNumber.valueOf("TOP_500k"));
        assertEquals(CruxListNumber.TOP_1M, CruxListNumber.valueOf("TOP_1M"));
    }

    @Test
    void testValueOfInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> CruxListNumber.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> CruxListNumber.valueOf(""));
        assertThrows(NullPointerException.class, () -> CruxListNumber.valueOf(null));
    }

    @Test
    void testEnumName() {
        assertEquals("TOP_1k", CruxListNumber.TOP_1k.name());
        assertEquals("TOP_5K", CruxListNumber.TOP_5K.name());
        assertEquals("TOP_10K", CruxListNumber.TOP_10K.name());
        assertEquals("TOP_50K", CruxListNumber.TOP_50K.name());
        assertEquals("TOP_100K", CruxListNumber.TOP_100K.name());
        assertEquals("TOP_500k", CruxListNumber.TOP_500k.name());
        assertEquals("TOP_1M", CruxListNumber.TOP_1M.name());
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, CruxListNumber.TOP_1k.ordinal());
        assertEquals(1, CruxListNumber.TOP_5K.ordinal());
        assertEquals(2, CruxListNumber.TOP_10K.ordinal());
        assertEquals(3, CruxListNumber.TOP_50K.ordinal());
        assertEquals(4, CruxListNumber.TOP_100K.ordinal());
        assertEquals(5, CruxListNumber.TOP_500k.ordinal());
        assertEquals(6, CruxListNumber.TOP_1M.ordinal());
    }

    @Test
    void testEnumToString() {
        assertEquals("TOP_1k", CruxListNumber.TOP_1k.toString());
        assertEquals("TOP_5K", CruxListNumber.TOP_5K.toString());
        assertEquals("TOP_10K", CruxListNumber.TOP_10K.toString());
        assertEquals("TOP_50K", CruxListNumber.TOP_50K.toString());
        assertEquals("TOP_100K", CruxListNumber.TOP_100K.toString());
        assertEquals("TOP_500k", CruxListNumber.TOP_500k.toString());
        assertEquals("TOP_1M", CruxListNumber.TOP_1M.toString());
    }

    @Test
    void testEnumEquality() {
        assertTrue(CruxListNumber.TOP_1k == CruxListNumber.TOP_1k);
        assertTrue(CruxListNumber.TOP_5K == CruxListNumber.TOP_5K);
        assertFalse(CruxListNumber.TOP_1k == CruxListNumber.TOP_5K);
    }

    @Test
    void testNumberValuesArePositive() {
        for (CruxListNumber cruxNumber : CruxListNumber.values()) {
            assertTrue(cruxNumber.getNumber() > 0);
        }
    }

    @Test
    void testNumberValuesAreOrdered() {
        CruxListNumber[] values = CruxListNumber.values();
        for (int i = 1; i < values.length; i++) {
            assertTrue(values[i].getNumber() > values[i - 1].getNumber());
        }
    }
}
