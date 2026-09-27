package com.github.phaze9.foldersizes;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void appliesExactPositiveAndNegativeDeltas() {
        DirectoryMetrics metrics = new DirectoryMetrics(4, 8, 1_000, true);

        assertEquals(
                new DirectoryMetrics(3, 10, 1_250, true),
                metrics.adjustedBy(-1, 2, 250));
    }

    @Test
    void rejectsUnderflowOverflowAndAdjustmentsToSaturatedValues() {
        DirectoryMetrics metrics = new DirectoryMetrics(0, 2, 10, true);

        assertNull(metrics.adjustedBy(-1, 0, 0));
        assertNull(metrics.adjustedBy(0, 0, Long.MAX_VALUE));
        assertNull(new DirectoryMetrics(1, 1, Long.MAX_VALUE, true).adjustedBy(0, 0, -1));
    }

    @Test
    void removesContentRootsAlreadyCoveredByAnAncestor() {
        Path root = Path.of("/project");

        assertEquals(
                Set.of(root, Path.of("/separate")),
                DirectoryMetricsService.withoutDescendants(List.of(
                        root.resolve("child"),
                        Path.of("/separate"),
                        root,
                        root.resolve("child/grandchild"))));
    }
}
