package org.passerbya;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginsLoader {

    private final List<PluginManifest> loadedPlugins = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void loadPlugins() {
        // 1. 指向插件目录
        File pluginsDir = new File("plugins");

        // 2. 目录不存在就创建并返回
        if (!pluginsDir.exists()) {
            pluginsDir.mkdir();
            System.out.println("📁 Plugin directory created");
            return;
        }

        // 3. 列出所有子目录
        File[] folders = pluginsDir.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            System.out.println("📭 No plugin folders found");
            return;
        }

        // 4. 遍历每个子目录
        for (File folder : folders) {
            // 每个子目录里必须有一个 manifest.json
            File manifestFile = new File(folder, "manifest.json");

            if (!manifestFile.exists()) {
                System.out.println("⚠️ Skipping " + folder.getName() + " — missing manifest.json");
                continue;
            }

            try {
                PluginManifest manifest = mapper.readValue(manifestFile, PluginManifest.class);
                loadedPlugins.add(manifest);
                System.out.println("✅ Loaded plugin: " + manifest.getName() + " (id: " + manifest.getId() + ")");

            } catch (IOException e) {
                System.out.println("❌ Failed to load " + manifestFile.getName() + " — " + e.getMessage());
            }
        }

        System.out.println("📦 Total plugins loaded: " + loadedPlugins.size());
    }

    public List<PluginManifest> getLoadedPlugins() {
        return loadedPlugins;
    }
}