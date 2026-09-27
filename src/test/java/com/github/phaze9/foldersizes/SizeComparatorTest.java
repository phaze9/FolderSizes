package com.github.phaze9.foldersizes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SizeComparatorTest {
    @Test
    void sortsLargestSizeFirst() {
        assertTrue(SizeComparator.compareSizes(20L, 10L) < 0);
        assertTrue(SizeComparator.compareSizes(10L, 20L) > 0);
        assertEquals(0, SizeComparator.compareSizes(10L, 10L));
    }

    @Test
    void putsUnknownSizesAfterKnownSizes() {
        assertTrue(SizeComparator.compareSizes(10L, null) < 0);
        assertTrue(SizeComparator.compareSizes(null, 10L) > 0);
        assertEquals(0, SizeComparator.compareSizes(null, null));
    }
}
