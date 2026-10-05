package org.passerbya.core;

import javafx.application.Platform;
import org.passerbya.debug.DebugLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * 管理一个长期存活的交互式 shell 进程：把命令写进它的 stdin，把输出读回来。
 *
 * <p>注意这不是 {@code cmd /c} 式的一次性执行 —— 进程常驻，cd / 环境变量等
 * 状态会跨命令保留。
 */
public class CommandManager {

    private Process process;
    private BufferedWriter writer;
    private Thread readerThread;
    private Consumer<String> outputCallback;
    private boolean isRunning = false;

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static Charset resolveCharset(String name) {
        try {
            return name == null || name.isBlank() ? Charset.defaultCharset() : Charset.forName(name);
        } catch (IllegalArgumentException e) {
            // 涵盖 UnsupportedCharsetException / IllegalCharsetNameException
            DebugLogger.warn("Unknown charset '" + name + "', falling back to default");
            return Charset.defaultCharset();
        }
    }

    /**
     * 返回可用的 shell 候选，按优先级排列。
     *
     * <p>用户配置的排第一；后面是系统上真实存在的兜底项，防止配错一个路径
     * 就彻底起不来。
     */
    private static java.util.List<String> shellCandidates(String configured) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(configured);
        }

        if (isWindows()) {
            String systemRoot = System.getenv("SystemRoot");
            if (systemRoot == null || systemRoot.isBlank()) {
                systemRoot = "C:\\Windows";
            }
            candidates.add(systemRoot + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe");
            candidates.add(systemRoot + "\\Program Files\\PowerShell\\7\\pwsh.exe");
            candidates.add("powershell.exe");
            candidates.add("pwsh.exe");
        } else {
            candidates.add("/bin/bash");
            candidates.add("/bin/sh");
            candidates.add("bash");
            candidates.add("sh");
        }
        return candidates;
    }

    /**
     * 逐个试候选 shell，返回第一个真的能起来的。
     *
     * <p>判断依据是"能不能起来"而不是"文件在不在" —— 裸名字要走 PATH 查找，
     * 光看 {@code File.exists()} 判断不出来。所以直接试着启动，失败就换下一个。
     *
     * @param userArgs 用户在设置里显式填的启动参数；留空表示没填
     * @param configured 用户配置的那个 shell，用来区分"首选项"和"回退项"
     */
    private Process launchShell(java.util.List<String> candidates, String[] userArgs,
                                String configured, File workDir) {
        boolean hasUserArgs = userArgs != null && userArgs.length > 0;

        for (String candidate : candidates) {
            try {
                java.util.List<String> argv = new java.util.ArrayList<>();
                argv.add(candidate);

                // 用户显式填过参数 -> 处处照用；没填 -> 按**这个候选自己**的类型给默认值。
                // 后者很重要：回退到一个 PowerShell 时不能漏掉 -NoLogo -NoExit。
                if (hasUserArgs) {
                    argv.addAll(java.util.Arrays.asList(userArgs));
                } else {
                    String defaults = AppSettings.defaultArgsFor(candidate);
                    if (!defaults.isBlank()) {
                        argv.addAll(java.util.Arrays.asList(defaults.trim().split("\\s+")));
                    }
                }

                ProcessBuilder pb = new ProcessBuilder(argv);
                pb.redirectErrorStream(true);
                if (workDir != null) pb.directory(workDir);

                Process started = pb.start();
                if (configured != null && !candidate.equals(configured)) {
                    DebugLogger.warn("Fell back to shell: " + candidate);
                }
                DebugLogger.info("Shell started: " + argv);
                return started;
            } catch (IOException e) {
                DebugLogger.warn("Cannot start shell '" + candidate + "': " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 启动 shell。已在运行则直接返回。
     *
     * @param settings 决定 shell 可执行文件、启动参数、工作目录与编码
     */
    public void start(AppSettings settings) {
        if (isRunning) return;
        if (settings == null) settings = new AppSettings();

        Charset charset = resolveCharset(settings.getCharset());

        // 编码和 shell 对不上时明确提醒。这个坑很隐蔽：
        // pwsh 7 输出 UTF-8，Windows PowerShell 5.1 输出系统代码页，配错了中文就是乱码。
        String encodingWarning = settings.encodingWarning();
        if (encodingWarning != null) {
            DebugLogger.warn(encodingWarning);
            if (outputCallback != null) {
                Platform.runLater(() -> outputCallback.accept("[warn] " + encodingWarning));
            }
        }

        File workDir = null;
        String configuredWorkDir = settings.getWorkingDirectory();
        if (configuredWorkDir != null && !configuredWorkDir.isBlank()) {
            File dir = new File(configuredWorkDir);
            if (dir.isDirectory()) {
                workDir = dir;
            } else {
                DebugLogger.warn("Working directory does not exist, ignoring: " + configuredWorkDir);
            }
        }

        java.util.List<String> candidates = shellCandidates(settings.getShell());
        process = launchShell(candidates, settings.explicitShellArgs(), settings.getShell(), workDir);

        if (process == null) {
            DebugLogger.error("No usable shell found. Tried: " + candidates);
            if (outputCallback != null) {
                Platform.runLater(() -> outputCallback.accept(
                        "[error] 启动不了任何 shell，请在设置页检查 Shell 路径。"));
            }
            return;
        }

        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), charset));
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), charset));

        readerThread = new Thread(() -> pump(reader), "command-manager-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        isRunning = true;
        DebugLogger.info("Shell charset: " + charset.name());

        // 可选的起始命令，例如切到插件目录
        String startup = settings.startupCommand();
        if (startup != null) executeCommand(startup);
    }

    private void pump(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String finalLine = line;
                Consumer<String> callback = outputCallback;
                if (callback != null) {
                    Platform.runLater(() -> callback.accept(finalLine));
                }
            }
            DebugLogger.info("Shell output stream closed.");
        } catch (IOException e) {
            // 进程被 stop() 杀掉时走到这里，属于正常收尾，不该报错
            if (process != null && process.isAlive()) {
                DebugLogger.error("Reader thread error", e);
            } else {
                DebugLogger.info("Shell process has exited.");
            }
        }
    }

    /** 把一条命令写进 shell 的 stdin。 */
    public void executeCommand(String command) {
        if (command == null || command.isBlank()) return;
        if (writer == null) {
            DebugLogger.warn("Shell is not running, cannot execute: " + command);
            if (outputCallback != null) {
                Platform.runLater(() -> outputCallback.accept("[error] shell 未启动，命令未执行"));
            }
            return;
        }
        try {
            writer.write(command);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            DebugLogger.error("Failed to write command to shell: " + command, e);
        }
    }

    public void setOutputCallback(Consumer<String> callback) {
        this.outputCallback = callback;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /** 关闭 shell 进程。先温和地 destroy，给它一点时间，再强制杀。 */
    public void stop() {
        isRunning = false;

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // 关闭失败无所谓，下面直接杀进程
            }
            writer = null;
        }

        if (process != null) {
            process.destroy();
            try {
                if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
            process = null;
        }
        readerThread = null;
    }
}
