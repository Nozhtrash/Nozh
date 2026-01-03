package dev.nozh.core.telemetry;

import dev.nozh.api.PerfSnapshot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TelemetryExportWriter {

    private TelemetryExportWriter() {
    }

    public static Path write(PerfSnapshot snapshot, long[] samplesNanos, Path outputFile,
            TelemetryExportFormat format) throws Exception {
        if (format == TelemetryExportFormat.CSV) {
            Files.writeString(outputFile, toCsv(snapshot, samplesNanos), StandardCharsets.UTF_8);
        } else {
            Files.writeString(outputFile, toJson(snapshot, samplesNanos), StandardCharsets.UTF_8);
        }
        return outputFile;
    }

    private static String toCsv(PerfSnapshot snapshot, long[] samplesNanos) {
        StringBuilder sb = new StringBuilder();
        sb.append("index,frametime_ms\n");
        for (int i = 0; i < samplesNanos.length; i++) {
            double ms = samplesNanos[i] / 1_000_000.0;
            sb.append(i).append(',').append(String.format("%.3f", ms)).append('\n');
        }
        sb.append("\n");
        sb.append("avg_ms,").append(formatMetricForCsv(snapshot.avgFrametimeMs())).append('\n');
        sb.append("p95_ms,").append(formatMetricForCsv(snapshot.p95FrametimeMs())).append('\n');
        sb.append("spikes,").append(snapshot.spikeCount()).append('\n');
        sb.append("samples,").append(snapshot.sampleCount()).append('\n');
        sb.append("window_seconds,").append(snapshot.windowSeconds()).append('\n');
        sb.append("timestamp_ms,").append(snapshot.timestampMillis()).append('\n');
        return sb.toString();
    }

    private static String toJson(PerfSnapshot snapshot, long[] samplesNanos) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"avgFrametimeMs\": ").append(formatMetricForJson(snapshot.avgFrametimeMs())).append(",\n");
        sb.append("  \"p95FrametimeMs\": ").append(formatMetricForJson(snapshot.p95FrametimeMs())).append(",\n");
        sb.append("  \"spikeCount\": ").append(snapshot.spikeCount()).append(",\n");
        sb.append("  \"sampleCount\": ").append(snapshot.sampleCount()).append(",\n");
        sb.append("  \"windowSeconds\": ").append(snapshot.windowSeconds()).append(",\n");
        sb.append("  \"timestampMillis\": ").append(snapshot.timestampMillis()).append(",\n");
        sb.append("  \"samplesMs\": [");
        for (int i = 0; i < samplesNanos.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("%.3f", samplesNanos[i] / 1_000_000.0));
        }
        sb.append("]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String formatMetricForCsv(double value) {
        if (Double.isNaN(value) || value <= 0) {
            return "--";
        }
        return String.format("%.3f", value);
    }

    private static String formatMetricForJson(double value) {
        if (Double.isNaN(value) || value <= 0) {
            return "\"--\"";
        }
        return String.format("%.3f", value);
    }
}
