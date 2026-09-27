package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class ModuleContentRoots {
    private ModuleContentRoots() {
    }

    @Nullable
    static Collection<VirtualFile> forNodeValue(Project project, Object value) {
        if (value instanceof Module module) {
            return rootsOf(List.of(module));
        }
        if (value instanceof ModuleGroup group) {
            return rootsOf(group.modulesInGroup(project, true));
        }
        return null;
    }

    private static Collection<VirtualFile> rootsOf(Collection<Module> modules) {
        List<VirtualFile> roots = new ArrayList<>();
        for (Module module : modules) {
            if (!module.isDisposed()) {
                roots.addAll(List.of(ModuleRootManager.getInstance(module).getContentRoots()));
            }
        }
        return roots;
    }
}
