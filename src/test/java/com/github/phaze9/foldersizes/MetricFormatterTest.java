package com.github.phaze9.foldersizes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricFormatterTest {
    @Test
    void formatsCountsAndDecimalSizes() {
        assertEquals("1 dir, 2 files, 1.50 KB", MetricFormatter.format(
                new DirectoryMetrics(1, 2, 1500, true)));
        assertEquals("0 dirs, 1 file, 999 B, partial", MetricFormatter.format(
                new DirectoryMetrics(0, 1, 999, false)));
    }
}
