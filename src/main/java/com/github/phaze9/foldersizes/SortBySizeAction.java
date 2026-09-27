package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.NodeSortKey;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class SortBySizeAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        AbstractProjectViewPane pane = currentPane(project);
        return project != null
                && pane != null
                && DirectoryMetricsService.getInstance(project).isSizeSortingEnabled(pane.getId());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean selected) {
        Project project = event.getProject();
        AbstractProjectViewPane pane = currentPane(project);
        if (project == null || pane == null) {
            return;
        }

        ProjectView projectView = ProjectView.getInstance(project);
        DirectoryMetricsService service = DirectoryMetricsService.getInstance(project);
        service.setSizeSortingEnabled(pane.getId(), selected);

        if (selected) {
            projectView.setManualOrder(pane.getId(), false);
            projectView.setSortKey(pane.getId(), NodeSortKey.BY_NAME);
            pane.installComparator(new SizeComparator(project, pane.getId(), service));
        } else {
            pane.installComparator();
        }
        pane.updateFromRoot(false);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        event.getPresentation().setEnabledAndVisible(currentPane(event.getProject()) != null);
    }

    private static AbstractProjectViewPane currentPane(Project project) {
        return project == null ? null : ProjectView.getInstance(project).getCurrentProjectViewPane();
    }
}
