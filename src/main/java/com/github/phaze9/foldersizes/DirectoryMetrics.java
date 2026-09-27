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

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
