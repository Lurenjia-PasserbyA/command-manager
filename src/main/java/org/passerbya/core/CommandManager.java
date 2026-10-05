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
     * 启动 shell。已在运行则直接返回。
     *
     * @param settings 决定 shell 可执行文件、启动参数、工作目录与编码
     */
    public void start(AppSettings settings) {
        if (isRunning) return;
        if (settings == null) settings = new AppSettings();

        String shell = settings.getShell();
        if (shell == null || shell.isBlank()) {
            shell = isWindows() ? "powershell.exe" : "bash";
        }

        Charset charset = resolveCharset(settings.getCharset());

        try {
            java.util.List<String> argv = new java.util.ArrayList<>();
            argv.add(shell);
            argv.addAll(java.util.Arrays.asList(settings.splitShellArgs()));

            ProcessBuilder pb = new ProcessBuilder(argv);
            pb.redirectErrorStream(true);

            String workDir = settings.getWorkingDirectory();
            if (workDir != null && !workDir.isBlank()) {
                File dir = new File(workDir);
                if (dir.isDirectory()) {
                    pb.directory(dir);
                } else {
                    DebugLogger.warn("Working directory does not exist, ignoring: " + workDir);
                }
            }

            process = pb.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), charset));

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), charset));

            readerThread = new Thread(() -> pump(reader), "command-manager-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            isRunning = true;
            DebugLogger.info("Shell started: " + argv + " (charset " + charset.name() + ")");

            // 可选的起始命令，例如切到插件目录
            String startup = settings.startupCommand();
            if (startup != null) executeCommand(startup);

        } catch (IOException e) {
            DebugLogger.error("Failed to start command manager with shell: " + shell, e);
            isRunning = false;
        }
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
