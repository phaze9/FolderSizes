package com.github.phaze9.foldersizes;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Serializable project-level snapshot of the directory metrics cache. */
public final class DirectoryMetricsState {
    public List<Entry> entries = new ArrayList<>();

    static final class Entry {
        public String path = "";
        public long directoryCount;
        public long fileCount;
        public long totalBytes;
        public boolean complete = true;

        public Entry() {
        }

        Entry(Path path, DirectoryMetrics metrics) {
            this.path = path.toString();
            directoryCount = metrics.directoryCount();
            fileCount = metrics.fileCount();
            totalBytes = metrics.totalBytes();
            complete = metrics.complete();
        }

        @Nullable
        RestoredEntry restore() {
            if (path == null || path.isBlank()) {
                return null;
            }
            try {
                Path restoredPath = Path.of(path).toAbsolutePath().normalize();
                DirectoryMetrics metrics = new DirectoryMetrics(
                        directoryCount, fileCount, totalBytes, complete);
                return new RestoredEntry(restoredPath, metrics);
            } catch (IllegalArgumentException | SecurityException exception) {
                return null;
            }
        }
    }

    record RestoredEntry(Path path, DirectoryMetrics metrics) {
    }
}
