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

    /**
     * 默认 shell 用**绝对路径**，不依赖 PATH。
     *
     * <p>写 "powershell.exe" 看着更干净，但 PATH 是会被改的 —— 安全加固、
     * 精简 PATH、把 System32 剔出去之类都会让裸名字解析不到，然后程序莫名其妙
     * 起不来。绝对路径没这个风险。
     *
     * <p>Windows 上优先 PowerShell 7，没有再退回系统自带的 Windows PowerShell。
     * PowerShell 7 有两种装法，路径不一样，都要认：WindowsApps 下的执行别名
     * （Store / MSIX 版）、Program Files 下的真实 exe（MSI 版）。
     */
    private static String defaultShell() {
        if (!isWindows()) {
            return "/bin/bash";
        }
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            systemRoot = "C:\\Windows";
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            String alias = localAppData + "\\Microsoft\\WindowsApps\\pwsh.exe";
            if (pathExists(alias)) {
                return alias;
            }
        }

        String pwsh7Msi = systemRoot + "\\Program Files\\PowerShell\\7\\pwsh.exe";
        if (pathExists(pwsh7Msi)) {
            return pwsh7Msi;
        }

        return systemRoot + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    }

    /**
     * 判断一个可执行文件路径是否可用。
     *
     * <p>{@code File.isFile()} 在这里不够用：Windows Store 应用执行别名是个
     * reparse point，Java 对它的 {@code exists()} / {@code isFile()} / {@code canRead()}
     * **全部返回 false**，只有列父目录才看得见。实测：
     *
     * <pre>
     * new File("...\\WindowsApps\\pwsh.exe").isFile()   // false
     * new File("...\\WindowsApps").list()               // 里面有 pwsh.exe
     * </pre>
     *
     * <p>所以先试 {@code isFile()}（MSI 那种真实文件走这条），不行再列父目录按名字找。
     * 别用 {@code canRead()} —— 别名同样返回 false，但**照样能启动**（已验证）。
     */
    private static boolean pathExists(String path) {
        File file = new File(path);
        if (file.isFile()) {
            return true;
        }
        File parent = file.getParentFile();
        String name = file.getName();
        String[] entries = parent == null ? null : parent.list();
        if (entries == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static String defaultCharset() {
        if (!isWindows()) {
            return "UTF-8";
        }
        // PowerShell 7 默认按 UTF-8 输出，5.1 按系统代码页（中文系统是 GBK）。
        // 默认编码必须跟着默认 shell 走，否则中文输出直接乱码。
        String shell = defaultShell();
        if (shell.toLowerCase().endsWith("pwsh.exe")) {
            return "UTF-8";
        }
        return "GBK";
    }

    /**
     * 指定 shell 的默认启动参数。
     *
     * <p>公开是为了让 {@code CommandManager} 在回退到候选 shell 时，
     * 能按**候选自己**的类型给参数 —— 否则把给原 shell 的参数照搬过去，
     * 回退到一个 PowerShell 时会漏掉 -NoLogo -NoExit，终端被 banner 刷屏。
     *
     * <p>用文件名后缀判断，所以绝对路径和裸名字都能命中。
     */
    public static String defaultArgsFor(String shell) {
        if (shell == null) return "";
        String exe = shell.toLowerCase();
        if (exe.endsWith("powershell.exe") || exe.endsWith("pwsh.exe")) {
            return "-NoLogo -NoExit";
        }
        return "";
    }

    /**
     * 检查编码和 shell 是否对得上，对不上返回一句提示，对得上返回 null。
     *
     * <p>这个坑很隐蔽：Windows PowerShell 5.1 默认按系统代码页（中文系统是 GBK）
     * 输出，而 PowerShell 7 默认输出 UTF-8。默认 shell 会优先选 pwsh 7，
     * 但默认编码仍是 GBK —— 不提醒的话，中文输出会变成乱码而没人知道为什么。
     */
    public String encodingWarning() {
        if (shell == null || charset == null) return null;

        String exe = shell.toLowerCase();
        boolean isPwsh7 = exe.endsWith("pwsh.exe");
        boolean isUtf8 = charset.toLowerCase().replace("-", "").equals("utf8");

        if (isPwsh7 && !isUtf8) {
            return "当前 shell 是 PowerShell 7，它默认按 UTF-8 输出，"
                    + "但编码设成了 " + charset + "，中文会变乱码。建议把编码改成 UTF-8。";
        }
        if (!isPwsh7 && exe.endsWith("powershell.exe") && isUtf8) {
            return "当前 shell 是 Windows PowerShell 5.1，它默认按系统代码页输出，"
                    + "但编码设成了 UTF-8。中文可能变乱码，建议改回 GBK。";
        }
        return null;
    }

    /**
     * 设置文件位置。委托给 {@link AppPaths}，避免路径散落在多个类里。
     */
    public static File configFile() {
        return AppPaths.configFile();
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
    /** 实际生效的启动参数：用户填了就用用户的，否则用配置 shell 的默认值。 */
    public String resolvedShellArgs() {
        if (shellArgs != null && !shellArgs.isBlank()) return shellArgs;
        return defaultArgsFor(shell);
    }

    /**
     * 用户在设置里**显式**填写的启动参数。
     *
     * <p>注意这里故意<b>不</b>回退到 {@link #defaultArgsFor}：调用方
     * （{@code CommandManager}）需要区分"用户指定"和"shell 自带默认值"，
     * 因为默认值只适用于匹配的那个 shell。若在这里就把默认值混进来，
     * 一旦回退到别的 shell（比如 bash），就会把 -NoLogo -NoExit 传过去把它弄崩。
     *
     * <p>没填时返回空数组。
     */
    public String[] explicitShellArgs() {
        if (shellArgs == null || shellArgs.isBlank()) return new String[0];
        return shellArgs.trim().split("\\s+");
    }

    /** 实际使用的启动参数（含默认值），按空白拆开。 */
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
