package dev.nozh.core.analytics;

import dev.nozh.NozhConstants;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Logs performance data for debugging and optimization tuning.
 * Useful for bug reports and analyzing performance patterns.
 * 
 * <p>
 * Log levels:
 * <ul>
 * <li>MINIMAL: Only errors</li>
 * <li>NORMAL: Errors + warnings + actions</li>
 * <li>VERBOSE: All decisions</li>
 * <li>DEBUG: Everything</li>
 * </ul>
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class PerformanceLogger {

    /**
     * Log verbosity levels.
     */
    public enum LogLevel {
        MINIMAL(0),
        NORMAL(1),
        VERBOSE(2),
        DEBUG(3);

        public final int priority;

        LogLevel(int priority) {
            this.priority = priority;
        }
    }

    /**
     * Log entry record.
     */
    public record LogEntry(
            long timestamp,
            LogLevel level,
            String category,
            String message,
            double value) {
        @Override
        public String toString() {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            if (value != 0) {
                return String.format("[%s] [%s] [%s] %s (%.2f)", time, level, category, message, value);
            }
            return String.format("[%s] [%s] [%s] %s", time, level, category, message);
        }
    }

    private LogLevel currentLevel;
    private final ConcurrentLinkedQueue<LogEntry> logBuffer;
    private static final int MAX_BUFFER_SIZE = 10000;
    private boolean enabled;

    /**
     * Constructs a new PerformanceLogger with NORMAL level.
     */
    public PerformanceLogger() {
        this.currentLevel = LogLevel.NORMAL;
        this.logBuffer = new ConcurrentLinkedQueue<>();
        this.enabled = true;
    }

    /**
     * Sets the log verbosity level.
     *
     * @param level new log level
     */
    public void setLogLevel(LogLevel level) {
        this.currentLevel = level;
        NozhConstants.LOGGER.info("Performance log level set to: {}", level);
    }

    /**
     * Gets current log level.
     *
     * @return current level
     */
    public LogLevel getLogLevel() {
        return currentLevel;
    }

    /**
     * Enables or disables logging.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Logs an action with its impact.
     *
     * @param action action description
     * @param impact impact value (e.g., FPS change)
     */
    public void logAction(String action, double impact) {
        log(LogLevel.NORMAL, "ACTION", action, impact);
    }

    /**
     * Logs a performance spike.
     *
     * @param frametime frametime in milliseconds
     * @param cause     suspected cause
     */
    public void logSpike(double frametime, String cause) {
        log(LogLevel.NORMAL, "SPIKE", cause, frametime);
    }

    /**
     * Logs a decision made by the optimizer.
     *
     * @param decision decision description
     */
    public void logDecision(String decision) {
        log(LogLevel.VERBOSE, "DECISION", decision, 0);
    }

    /**
     * Logs debug information.
     *
     * @param category debug category
     * @param message  debug message
     */
    public void logDebug(String category, String message) {
        log(LogLevel.DEBUG, category, message, 0);
    }

    /**
     * Logs an error.
     *
     * @param error error description
     */
    public void logError(String error) {
        log(LogLevel.MINIMAL, "ERROR", error, 0);
    }

    /**
     * Core logging method.
     */
    private void log(LogLevel level, String category, String message, double value) {
        if (!enabled || level.priority > currentLevel.priority) {
            return;
        }

        LogEntry entry = new LogEntry(System.currentTimeMillis(), level, category, message, value);
        logBuffer.offer(entry);

        // Trim buffer if too large
        while (logBuffer.size() > MAX_BUFFER_SIZE) {
            logBuffer.poll();
        }
    }

    /**
     * Gets recent log entries.
     *
     * @param count number of entries to retrieve
     * @return list of recent entries
     */
    public List<LogEntry> getRecentEntries(int count) {
        List<LogEntry> entries = new ArrayList<>(logBuffer);
        int start = Math.max(0, entries.size() - count);
        return entries.subList(start, entries.size());
    }

    /**
     * Exports log to a file.
     *
     * @param path file path to export to
     * @throws IOException if export fails
     */
    public void exportLog(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("=== NOZH Performance Log ===");
        lines.add("Exported: " + LocalDateTime.now());
        lines.add("Log Level: " + currentLevel);
        lines.add("Entries: " + logBuffer.size());
        lines.add("");

        for (LogEntry entry : logBuffer) {
            lines.add(entry.toString());
        }

        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        NozhConstants.LOGGER.info("Performance log exported to: {}", path);
    }

    /**
     * Clears the log buffer.
     */
    public void clear() {
        logBuffer.clear();
    }

    /**
     * Gets log buffer size.
     *
     * @return number of entries in buffer
     */
    public int getBufferSize() {
        return logBuffer.size();
    }
}
