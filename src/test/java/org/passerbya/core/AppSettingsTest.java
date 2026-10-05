package org.passerbya.core;

import org.junit.jupiter.api.Test;

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

    @Test
    void configFilePointsAtHiddenDirectory() {
        String path = AppSettings.configFile().getPath();

        assertTrue(path.contains(".command-manager"), "实际路径：" + path);
        assertTrue(path.endsWith("config.json"), "实际路径：" + path);
    }
}
