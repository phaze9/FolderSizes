package com.github.phaze9.foldersizes;

record DirectoryMetrics(long directoryCount, long fileCount, long totalBytes, boolean complete) {
    DirectoryMetrics {
        if (directoryCount < 0 || fileCount < 0 || totalBytes < 0) {
            throw new IllegalArgumentException("Directory metrics cannot be negative");
        }
    }

    DirectoryMetrics plus(DirectoryMetrics other) {
        return new DirectoryMetrics(
                saturatedAdd(directoryCount, other.directoryCount),
                saturatedAdd(fileCount, other.fileCount),
                saturatedAdd(totalBytes, other.totalBytes),
                complete && other.complete);
    }

    /**
     * Applies an exact VFS delta. A {@code null} result means that the cached
     * value cannot be updated safely and must be recalculated instead.
     */
    DirectoryMetrics adjustedBy(long directoryDelta, long fileDelta, long byteDelta) {
        try {
            long adjustedDirectories = adjust(directoryCount, directoryDelta);
            long adjustedFiles = adjust(fileCount, fileDelta);
            long adjustedBytes = adjust(totalBytes, byteDelta);
            return new DirectoryMetrics(adjustedDirectories, adjustedFiles, adjustedBytes, complete);
        } catch (ArithmeticException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private static long adjust(long value, long delta) {
        // A saturated value no longer contains enough information to subtract
        // from it accurately.
        if (value == Long.MAX_VALUE && delta != 0) {
            throw new ArithmeticException("Cannot adjust a saturated metric");
        }
        long adjusted = Math.addExact(value, delta);
        if (adjusted < 0) {
            throw new IllegalArgumentException("Metric delta produced a negative value");
        }
        return adjusted;
    }

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
