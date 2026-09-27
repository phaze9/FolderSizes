package com.github.phaze9.foldersizes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.SimpleTextAttributes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DirectoryMetricsDecoratorTest {
    @Test
    void keepsPresentableNameWhenAddingSuffix() {
        PresentationData data = new PresentationData();
        data.setPresentableText("src");

        DirectoryMetricsDecorator.appendSuffix(data, "  12 dirs, 84 files, 1.27 MB");

        assertEquals(2, data.getColoredText().size());
        assertEquals("src", data.getColoredText().get(0).getText());
        assertSame(SimpleTextAttributes.REGULAR_ATTRIBUTES,
                data.getColoredText().get(0).getAttributes());
        assertEquals("  12 dirs, 84 files, 1.27 MB",
                data.getColoredText().get(1).getText());
        assertSame(SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES,
                data.getColoredText().get(1).getAttributes());
    }

    @Test
    void doesNotDuplicateAnExistingColoredName() {
        PresentationData data = new PresentationData();
        data.setPresentableText("src");
        data.addText("src", SimpleTextAttributes.REGULAR_ATTRIBUTES);

        DirectoryMetricsDecorator.appendSuffix(data, "  calculating…");

        assertEquals(2, data.getColoredText().size());
        assertEquals("src", data.getColoredText().get(0).getText());
        assertEquals("  calculating…", data.getColoredText().get(1).getText());
    }
}
