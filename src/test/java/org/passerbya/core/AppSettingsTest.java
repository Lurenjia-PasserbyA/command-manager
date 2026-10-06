package org.passerbya.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AppSettings 的默认值与派生逻辑。不触碰磁盘。
 */
class AppSettingsTest {

    @Test
    void defaultsAreUsable() {
        AppSettings settings = new AppSettings();

        assertNotNull(settings.getShell());
        assertFalse(settings.getShell().isBlank());
        assertNotNull(settings.getCharset());
        assertTrue(settings.getTerminalFontSize() > 0);
        assertTrue(settings.getScrollbackLimit() > 0);
    }

    /**
     * 默认 shell 必须是绝对路径：裸名字依赖 PATH，而 PATH 是会被改的
     * （安全加固、精简 PATH 都会让 "powershell.exe" 解析不到）。
     */
    @Test
    void defaultShellIsAbsoluteAndNotBareName() {
        String shell = new AppSettings().getShell();

        boolean absolute = shell.startsWith("/") || shell.matches("^[A-Za-z]:[\\\\/].*");
        assertTrue(absolute, "默认 shell 应为绝对路径，实际：" + shell);

        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            assertTrue(shell.toLowerCase().endsWith("powershell.exe")
                            || shell.toLowerCase().endsWith("pwsh.exe"),
                    "Windows 上默认应为 PowerShell，实际：" + shell);
        } else {
            assertTrue(shell.endsWith("bash") || shell.endsWith("sh"),
                    "非 Windows 上默认应为 bash/sh，实际：" + shell);
        }
    }

    /** 绝对路径的 PowerShell 也应该拿到 -NoLogo -NoExit（后缀匹配，不受路径影响）。 */
    @Test
    void absolutePowerShellPathStillGetsDefaultArgs() {
        AppSettings settings = new AppSettings();
        settings.setShell("D:\\tools\\pwsh.exe");
        settings.setShellArgs("");

        assertEquals("-NoLogo -NoExit", settings.resolvedShellArgs());
    }

    @Test
    void blankShellArgsFallBackToPlatformDefault() {
        AppSettings settings = new AppSettings();
        settings.setShell("pwsh.exe");
        settings.setShellArgs("   ");

        // PowerShell 家族默认要带 -NoLogo -NoExit，否则终端会被 banner 刷屏
        assertEquals("-NoLogo -NoExit", settings.resolvedShellArgs());
    }

    @Test
    void explicitShellArgsWin() {
        AppSettings settings = new AppSettings();
        settings.setShell("pwsh.exe");
        settings.setShellArgs("-NoProfile");

        assertEquals("-NoProfile", settings.resolvedShellArgs());
    }

    /**
     * explicitShellArgs() 必须区分"用户填了"和"shell 自带默认值"。
     *
     * <p>回归测试：如果这里在 shellArgs 为空时回退到 shell 的默认参数，
     * CommandManager 回退到 bash 时会把 -NoLogo -NoExit 一起传过去，
     * bash 直接以 "invalid option" 失败。
     */
    @Test
    void explicitShellArgsStayEmptyWhenUserLeftThemBlank() {
        AppSettings settings = new AppSettings();
        settings.setShell("pwsh.exe");
        settings.setShellArgs("");

        // 面向用户的取值仍然有默认值……
        assertEquals("-NoLogo -NoExit", settings.resolvedShellArgs());
        // ……但"用户显式指定"必须是空的，否则会污染回退候选
        assertArrayEquals(new String[0], settings.explicitShellArgs());
    }

    @Test
    void explicitShellArgsAreSplitWhenProvided() {
        AppSettings settings = new AppSettings();
        settings.setShellArgs("-NoLogo   -NoProfile");

        assertArrayEquals(new String[]{"-NoLogo", "-NoProfile"}, settings.explicitShellArgs());
    }

    @Test
    void nonPowerShellShellGetsNoDefaultArgs() {
        AppSettings settings = new AppSettings();
        settings.setShell("bash");
        settings.setShellArgs("");

        assertEquals("", settings.resolvedShellArgs());
        assertArrayEquals(new String[0], settings.splitShellArgs());
    }

    @Test
    void shellArgsAreSplitOnWhitespace() {
        AppSettings settings = new AppSettings();
        settings.setShellArgs("-NoLogo   -NoProfile -NoExit");

        assertArrayEquals(new String[]{"-NoLogo", "-NoProfile", "-NoExit"}, settings.splitShellArgs());
    }

    @Test
    void powershellExeAlsoGetsDefaultArgs() {
        AppSettings settings = new AppSettings();
        settings.setShell("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe");
        settings.setShellArgs("");

        assertEquals("-NoLogo -NoExit", settings.resolvedShellArgs());
    }

    @Test
    void startupCommandIsNullByDefault() {
        AppSettings settings = new AppSettings();

        assertNull(settings.startupCommand());
    }

    @Test
    void startupCommandIsProducedWhenEnabled() {
        AppSettings settings = new AppSettings();
        settings.setCdToPluginsDirOnStart(true);

        String command = settings.startupCommand();

        assertNotNull(command);
        assertTrue(command.contains("plugins"), "起始命令应指向插件目录，实际：" + command);
    }

    // ---------- 编码与 shell 的匹配 ----------

    @Test
    void noEncodingWarningWhenPwshPairsWithUtf8() {
        AppSettings settings = new AppSettings();
        settings.setShell("C:\\tools\\pwsh.exe");
        settings.setCharset("UTF-8");

        assertNull(settings.encodingWarning());
    }

    @Test
    void warnsWhenPwshIsPairedWithGbk() {
        AppSettings settings = new AppSettings();
        settings.setShell("C:\\tools\\pwsh.exe");
        settings.setCharset("GBK");

        String warning = settings.encodingWarning();

        // pwsh 7 输出 UTF-8，配 GBK 会乱码 —— 静默出错最难查，必须提醒
        assertNotNull(warning, "pwsh 7 + GBK 应该给出提示");
        assertTrue(warning.contains("UTF-8"), "提示里应说明该改成什么，实际：" + warning);
    }

    @Test
    void noEncodingWarningWhenWindowsPowerShellPairsWithGbk() {
        AppSettings settings = new AppSettings();
        settings.setShell("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe");
        settings.setCharset("GBK");

        assertNull(settings.encodingWarning());
    }

    @Test
    void warnsWhenWindowsPowerShellIsPairedWithUtf8() {
        AppSettings settings = new AppSettings();
        settings.setShell("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe");
        settings.setCharset("UTF-8");

        assertNotNull(settings.encodingWarning(), "5.1 + UTF-8 应该给出提示");
    }

    @Test
    void nonPowerShellShellNeverWarnsAboutEncoding() {
        AppSettings settings = new AppSettings();
        settings.setShell("/bin/bash");
        settings.setCharset("GBK");

        assertNull(settings.encodingWarning());
    }

    @Test
    void configFileLivesUnderCmmgrConfig() {
        Path expected = Path.of(System.getProperty("user.dir")).resolve("cmmgr/config/config.json");

        assertEquals(expected.normalize(), AppSettings.configFile().toPath().normalize(),
                "配置应落在 <工作目录>/cmmgr/config/config.json");
    }

    /** 配置和插件必须在同一个 cmmgr/ 根下，各占一个子目录。 */
    @Test
    void pluginsAndConfigShareTheCmmgrRoot() {
        Path workdir = Path.of(System.getProperty("user.dir"));

        assertEquals(workdir.resolve("cmmgr/plugins").normalize(),
                AppPaths.pluginsDirectory().toPath().normalize());
        // 两者都从 cmmgr/ 下派生
        assertTrue(AppPaths.configFile().toPath().normalize()
                        .startsWith(workdir.resolve("cmmgr").normalize()),
                "配置应在 cmmgr/ 下");
    }
}
