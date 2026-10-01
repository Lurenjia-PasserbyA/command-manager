package org.passerbya.core;

import javafx.application.Platform;
import org.passerbya.debug.DebugLogger;

import java.io.*;
import java.util.concurrent.ScheduledExecutorService;
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

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String finalLine = line;
                        if (outputCallback != null) {
                            Platform.runLater(() -> outputCallback.accept(finalLine));
                        }
                    }
                } catch (IOException e) {
                    if (process != null && !process.isAlive()) {
                        DebugLogger.info("Process has exited.");
                    } else {
                        DebugLogger.error("Reader thread error", e);
                    }
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            isRunning = true;
        } catch (IOException e) {
            DebugLogger.error("Failed to start command manager", e);
        }
    }

    public void executeCommand(String command) {
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