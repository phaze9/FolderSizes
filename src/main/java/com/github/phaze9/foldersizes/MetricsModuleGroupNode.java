package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleGroupNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

final class MetricsModuleGroupNode extends ProjectViewModuleGroupNode {
    private final DirectoryMetricsService service;

    MetricsModuleGroupNode(
            Project project,
            ModuleGroup group,
            ViewSettings settings,
            DirectoryMetricsService service) {
        super(project, group, settings);
        this.service = service;
    }

    @Override
    public void update(@NotNull PresentationData data) {
        super.update(data);
        Collection<VirtualFile> roots = ModuleContentRoots.forNodeValue(getProject(), getValue());
        if (roots != null) {
            DirectoryMetricsDecorator.decorateRoots(service, roots, data);
        }
    }
}
