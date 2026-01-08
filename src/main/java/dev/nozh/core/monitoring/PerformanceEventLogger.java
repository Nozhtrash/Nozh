package dev.nozh.core.monitoring;

import dev.nozh.NozhConstants;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * Logs performance events to file.
 * Extended to support action execution logging.
 */
public class PerformanceEventLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final Path logPath;
    private final ExecutorService logExecutor;
    private BufferedWriter writer;
    
    public PerformanceEventLogger(Path logPath) {
        this.logPath = logPath;
        this.logExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PerformanceEventLogger");
            t.setDaemon(true);
            return t;
        });
        
        try {
            Files.createDirectories(logPath.getParent());
            this.writer = Files.newBufferedWriter(logPath, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            NozhConstants.LOGGER.error("Failed to initialize performance logger", e);
        }
    }
    
    public void logMetrics(double avgFps, double p95Frametime, int spikeCount) {
        logEvent(String.format("METRICS | FPS: %.1f | P95: %.1fms | Spikes: %d",
                avgFps, p95Frametime, spikeCount));
    }
    
    public void logActionExecution(String actionId, boolean success, long durationMs) {
        logEvent(String.format("ACTION | %s | %s | %dms",
                actionId, success ? "SUCCESS" : "FAILED", durationMs));
    }
    
    private void logEvent(String message) {
        if (writer == null) {
            return;
        }
        
        logExecutor.submit(() -> {
            try {
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                writer.write(String.format("[%s] %s%n", timestamp, message));
                writer.flush();
            } catch (IOException e) {
                NozhConstants.LOGGER.error("Failed to write log", e);
            }
        });
    }
    
    public void shutdown() {
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to shutdown logger", e);
        }
    }
}
