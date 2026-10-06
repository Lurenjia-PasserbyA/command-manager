package org.passerbya.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 旧布局迁移测试。
 *
 * <p>{@link AppPaths} 的根目录取自 {@code user.dir} 属性，所以这里把它指向
 * JUnit 的临时目录即可把迁移跑在沙箱里，不会碰到真实的运行目录。
 * （用 {@code new File("x")} 的隐式解析是做不到这点的 —— 那玩意儿的基准是
 * 进程真实工作目录，改 {@code user.dir} 没用。）
 */
class AppPathsTest {

    @FunctionalInterface
    private interface Body {
        void run() throws Exception;
    }

    /** 在临时目录里跑一段代码，结束后恢复 user.dir。 */
    private static void inDir(Path dir, Body body) throws Exception {
        String original = System.getProperty("user.dir");
        System.setProperty("user.dir", dir.toString());
        try {
            body.run();
        } finally {
            System.setProperty("user.dir", original);
        }
    }

    // ---------- 路径形状 ----------

    @Test
    void pathsAreNestedUnderCmmgr() {
        Path workdir = Path.of(System.getProperty("user.dir"));

        // 路径都相对工作目录，落在 cmmgr/ 下面
        assertEquals(workdir.resolve("cmmgr/plugins").normalize(),
                AppPaths.pluginsDirectory().toPath().normalize());
        assertEquals(workdir.resolve("cmmgr/config/config.json").normalize(),
                AppPaths.configFile().toPath().normalize());
    }

    @Test
    void pathsFollowUserDir(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            assertEquals(dir.resolve("cmmgr/plugins"),
                    AppPaths.pluginsDirectory().toPath().normalize());
            assertEquals(dir.resolve("cmmgr/config/config.json"),
                    AppPaths.configFile().toPath().normalize());
        });
    }

    // ---------- 迁移 ----------

    @Test
    void migratesLegacyPluginsAndConfig(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            Path legacyPlugin = dir.resolve("plugins/ffmpeg");
            Files.createDirectories(legacyPlugin);
            Files.writeString(legacyPlugin.resolve("manifest.json"), "{\"id\":\"ffmpeg\"}");
            Files.writeString(legacyPlugin.resolve("run.ps1"), "# script");
            Files.createDirectories(dir.resolve(".command-manager"));
            Files.writeString(dir.resolve(".command-manager/config.json"), "{\"shell\":\"x\"}");

            AppPaths.migrateLegacyLayout();

            Path newPlugin = dir.resolve("cmmgr/plugins/ffmpeg");
            assertTrue(Files.isDirectory(newPlugin), "插件目录应被迁移");
            assertTrue(Files.isRegularFile(newPlugin.resolve("manifest.json")), "manifest 应被迁移");
            assertTrue(Files.isRegularFile(newPlugin.resolve("run.ps1")), "run.ps1 应被迁移");

            Path newConfig = dir.resolve("cmmgr/config/config.json");
            assertTrue(Files.isRegularFile(newConfig), "配置应被迁移");
            assertEquals("{\"shell\":\"x\"}", Files.readString(newConfig), "配置内容不应被改动");

            assertFalse(Files.exists(dir.resolve("plugins")), "空的旧 plugins/ 应被删除");
            assertFalse(Files.exists(dir.resolve(".command-manager")), "空的旧配置目录应被删除");
        });
    }

    @Test
    void skipsWhenTargetAlreadyHasPlugins(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            Files.createDirectories(dir.resolve("cmmgr/plugins/existing"));
            Files.writeString(dir.resolve("cmmgr/plugins/existing/manifest.json"), "{}");
            Files.createDirectories(dir.resolve("plugins/old"));
            Files.writeString(dir.resolve("plugins/old/manifest.json"), "{}");

            AppPaths.migrateLegacyLayout();
            AppPaths.migrateLegacyLayout();   // 幂等：第二次也不能出问题

            assertTrue(Files.isDirectory(dir.resolve("cmmgr/plugins/existing")));
            assertFalse(Files.exists(dir.resolve("cmmgr/plugins/old")), "目标非空时不应再搬");
            assertTrue(Files.isDirectory(dir.resolve("plugins/old")), "跳过后旧目录应原样保留");
        });
    }

    @Test
    void doesNothingOnCleanDirectory(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            AppPaths.migrateLegacyLayout();

            assertFalse(Files.exists(dir.resolve("cmmgr")), "没有旧数据时不该创建 cmmgr/");
        });
    }

    @Test
    void doesNotOverwriteExistingNewConfig(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            Files.createDirectories(dir.resolve("cmmgr/config"));
            Files.writeString(dir.resolve("cmmgr/config/config.json"), "{\"new\":true}");
            Files.createDirectories(dir.resolve(".command-manager"));
            Files.writeString(dir.resolve(".command-manager/config.json"), "{\"old\":true}");

            AppPaths.migrateLegacyLayout();

            assertEquals("{\"new\":true}",
                    Files.readString(dir.resolve("cmmgr/config/config.json")),
                    "新配置不能被旧配置覆盖");
        });
    }

    @Test
    void keepsLegacyConfigDirWhenItHasOtherFiles(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            Files.createDirectories(dir.resolve(".command-manager"));
            Files.writeString(dir.resolve(".command-manager/config.json"), "{}");
            Files.writeString(dir.resolve(".command-manager/important.txt"), "用户的其他文件");

            AppPaths.migrateLegacyLayout();

            assertTrue(Files.isRegularFile(dir.resolve("cmmgr/config/config.json")), "配置应已迁移");
            assertTrue(Files.isDirectory(dir.resolve(".command-manager")), "还有别的文件就不能删目录");
            assertTrue(Files.isRegularFile(dir.resolve(".command-manager/important.txt")),
                    "别的文件不应被碰");
        });
    }

    @Test
    void ignoresMalformedLegacyLayout(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            // 旧 plugins 是个文件而不是目录：非法布局，应被忽略而不是抛异常
            Files.writeString(dir.resolve("plugins"), "not a directory");

            AppPaths.migrateLegacyLayout();

            assertTrue(Files.isRegularFile(dir.resolve("plugins")), "非法旧布局应原样保留");
        });
    }

    @Test
    void migratesPluginsButKeepsUnrelatedLegacyEntries(@TempDir Path dir) throws Exception {
        inDir(dir, () -> {
            // 旧 plugins/ 下混着一个普通文件，也应一并搬过去
            Files.createDirectories(dir.resolve("plugins"));
            Files.writeString(dir.resolve("plugins/notes.txt"), "随手记的");

            AppPaths.migrateLegacyLayout();

            assertTrue(Files.isRegularFile(dir.resolve("cmmgr/plugins/notes.txt")), "文件也应被迁移");
            assertFalse(Files.exists(dir.resolve("plugins")), "搬空后旧目录应删除");
        });
    }
}
