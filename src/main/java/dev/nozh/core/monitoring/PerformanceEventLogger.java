package dev.nozh.core.monitoring;

import dev.nozh.NozhConstants;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.governor.DecisionReasoning;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Structured event logger for performance analysis.
 * 
 * Logs:
 * - Governor decisions with reasoning
 * - Action executions and results
 * - Scenario changes
 * - Performance degradations
 * 
 * Outputs JSON-compatible structured logs for analysis.
 * 
 * TASK 12: Performance logging - structured events
 */
public final class PerformanceEventLogger {

    private final BlockingQueue<LogEvent> eventQueue = new LinkedBlockingQueue<>(1000);
    private final Path logFile;
    private final Thread writerThread;
    private volatile boolean running = true;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    public PerformanceEventLogger(Path logFile) {
        this.logFile = logFile;
        this.writerThread = new Thread(this::processQueue, "NozhEventLogger");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    /**
     * Log governor decision.
     */
    public void logDecision(DecisionReasoning reasoning, Scenario scenario, double confidenceScore) {
        LogEvent event = new LogEvent(
                EventType.DECISION,
                String.format("{\"action\":\"%s\",\"scenario\":\"%s\",\"confidence\":%.2f,\"triggers\":%s}",
                        reasoning.getActionId(),
                        scenario,
                        confidenceScore,
                        formatList(reasoning.getTriggers()))
        );
        offerEvent(event);
    }

    /**
     * Log action execution.
     */
    public void logActionExecution(String actionId, boolean success, long durationMs) {
        LogEvent event = new LogEvent(
                EventType.ACTION_EXECUTION,
                String.format("{\"action\":\"%s\",\"success\":%b,\"duration_ms\":%d}",
                        actionId, success, durationMs)
        );
        offerEvent(event);
    }

    /**
     * Log performance metrics.
     */
    public void logMetrics(double avgFps, double p95Frametime, int spikes) {
        LogEvent event = new LogEvent(
                EventType.METRICS,
                String.format("{\"avg_fps\":%.1f,\"p95_frametime\":%.2f,\"spikes\":%d}",
                        avgFps, p95Frametime, spikes)
        );
        offerEvent(event);
    }

    /**
     * Log scenario change.
     */
    public void logScenarioChange(Scenario oldScenario, Scenario newScenario, double confidence) {
        LogEvent event = new LogEvent(
                EventType.SCENARIO_CHANGE,
                String.format("{\"old\":\"%s\",\"new\":\"%s\",\"confidence\":%.2f}",
                        oldScenario, newScenario, confidence)
        );
        offerEvent(event);
    }

    /**
     * Log performance degradation.
     */
    public void logDegradation(String reason, double severity) {
        LogEvent event = new LogEvent(
                EventType.DEGRADATION,
                String.format("{\"reason\":\"%s\",\"severity\":%.2f}", reason, severity)
        );
        offerEvent(event);
    }

    /**
     * Shutdown logger.
     */
    public void shutdown() {
        running = false;
        writerThread.interrupt();
        try {
            writerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void offerEvent(LogEvent event) {
        if (!eventQueue.offer(event)) {
            // Queue full - log to main logger instead
            NozhConstants.LOGGER.warn("Event queue full, dropped: " + event);
        }
    }

    private void processQueue() {
        try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            while (running || !eventQueue.isEmpty()) {
                LogEvent event = eventQueue.poll();
                if (event == null) {
                    Thread.sleep(100);
                    continue;
                }

                String line = formatLogLine(event);
                writer.write(line);
                writer.newLine();
                writer.flush();
            }

        } catch (IOException | InterruptedException e) {
            NozhConstants.LOGGER.error("Event logger error", e);
        }
    }

    private String formatLogLine(LogEvent event) {
        String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(event.timestamp));
        return String.format("[%s] %s %s", timestamp, event.type, event.data);
    }

    private String formatList(java.util.List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        return "[\"" + String.join("\",\"", list) + "\"]";
    }

    private enum EventType {
        DECISION,
        ACTION_EXECUTION,
        METRICS,
        SCENARIO_CHANGE,
        DEGRADATION
    }

    private record LogEvent(EventType type, String data, long timestamp) {
        LogEvent(EventType type, String data) {
            this(type, data, System.currentTimeMillis());
        }
    }
}
