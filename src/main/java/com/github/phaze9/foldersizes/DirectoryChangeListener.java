package com.github.phaze9.foldersizes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DirectoryChangeListener implements BulkFileListener {
    private final DirectoryMetricsService service;

    public DirectoryChangeListener(Project project) {
        service = DirectoryMetricsService.getInstance(project);
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        Set<Path> changed = new HashSet<>();
        for (VFileEvent event : events) {
            addPath(changed, event.getPath());

            if (event instanceof VFileMoveEvent move) {
                addPath(changed, move.getOldParent().getPath() + "/" + move.getFile().getName());
                addPath(changed, move.getNewParent().getPath() + "/" + move.getFile().getName());
            } else if (event instanceof VFilePropertyChangeEvent property
                    && "name".equals(property.getPropertyName())
                    && property.getFile().getParent() != null) {
                addPath(changed, property.getFile().getParent().getPath() + "/" + property.getOldValue());
                addPath(changed, property.getFile().getParent().getPath() + "/" + property.getNewValue());
            }
        }
        service.pathsChanged(changed);
    }

    private static void addPath(Set<Path> paths, String value) {
        try {
            paths.add(Path.of(value));
        } catch (InvalidPathException ignored) {
            // Non-local VFS paths are not used by the directory scanner.
        }
    }
}
