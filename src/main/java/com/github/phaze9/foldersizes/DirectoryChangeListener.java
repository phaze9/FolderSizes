package com.github.phaze9.foldersizes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DirectoryChangeListener implements BulkFileListener {
    private static final DirectoryMetrics EMPTY_DIRECTORY = new DirectoryMetrics(0, 0, 0, true);
    private static final DirectoryMetrics DIRECTORY_ENTRY = new DirectoryMetrics(1, 0, 0, true);

    private final DirectoryMetricsService service;
    private final Map<VFileEvent, KnownChange> changesBeforeEvent = new IdentityHashMap<>();

    public DirectoryChangeListener(Project project) {
        service = DirectoryMetricsService.getInstance(project);
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            if (event instanceof VFileDeleteEvent delete) {
                remember(event, delete.getFile());
            } else if (event instanceof VFileMoveEvent move) {
                remember(event, move.getFile());
            } else if (event instanceof VFilePropertyChangeEvent property && property.isRename()) {
                remember(event, property.getFile());
            }
        }
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        Set<Path> fallback = new HashSet<>();
        try {
            for (VFileEvent event : events) {
                if (!applyKnownChange(event)) {
                    addAffectedPaths(fallback, event);
                }
            }
        } finally {
            for (VFileEvent event : events) {
                changesBeforeEvent.remove(event);
            }
        }
        service.pathsChanged(fallback);
    }

    private boolean applyKnownChange(VFileEvent event) {
        Path eventPath = path(event.getPath());
        if (eventPath == null) {
            return false;
        }

        if (event instanceof VFileContentChangeEvent content
                && content.isLengthAndTimestampDiffProvided()) {
            service.fileLengthChanged(eventPath, content.getOldLength(), content.getNewLength());
            return true;
        }

        if (event instanceof VFileCreateEvent create) {
            VirtualFile created = create.getFile();
            if (created == null || created.is(VFileProperty.SYMLINK)
                    || created.is(VFileProperty.SPECIAL)) {
                return false;
            }
            if (!create.isDirectory()) {
                service.pathAdded(eventPath,
                        new DirectoryMetrics(0, 1, created.getLength(), true), null);
                return true;
            }
            if (create.isEmptyDirectory()) {
                service.pathAdded(eventPath, DIRECTORY_ENTRY, EMPTY_DIRECTORY);
                return true;
            }
            return false;
        }

        KnownChange known = changesBeforeEvent.get(event);
        if (known == null) {
            return false;
        }
        if (event instanceof VFileDeleteEvent) {
            service.pathRemoved(known.oldPath(), known.contribution());
            return true;
        }
        if (event instanceof VFileMoveEvent move) {
            Path newPath = path(move.getNewPath());
            if (newPath == null) {
                return false;
            }
            service.pathMoved(known.oldPath(), newPath, known.contribution());
            return true;
        }
        if (event instanceof VFilePropertyChangeEvent property && property.isRename()) {
            Path newPath = path(property.getNewPath());
            if (newPath == null) {
                return false;
            }
            service.pathMoved(known.oldPath(), newPath, known.contribution());
            return true;
        }
        return false;
    }

    private void remember(VFileEvent event, VirtualFile file) {
        Path oldPath = localPath(file);
        DirectoryMetrics contribution = service.knownContribution(file);
        if (oldPath != null && contribution != null) {
            changesBeforeEvent.put(event, new KnownChange(oldPath, contribution));
        }
    }

    private static void addAffectedPaths(Set<Path> paths, VFileEvent event) {
        addPath(paths, event.getPath());
        if (event instanceof VFileMoveEvent move) {
            addPath(paths, move.getOldPath());
            addPath(paths, move.getNewPath());
        } else if (event instanceof VFilePropertyChangeEvent property && property.isRename()) {
            addPath(paths, property.getOldPath());
            addPath(paths, property.getNewPath());
        }
    }

    @Nullable
    private static Path localPath(VirtualFile file) {
        if (!file.isValid() || !file.isInLocalFileSystem()) {
            return null;
        }
        try {
            return file.toNioPath().toAbsolutePath().normalize();
        } catch (UnsupportedOperationException | IllegalArgumentException | SecurityException exception) {
            return null;
        }
    }

    private static void addPath(Set<Path> paths, String value) {
        Path path = path(value);
        if (path != null) {
            paths.add(path);
        }
    }

    @Nullable
    private static Path path(String value) {
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (InvalidPathException | SecurityException exception) {
            return null;
        }
    }

    private record KnownChange(Path oldPath, DirectoryMetrics contribution) {
    }
}
