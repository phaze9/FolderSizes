package com.github.phaze9.foldersizes;

record DirectoryMetrics(long directoryCount, long fileCount, long totalBytes, boolean complete) {
    DirectoryMetrics {
        if (directoryCount < 0 || fileCount < 0 || totalBytes < 0) {
            throw new IllegalArgumentException("Directory metrics cannot be negative");
        }
    }
}
