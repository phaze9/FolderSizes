package com.github.phaze9.foldersizes;

import java.util.Locale;

final class MetricFormatter {
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};

    private MetricFormatter() {
    }

    static String format(DirectoryMetrics metrics) {
        String suffix = metrics.complete() ? "" : ", partial";
        return String.format(Locale.ROOT, "%s, %s, %s%s",
                quantity(metrics.directoryCount(), "dir", "dirs"),
                quantity(metrics.fileCount(), "file", "files"),
                formatBytes(metrics.totalBytes()),
                suffix);
    }

    static String formatBytes(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        double value = bytes;
        int unit = 0;
        while (value >= 1000 && unit < UNITS.length - 1) {
            value /= 1000;
            unit++;
        }
        String pattern = value >= 100 ? "%.0f %s" : value >= 10 ? "%.1f %s" : "%.2f %s";
        return String.format(Locale.ROOT, pattern, value, UNITS[unit]);
    }

    private static String quantity(long value, String singular, String plural) {
        return value + " " + (value == 1 ? singular : plural);
    }
}
