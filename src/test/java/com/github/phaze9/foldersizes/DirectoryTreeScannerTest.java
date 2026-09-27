package com.github.phaze9.foldersizes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryTreeScannerTest {
    @TempDir
    Path tempDirectory;

    @Test
    void calculatesRecursiveMetricsAndPublishesSubtrees() throws Exception {
        Path nested = Files.createDirectories(tempDirectory.resolve("one/two"));
        Files.write(tempDirectory.resolve("root.bin"), new byte[7]);
        Files.write(tempDirectory.resolve("one/child.bin"), new byte[11]);
        Files.write(nested.resolve("nested.bin"), new byte[13]);

        Map<Path, DirectoryMetrics> results = new HashMap<>();
        DirectoryMetrics root = DirectoryTreeScanner.scan(tempDirectory, results::put);

        assertEquals(new DirectoryMetrics(2, 3, 31, true), root);
        assertEquals(new DirectoryMetrics(1, 2, 24, true), results.get(tempDirectory.resolve("one")));
        assertEquals(new DirectoryMetrics(0, 1, 13, true), results.get(nested));
    }

    @Test
    void doesNotFollowDirectorySymbolicLinks() throws Exception {
        Path real = Files.createDirectories(tempDirectory.resolve("real"));
        Files.write(real.resolve("data.bin"), new byte[5]);
        Path link = tempDirectory.resolve("link");
        try {
            Files.createSymbolicLink(link, real);
        } catch (UnsupportedOperationException exception) {
            return;
        }

        DirectoryMetrics root = DirectoryTreeScanner.scan(tempDirectory, (path, metrics) -> { });

        assertEquals(1, root.directoryCount());
        assertEquals(2, root.fileCount());
        assertTrue(root.totalBytes() >= 5);
    }
}
