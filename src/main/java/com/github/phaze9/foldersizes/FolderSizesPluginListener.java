package com.github.phaze9.foldersizes;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Enables File Details on activation and remembers when the plugin is disabled. */
public final class FolderSizesPluginListener implements AppLifecycleListener, DynamicPluginListener {
    private static final String PLUGIN_ID = "com.github.phaze9.foldersizes";
    private static final String PLUGIN_ACTIVE_KEY = PLUGIN_ID + ".pluginActive";

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        activate();
    }

    @Override
    public void pluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
        if (isThisPlugin(pluginDescriptor)) {
            activate();
        }
    }

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (isThisPlugin(pluginDescriptor)) {
            PropertiesComponent.getInstance().setValue(PLUGIN_ACTIVE_KEY, false);
        }
    }

    private static void activate() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        if (!properties.getBoolean(PLUGIN_ACTIVE_KEY, false)) {
            UISettings settings = UISettings.getInstance();
            if (!settings.getShowInplaceComments()) {
                settings.setShowInplaceComments(true);
                settings.fireUISettingsChanged();
            }
            properties.setValue(PLUGIN_ACTIVE_KEY, true);
        }
    }

    private static boolean isThisPlugin(IdeaPluginDescriptor pluginDescriptor) {
        return PLUGIN_ID.equals(pluginDescriptor.getPluginId().getIdString());
    }
}
