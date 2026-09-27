package com.github.phaze9.foldersizes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectoryMetricsTest {
    @Test
    void combinesCountsSizesAndCompleteness() {
        DirectoryMetrics first = new DirectoryMetrics(2, 3, 100, true);
        DirectoryMetrics second = new DirectoryMetrics(5, 7, 200, false);

        assertEquals(new DirectoryMetrics(7, 10, 300, false), first.plus(second));
    }

    @Test
    void saturatesCombinedValuesAtLongMaximum() {
        DirectoryMetrics nearMaximum = new DirectoryMetrics(
                Long.MAX_VALUE - 1, Long.MAX_VALUE - 2, Long.MAX_VALUE - 3, true);
        DirectoryMetrics additional = new DirectoryMetrics(2, 3, 4, true);

        assertEquals(
                new DirectoryMetrics(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, true),
                nearMaximum.plus(additional));
    }
}
