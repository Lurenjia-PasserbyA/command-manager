package org.passerbya;

import javafx.application.Platform;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class CommandManager {

    private Process process;
    private BufferedWriter writer;
    private ScheduledExecutorService scheduler;
    private Consumer<String> outputCallback;
    private boolean isRunning = false;

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    // 在启动进程时用
    public void start() {
        if (isRunning) return;

        try {
            ProcessBuilder pb;
            String charset;

            if (isWindows()) {
                pb = new ProcessBuilder("powershell.exe");
                charset = "GBK";
            } else {
                pb = new ProcessBuilder("bash");
                charset = "UTF-8";
            }

            pb.redirectErrorStream(true);

            process = pb.start();

            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), charset));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset));

            // 启动一个"守护线程"（后台线程），持续阻塞读取
            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    // 这里会一直阻塞，直到读到一行或者进程结束
                    while ((line = reader.readLine()) != null) {
                        // 把每一行输出回调给 GUI
                        String finalLine = line;
                        if (outputCallback != null) {
                            Platform.runLater(() -> outputCallback.accept(finalLine));
                        }
                    }
                } catch (IOException e) {
                    if (process != null && !process.isAlive()) {
                        System.out.println("进程已关闭，读取线程结束");
                    } else {
                        e.printStackTrace();
                    }
                }
            });
            // 设为守护线程，这样主程序退出时自动结束
            readerThread.setDaemon(true);
            readerThread.start();

            isRunning = true;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void executeCommand (String command) {
        try {
            writer.write(command);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setOutputCallback(Consumer<String> callback) {
        this.outputCallback = callback;
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
        isRunning = false;
    }
}