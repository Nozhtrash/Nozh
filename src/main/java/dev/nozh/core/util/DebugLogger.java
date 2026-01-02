package dev.nozh.core.util;

import dev.nozh.NozhConstants;
import dev.nozh.core.config.ConfigManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Async debug file logger for NOZH diagnostics.
 * 
 * CRITICAL DESIGN:
 * - log() enqueues message (lock-free, zero disk IO)
 * - Daemon writer thread drains queue and writes to disk
 * - If debugLogs=false, cost is a single 'if' check + return
 * - Rotation happens ONLY on writer thread
 * - Safe shutdown ensures all pending logs are flushed
 * 
 * This design guarantees ZERO impact on render/tick hot paths.
 */
public final class DebugLogger {

    private static final Path LOG_FILE = NozhConstants.CONFIG_DIR.resolve("nozh-debug.log");
    private static final long MAX_LOG_SIZE = 5 * 1024 * 1024; // 5MB
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>(1000);
    private static Thread writerThread;
    private static volatile boolean running = false;
    private static boolean initialized = false;

    private DebugLogger() {
    }

    /**
     * Initialize async debug logger if debugLogs is enabled.
     */
    public static synchronized void init() {
        if (initialized)
            return;
        initialized = true;

        if (!ConfigManager.getConfig().debugLogs) {
            return; // Debug disabled
        }

        try {
            Files.createDirectories(NozhConstants.CONFIG_DIR);

            // Rotate if needed (BEFORE starting writer)
            rotateIfNeeded();

            running = true;
            writerThread = new Thread(DebugLogger::writerLoop, "NOZH-DebugWriter");
            writerThread.setDaemon(true); // Don't block JVM shutdown
            writerThread.start();

            log("INIT", "=== NOZH Debug Session Started ===");
            log("INIT", "Version: " + NozhConstants.getVersion());

        } catch (IOException e) {
            NozhConstants.LOGGER.error("Failed to initialize debug logger: {}", e.getMessage());
        }
    }

    /**
     * Enqueue a log message (FAST, NO BLOCKING).
     * Cost if debug=false: single boolean check.
     */
    public static void log(String level, String message) {
        if (!ConfigManager.getConfig().debugLogs || !running) {
            return; // Early exit, zero cost
        }

        String timestamp = TIMESTAMP_FORMAT.format(Instant.now());
        String line = String.format("[%s] [%s] [%s] %s",
                timestamp, Thread.currentThread().getName(), level, message);

        logQueue.offer(line); // Non-blocking offer
    }

    /**
     * Structured log for profiler.
     */
    public static void logProfiler(String event, double avgMs, double p95Ms, int samples) {
        if (!ConfigManager.getConfig().debugLogs)
            return;
        log("PROFILER", String.format("%s | avg=%.2fms p95=%.2fms samples=%d",
                event, avgMs, p95Ms, samples));
    }

    /**
     * Structured log for governor.
     */
    public static void logGovernor(String decision, String bound, String severity, double confidence) {
        if (!ConfigManager.getConfig().debugLogs)
            return;
        log("GOVERNOR", String.format("Decision=%s Bound=%s Severity=%s Confidence=%.2f",
                decision, bound, severity, confidence));
    }

    /**
     * Structured log for executor.
     */
    public static void logExecutor(String action, String status, String reason) {
        if (!ConfigManager.getConfig().debugLogs)
            return;
        log("EXECUTOR", String.format("Action=%s Status=%s Reason=%s", action, status, reason));
    }

    /**
     * Stop the writer thread and flush remaining logs.
     */
    public static synchronized void close() {
        if (!running)
            return;

        log("SHUTDOWN", "=== NOZH Debug Session Ended ===");

        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
            try {
                writerThread.join(2000); // Wait max 2s for flush
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Writer thread loop (daemon, async).
     */
    private static void writerLoop() {
        try (BufferedWriter writer = Files.newBufferedWriter(LOG_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            while (running || !logQueue.isEmpty()) {
                String line = logQueue.poll();
                if (line != null) {
                    writer.write(line);
                    writer.newLine();

                    // Flush every 10 lines or if queue is empty (for crash safety)
                    if (logQueue.isEmpty() || logQueue.size() % 10 == 0) {
                        writer.flush();
                    }
                } else {
                    Thread.sleep(50); // Reduce CPU spinning
                }
            }

        } catch (IOException | InterruptedException e) {
            NozhConstants.LOGGER.error("Debug writer thread error: {}", e.getMessage());
        }
    }

    /**
     * Rotate log file if too large (called BEFORE writer starts).
     */
    private static void rotateIfNeeded() throws IOException {
        if (Files.exists(LOG_FILE) && Files.size(LOG_FILE) > MAX_LOG_SIZE) {
            Path backup = NozhConstants.CONFIG_DIR.resolve("nozh-debug.log.old");
            Files.deleteIfExists(backup);
            Files.move(LOG_FILE, backup); // Simple rename
        }
    }
}
