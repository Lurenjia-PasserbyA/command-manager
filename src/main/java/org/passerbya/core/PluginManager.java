package org.passerbya.core;

import org.passerbya.debug.DebugLogger;
import org.passerbya.tools.PluginManifest;
import org.passerbya.tools.PluginsLoader;

import java.util.Collections;
import java.util.List;

public class PluginManager {

    private List<PluginManifest> plugins = Collections.emptyList();

    public void load() {
        PluginsLoader loader = new PluginsLoader();
        loader.loadPlugins();
        this.plugins = loader.getLoadedPlugins();
        DebugLogger.info("PluginManager loaded " + plugins.size() + " plugins");
    }

    public List<PluginManifest> getPlugins() {
        return plugins;
    }
}