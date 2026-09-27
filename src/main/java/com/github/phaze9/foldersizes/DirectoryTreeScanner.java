package com.github.phaze9.foldersizes;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;

final class DirectoryTreeScanner {
    interface ResultConsumer {
        void accept(Path directory, DirectoryMetrics metrics);
    }

    private DirectoryTreeScanner() {
    }

    static DirectoryMetrics scan(Path root, ResultConsumer consumer) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IOException("Not a directory: " + normalizedRoot);
        }

        Deque<Accumulator> stack = new ArrayDeque<>();
        DirectoryMetrics[] rootResult = new DirectoryMetrics[1];

        Files.walkFileTree(normalizedRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                stack.push(new Accumulator(directory));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                Accumulator current = stack.peek();
                if (current != null) {
                    current.files++;
                    current.bytes = saturatedAdd(current.bytes, attributes.size());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException error) {
                Accumulator current = stack.peek();
                if (current != null) {
                    current.complete = false;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException error) {
                Accumulator finished = stack.pop();
                if (error != null) {
                    finished.complete = false;
                }

                DirectoryMetrics metrics = new DirectoryMetrics(
                        finished.directories, finished.files, finished.bytes, finished.complete);
                consumer.accept(finished.path, metrics);

                Accumulator parent = stack.peek();
                if (parent == null) {
                    rootResult[0] = metrics;
                } else {
                    parent.directories = saturatedAdd(parent.directories, saturatedAdd(1, metrics.directoryCount()));
                    parent.files = saturatedAdd(parent.files, metrics.fileCount());
                    parent.bytes = saturatedAdd(parent.bytes, metrics.totalBytes());
                    parent.complete &= metrics.complete();
                }
                return FileVisitResult.CONTINUE;
            }
        });

        if (rootResult[0] == null) {
            throw new IOException("Unable to scan directory: " + normalizedRoot);
        }
        return rootResult[0];
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static final class Accumulator {
        private final Path path;
        private long directories;
        private long files;
        private long bytes;
        private boolean complete = true;

        private Accumulator(Path path) {
            this.path = path;
        }
    }
}
