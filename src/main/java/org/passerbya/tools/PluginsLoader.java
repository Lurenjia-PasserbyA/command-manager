package org.passerbya.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.passerbya.debug.DebugLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginsLoader {

    private final List<PluginManifest> loadedPlugins = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void loadPlugins() {
        File pluginsDir = new File("plugins");

        if (!pluginsDir.exists()) {
            pluginsDir.mkdir();
            DebugLogger.info("Plugin directory created");
            return;
        }

        File[] folders = pluginsDir.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            DebugLogger.info("No plugin folders found");
            return;
        }

        for (File folder : folders) {
            File manifestFile = new File(folder, "manifest.json");

            if (!manifestFile.exists()) {
                DebugLogger.warn("Skipping " + folder.getName() + " — missing manifest.json");
                continue;
            }

            try {
                PluginManifest manifest = mapper.readValue(manifestFile, PluginManifest.class);
                loadedPlugins.add(manifest);
                DebugLogger.info("Loaded plugin: " + manifest.getName() + " (id: " + manifest.getId() + ")");
            } catch (IOException e) {
                DebugLogger.error("Failed to load " + manifestFile.getName(), e);
            }
        }

        DebugLogger.info("Total plugins loaded: " + loadedPlugins.size());
    }

    public List<PluginManifest> getLoadedPlugins() {
        return loadedPlugins;
    }
}