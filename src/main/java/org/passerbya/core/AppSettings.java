package org.passerbya.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.passerbya.debug.DebugLogger;

import java.io.File;
import java.io.IOException;

/**
 * 应用设置。持久化到 {工作目录}/.command-manager/config.json。
 * 只放“进程 / 终端”层面的可调参数，UI 主题由 style.css 负责。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppSettings {

    private static final String CONFIG_DIR = ".command-manager";
    private static final String CONFIG_FILE = "config.json";

    private String shell = defaultShell();
    private String shellArgs = "";
    private String workingDirectory = "";
    private String charset = defaultCharset();
    private int terminalFontSize = 13;
    private int scrollbackLimit = 5000;
    private boolean cdToPluginsDirOnStart = false;

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String defaultShell() {
        return isWindows() ? "powershell.exe" : "bash";
    }

    private static String defaultCharset() {
        return isWindows() ? "GBK" : "UTF-8";
    }

    /**
     * Windows 下 PowerShell 的默认启动参数：关掉横幅与退出 logo，
     * 否则终端输出区会被启动噪音刷屏。
     */
    private static String defaultArgsFor(String shell) {
        if (shell == null) return "";
        String exe = shell.toLowerCase();
        if (exe.endsWith("powershell.exe") || exe.endsWith("pwsh.exe")) {
            return "-NoLogo -NoExit";
        }
        return "";
    }

    public static File configFile() {
        return new File(CONFIG_DIR, CONFIG_FILE);
    }

    /**
     * 读取配置。文件不存在或损坏时返回默认配置并记日志，不抛异常 ——
     * 配置坏了不应该阻止程序启动。
     */
    public static AppSettings load() {
        File file = configFile();
        if (!file.exists()) {
            DebugLogger.info("No settings file at " + file.getAbsolutePath() + ", using defaults");
            return new AppSettings();
        }
        try {
            AppSettings settings = new ObjectMapper().readValue(file, AppSettings.class);
            DebugLogger.info("Settings loaded from " + file.getAbsolutePath());
            return settings;
        } catch (IOException e) {
            DebugLogger.error("Failed to read settings, falling back to defaults", e);
            return new AppSettings();
        }
    }

    /** 写入配置。失败时记日志并返回 false，不抛异常。 */
    public boolean save() {
        File file = configFile();
        File dir = file.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            DebugLogger.error("Cannot create settings directory: " + dir.getAbsolutePath());
            return false;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, this);
            DebugLogger.info("Settings saved to " + file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            DebugLogger.error("Failed to write settings to " + file.getAbsolutePath(), e);
            return false;
        }
    }

    /** 实际使用的启动参数：用户填了就用用户的，否则按平台给默认值。 */
    public String resolvedShellArgs() {
        if (shellArgs != null && !shellArgs.isBlank()) return shellArgs;
        return defaultArgsFor(shell);
    }

    public String[] splitShellArgs() {
        String args = resolvedShellArgs();
        if (args == null || args.isBlank()) return new String[0];
        return args.trim().split("\\s+");
    }

    /** 让 shell 切到插件目录的起始命令；不需要则为 null。 */
    public String startupCommand() {
        if (!cdToPluginsDirOnStart) return null;
        String dir = PluginManager.pluginsDirectory().getAbsolutePath();
        return isWindows()
                ? "Set-Location -LiteralPath '" + dir + "'"
                : "cd '" + dir + "'";
    }

    // ---------- getters / setters ----------

    public String getShell() { return shell; }
    public void setShell(String shell) { this.shell = shell; }

    public String getShellArgs() { return shellArgs; }
    public void setShellArgs(String shellArgs) { this.shellArgs = shellArgs; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }

    public int getTerminalFontSize() { return terminalFontSize; }
    public void setTerminalFontSize(int terminalFontSize) { this.terminalFontSize = terminalFontSize; }

    public int getScrollbackLimit() { return scrollbackLimit; }
    public void setScrollbackLimit(int scrollbackLimit) { this.scrollbackLimit = scrollbackLimit; }

    public boolean isCdToPluginsDirOnStart() { return cdToPluginsDirOnStart; }
    public void setCdToPluginsDirOnStart(boolean cdToPluginsDirOnStart) {
        this.cdToPluginsDirOnStart = cdToPluginsDirOnStart;
    }
}
