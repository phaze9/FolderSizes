package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

public final class DirectoryMetricsDecorator implements ProjectViewNodeDecorator {
    private final DirectoryMetricsService service;

    public DirectoryMetricsDecorator(Project project) {
        service = DirectoryMetricsService.getInstance(project);
    }

    @Override
    public void decorate(@NotNull ProjectViewNode<?> node, @NotNull PresentationData data) {
        VirtualFile file = node.getVirtualFile();
        if (file == null || !file.isDirectory()) {
            return;
        }

        DirectoryMetrics metrics = service.getOrSchedule(file);
        if (metrics == null) {
            data.addText("  calculating…", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
        } else {
            data.addText("  " + MetricFormatter.format(metrics), SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
            if (!metrics.complete()) {
                String existing = data.getTooltip();
                String warning = "Folder metrics are partial because some entries could not be read.";
                data.setTooltip(existing == null || existing.isBlank() ? warning : existing + "\n" + warning);
            }
        }
    }
}
