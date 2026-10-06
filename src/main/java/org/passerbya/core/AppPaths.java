package org.passerbya.core;

import org.passerbya.debug.DebugLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * 集中管理运行期数据的位置。
 *
 * <pre>
 * &lt;工作目录&gt;/
 * └── cmmgr/
 *     ├── config/
 *     │   └── config.json
 *     └── plugins/
 *         └── &lt;插件名&gt;/manifest.json
 * </pre>
 *
 * <p>为什么要集中：之前路径散在三个类里（配置、插件加载器、插件管理器各写一份），
 * 改目录结构就得满仓库找，漏一处就是"配置读到了、插件没读到"这类难查的问题。
 *
 * <p>根目录取自系统属性 {@code user.dir}，而不是靠 JVM 对相对路径的隐式解析。
 * 两者在正常运行下等价（{@code user.dir} 就是进程工作目录），但显式取值让迁移
 * 逻辑可以被单元测试指向临时目录 —— {@code new File("x")} 的解析基准是进程真实
 * 工作目录，改 {@code user.dir} 是改不动它的。
 */
public final class AppPaths {

    /** 数据根目录名。 */
    private static final String BASE_DIR = "cmmgr";

    /** 配置子目录名。 */
    private static final String CONFIG_SUBDIR = "config";

    /** 插件子目录名。 */
    private static final String PLUGINS_SUBDIR = "plugins";

    private static final String CONFIG_FILE = "config.json";

    // ---------- 旧布局 ----------
    private static final String LEGACY_CONFIG_DIR = ".command-manager";
    private static final String LEGACY_PLUGINS_DIR = "plugins";

    private AppPaths() {}

    /** 工作目录。 */
    private static File base() {
        String dir = System.getProperty("user.dir");
        return dir == null || dir.isBlank() ? new File(".") : new File(dir);
    }

    /** 数据根目录 cmmgr/。 */
    public static File baseDirectory() {
        return new File(base(), BASE_DIR);
    }

    /** 插件根目录 cmmgr/plugins/。 */
    public static File pluginsDirectory() {
        return new File(baseDirectory(), PLUGINS_SUBDIR);
    }

    /** 设置文件 cmmgr/config/config.json。 */
    public static File configFile() {
        return new File(new File(baseDirectory(), CONFIG_SUBDIR), CONFIG_FILE);
    }

    // ---------- 旧布局迁移 ----------

    /**
     * 把旧布局的数据搬进 cmmgr/。
     *
     * <p>旧布局：
     * <ul>
     *   <li>{@code .command-manager/config.json} → {@code cmmgr/config/config.json}</li>
     *   <li>{@code plugins/} → {@code cmmgr/plugins/}</li>
     * </ul>
     *
     * <p>必须在读配置和加载插件**之前**调用，否则会读到空的新位置，
     * 表现为"升级一次设置和插件全没了"。
     */
    public static void migrateLegacyLayout() {
        File root = base();
        try {
            migratePlugins(new File(root, LEGACY_PLUGINS_DIR), pluginsDirectory());
            migrateConfig(new File(new File(root, LEGACY_CONFIG_DIR), CONFIG_FILE), configFile());
        } catch (RuntimeException e) {
            // 迁移失败不应该阻止程序启动：旧数据还在，只是这次用不上新位置
            DebugLogger.error("Legacy layout migration failed", e);
        }
    }

    private static void migratePlugins(File legacy, File current) {
        if (!legacy.isDirectory()) return;
        if (current.isDirectory() && hasChildren(current)) {
            DebugLogger.info("Plugins already at " + display(current) + ", skipping migration");
            return;
        }
        if (!current.isDirectory() && !current.mkdirs()) {
            DebugLogger.warn("Cannot create " + display(current) + ", keeping legacy plugins dir");
            return;
        }

        boolean clean = true;
        try (Stream<Path> entries = Files.list(legacy.toPath())) {
            for (Path src : entries.toList()) {
                Path dst = current.toPath().resolve(src.getFileName().toString());
                if (Files.exists(dst)) {
                    continue;                       // 目标已有同名项就不动
                }
                try {
                    Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    clean = false;
                    DebugLogger.warn("Cannot move " + src.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            clean = false;
            DebugLogger.warn("Cannot list legacy plugins dir: " + e.getMessage());
        }

        if (clean && removeIfEmpty(legacy)) {
            DebugLogger.info("Migrated plugins/ -> " + display(current));
        }
    }

    private static void migrateConfig(File legacy, File current) {
        if (!legacy.isFile() || current.exists()) return;

        File parent = current.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            DebugLogger.warn("Cannot create " + display(parent) + ", keeping legacy config");
            return;
        }

        try {
            Files.move(legacy.toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING);
            DebugLogger.info("Migrated config -> " + display(current));
        } catch (IOException e) {
            DebugLogger.warn("Cannot move legacy config: " + e.getMessage());
            return;
        }

        // 搬空后尝试删掉 .command-manager/ 空壳；里面还有别的东西就保留
        removeIfEmpty(legacy.getParentFile());
    }

    // ---------- 内部辅助 ----------

    private static boolean hasChildren(File dir) {
        String[] children = dir.list();
        return children != null && children.length > 0;
    }

    /** 目录为空则删掉，返回是否确实删除了。 */
    private static boolean removeIfEmpty(File dir) {
        if (dir == null || !dir.isDirectory() || hasChildren(dir)) return false;
        if (dir.delete()) {
            DebugLogger.info("Removed empty legacy dir: " + dir.getName());
            return true;
        }
        return false;
    }

    /** 日志里用相对工作目录的短路径，别刷一长串绝对路径。 */
    private static String display(File file) {
        return file == null ? "?" : file.getPath().replace('\\', '/');
    }
}
