package dev.nozh.core.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Export telemetry data for external analysis.
 * Supports CSV, JSON, and Prometheus formats.
 * 
 * INTEGRATION: Telemetry and monitoring
 * CONTRACT: Thread-safe file I/O
 */
public final class TelemetryExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Export format.
     */
    public enum ExportFormat {
        CSV,
        JSON,
        PROMETHEUS
    }

    /**
     * Telemetry data point.
     */
    public record DataPoint(
        long timestamp,
        double fps,
        double frametime,
        String scenario,
        String action
    ) {}

    private final List<DataPoint> sessionData = new CopyOnWriteArrayList<>();
    private final AtomicBoolean continuousExportActive = new AtomicBoolean(false);
    private volatile Thread exportThread;

    /**
     * Record a data point.
     */
    public void record(long timestamp, double fps, double frametime, String scenario, String action) {
        sessionData.add(new DataPoint(timestamp, fps, frametime, scenario, action));
    }

    /**
     * Export current session data.
     */
    public void exportSession(Path outputPath, ExportFormat format) throws IOException {
        String content = switch (format) {
            case CSV -> exportToCsv();
            case JSON -> exportToJson();
            case PROMETHEUS -> exportToPrometheus();
        };

        Files.writeString(outputPath, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Start continuous export (writes incrementally).
     */
    public void startContinuousExport(Path outputPath, long intervalMs) {
        if (continuousExportActive.get()) {
            throw new IllegalStateException("Continuous export already active");
        }

        continuousExportActive.set(true);
        exportThread = new Thread(() -> {
            while (continuousExportActive.get()) {
                try {
                    Thread.sleep(intervalMs);
                    exportSession(outputPath, ExportFormat.CSV);
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    // Log error but continue
                }
            }
        }, "TelemetryExporter");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    /**
     * Stop continuous export.
     */
    public void stopContinuousExport() {
        continuousExportActive.set(false);
        if (exportThread != null) {
            exportThread.interrupt();
        }
    }

    /**
     * Get Prometheus-compatible metrics.
     */
    public String getPrometheusMetrics() {
        if (sessionData.isEmpty()) {
            return "# No data available\n";
        }

        StringBuilder metrics = new StringBuilder();
        metrics.append("# HELP nozh_fps Current frames per second\n");
        metrics.append("# TYPE nozh_fps gauge\n");
        
        DataPoint latest = sessionData.get(sessionData.size() - 1);
        metrics.append(String.format("nozh_fps %.2f %d\n", latest.fps(), latest.timestamp()));

        metrics.append("# HELP nozh_frametime Current frame time in milliseconds\n");
        metrics.append("# TYPE nozh_frametime gauge\n");
        metrics.append(String.format("nozh_frametime %.2f %d\n", latest.frametime(), latest.timestamp()));

        metrics.append("# HELP nozh_samples Total number of samples collected\n");
        metrics.append("# TYPE nozh_samples counter\n");
        metrics.append(String.format("nozh_samples %d\n", sessionData.size()));

        return metrics.toString();
    }

    /**
     * Clear all session data.
     */
    public void clear() {
        sessionData.clear();
    }

    /**
     * Get data point count.
     */
    public int getDataPointCount() {
        return sessionData.size();
    }

    private String exportToCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("timestamp,fps,frametime_ms,scenario,action\n");

        for (DataPoint point : sessionData) {
            csv.append(String.format("%d,%.2f,%.2f,%s,%s\n",
                point.timestamp(),
                point.fps(),
                point.frametime(),
                escapeCsv(point.scenario()),
                escapeCsv(point.action())
            ));
        }

        return csv.toString();
    }

    private String exportToJson() {
        return GSON.toJson(new ArrayList<>(sessionData));
    }

    private String exportToPrometheus() {
        return getPrometheusMetrics();
    }

    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
