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
            appendSuffix(data, "  calculating…");
        } else {
            appendSuffix(data, "  " + MetricFormatter.format(metrics));
            if (!metrics.complete()) {
                String existing = data.getTooltip();
                String warning = "Folder metrics are partial because some entries could not be read.";
                data.setTooltip(existing == null || existing.isBlank() ? warning : existing + "\n" + warning);
            }
        }
    }

    static void appendSuffix(PresentationData data, String suffix) {
        if (data.getColoredText().isEmpty()) {
            String name = data.getPresentableText();
            if (name != null) {
                data.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
        data.addText(suffix, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES);
    }
}
