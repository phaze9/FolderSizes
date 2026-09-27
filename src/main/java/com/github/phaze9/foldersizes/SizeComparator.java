package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.GroupByTypeComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

final class SizeComparator extends GroupByTypeComparator {
    private final DirectoryMetricsService service;

    SizeComparator(Project project, String paneId, DirectoryMetricsService service) {
        super(project, paneId);
        this.service = service;
    }

    @Override
    public int compare(NodeDescriptor firstDescriptor, NodeDescriptor secondDescriptor) {
        if (firstDescriptor instanceof ProjectViewNode<?> first
                && secondDescriptor instanceof ProjectViewNode<?> second) {
            VirtualFile firstFile = first.getVirtualFile();
            VirtualFile secondFile = second.getVirtualFile();
            if (firstFile != null && secondFile != null) {
                if (isFoldersAlwaysOnTop() && firstFile.isDirectory() != secondFile.isDirectory()) {
                    return firstFile.isDirectory() ? -1 : 1;
                }
            }

            if (isSizeable(first) && isSizeable(second)) {
                int sizeComparison = compareSizes(sizeOf(first), sizeOf(second));
                if (sizeComparison != 0) {
                    return sizeComparison;
                }
            }
        }

        // Names provide a stable tie-breaker and preserve IntelliJ's ordering
        // rules for modules, libraries, packages, and other special nodes.
        return super.compare(firstDescriptor, secondDescriptor);
    }

    private boolean isSizeable(ProjectViewNode<?> node) {
        return node.getValue() instanceof Module || node.getVirtualFile() != null;
    }

    @Nullable
    private Long sizeOf(ProjectViewNode<?> node) {
        if (node.getValue() instanceof Module module) {
            if (module.isDisposed()) {
                return null;
            }
            return service.getCombinedSizeOrSchedule(
                    java.util.List.of(ModuleRootManager.getInstance(module).getContentRoots()));
        }

        VirtualFile file = node.getVirtualFile();
        return file == null ? null : service.getSizeOrSchedule(file);
    }

    static int compareSizes(@Nullable Long firstSize, @Nullable Long secondSize) {
        if (firstSize == null) {
            return secondSize == null ? 0 : 1;
        }
        if (secondSize == null) {
            return -1;
        }
        return Long.compare(secondSize, firstSize);
    }
}
