package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleGroupNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ModuleGroupMetricsTreeStructureProvider
        implements TreeStructureProvider, DumbAware {
    private final Project project;
    private final DirectoryMetricsService service;

    public ModuleGroupMetricsTreeStructureProvider(Project project) {
        this.project = project;
        service = DirectoryMetricsService.getInstance(project);
    }

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> modify(
            @NotNull AbstractTreeNode<?> parent,
            @NotNull Collection<AbstractTreeNode<?>> children,
            ViewSettings settings) {
        List<AbstractTreeNode<?>> result = new ArrayList<>(children.size());
        for (AbstractTreeNode<?> child : children) {
            if (child instanceof ProjectViewModuleGroupNode groupNode
                    && !(child instanceof MetricsModuleGroupNode)) {
                ModuleGroup group = groupNode.getValue();
                result.add(group == null
                        ? child
                        : new MetricsModuleGroupNode(project, group, settings, service));
            } else {
                result.add(child);
            }
        }
        return result;
    }
}
