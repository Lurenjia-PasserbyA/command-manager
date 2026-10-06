package org.passerbya.core;

import org.passerbya.debug.DebugLogger;
import org.passerbya.tools.PluginManifest;
import org.passerbya.tools.PluginsLoader;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PluginManager {

    private List<PluginManifest> plugins = Collections.emptyList();

    /** 插件根目录 cmmgr/plugins/。位置由 {@link AppPaths} 统一定义。 */
    public static File pluginsDirectory() {
        return AppPaths.pluginsDirectory();
    }

    /** 重新扫描插件目录，替换当前列表。 */
    public void load() {
        PluginsLoader loader = new PluginsLoader();
        loader.loadPlugins();
        this.plugins = loader.getLoadedPlugins();
        DebugLogger.info("PluginManager loaded " + plugins.size() + " plugins");
    }

    public List<PluginManifest> getPlugins() {
        return plugins;
    }

    /**
     * 从当前列表中移除一个插件。
     *
     * <p>只影响内存中的列表，不会去删磁盘上的插件目录 —— 删除文件是破坏性操作，
     * 交给人自己在文件管理器里做。
     *
     * @return 是否真的移除了
     */
    public boolean removePlugin(PluginManifest plugin) {
        if (plugin == null) return false;
        boolean removed = plugins.removeIf(p -> Objects.equals(p.getId(), plugin.getId()));
        if (removed) {
            DebugLogger.info("Plugin removed from list: " + plugin.getId());
        }
        return removed;
    }
}
