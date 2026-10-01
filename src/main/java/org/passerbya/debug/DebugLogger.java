package org.passerbya.debug;

/**
 * 统一调试日志输出，方便后续替换为真正的日志框架。
 */
public final class DebugLogger {

    private DebugLogger() {}

    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void warn(String message) {
        System.out.println("[WARN] " + message);
    }

    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }

    public static void error(String message, Throwable t) {
        System.err.println("[ERROR] " + message);
        t.printStackTrace();
    }
}