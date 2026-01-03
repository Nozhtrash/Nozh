package dev.nozh.core.telemetry;

import dev.nozh.api.Bound;
import dev.nozh.api.PerfSnapshot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TelemetryExportWriter {

    private TelemetryExportWriter() {
    }

    public static Path write(PerfSnapshot snapshot, long[] samplesNanos, Path outputFile,
            TelemetryExportFormat format, Bound bound, PerfSnapshot tickSnapshot) throws Exception {
        if (format == TelemetryExportFormat.CSV) {
            Files.writeString(outputFile, toCsv(snapshot, samplesNanos, bound, tickSnapshot), StandardCharsets.UTF_8);
        } else if (format == TelemetryExportFormat.CSV_MINIMAL) {
            Files.writeString(outputFile, toCsvMinimal(snapshot, bound, tickSnapshot), StandardCharsets.UTF_8);
        } else if (format == TelemetryExportFormat.DEBUG) {
            Files.writeString(outputFile, toDebug(snapshot, samplesNanos, bound, tickSnapshot),
                    StandardCharsets.UTF_8);
        } else {
            Files.writeString(outputFile, toJson(snapshot, samplesNanos, bound, tickSnapshot), StandardCharsets.UTF_8);
        }
        return outputFile;
    }

    private static String toCsv(PerfSnapshot snapshot, long[] samplesNanos, Bound bound, PerfSnapshot tickSnapshot) {
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
        if (bound != null) {
            sb.append("bound,").append(bound.name()).append('\n');
        }
        if (tickSnapshot != null && tickSnapshot.sufficientData()) {
            sb.append("tick_avg_ms,").append(formatMetricForCsv(tickSnapshot.avgFrametimeMs())).append('\n');
            sb.append("tick_p95_ms,").append(formatMetricForCsv(tickSnapshot.p95FrametimeMs())).append('\n');
        }
        return sb.toString();
    }

    private static String toCsvMinimal(PerfSnapshot snapshot, Bound bound, PerfSnapshot tickSnapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("avg_ms,p95_ms,p99_ms,stddev_ms,spikes,samples,window_seconds,timestamp_ms,bound,tick_avg_ms,tick_p95_ms\n");
        sb.append(formatMetricForCsv(snapshot.avgFrametimeMs())).append(',')
                .append(formatMetricForCsv(snapshot.p95FrametimeMs())).append(',')
                .append(formatMetricForCsv(snapshot.p99FrametimeMs())).append(',')
                .append(formatMetricForCsv(snapshot.frametimeStddevMs())).append(',')
                .append(snapshot.spikeCount()).append(',')
                .append(snapshot.sampleCount()).append(',')
                .append(snapshot.windowSeconds()).append(',')
                .append(snapshot.timestampMillis()).append(',')
                .append(bound != null ? bound.name() : "UNKNOWN").append(',');
        if (tickSnapshot != null && tickSnapshot.sufficientData()) {
            sb.append(formatMetricForCsv(tickSnapshot.avgFrametimeMs())).append(',')
                    .append(formatMetricForCsv(tickSnapshot.p95FrametimeMs()));
        } else {
            sb.append("--,--");
        }
        sb.append('\n');
        return sb.toString();
    }

    private static String toJson(PerfSnapshot snapshot, long[] samplesNanos, Bound bound, PerfSnapshot tickSnapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"avgFrametimeMs\": ").append(formatMetricForJson(snapshot.avgFrametimeMs())).append(",\n");
        sb.append("  \"p95FrametimeMs\": ").append(formatMetricForJson(snapshot.p95FrametimeMs())).append(",\n");
        sb.append("  \"p99FrametimeMs\": ").append(formatMetricForJson(snapshot.p99FrametimeMs())).append(",\n");
        sb.append("  \"frametimeStddevMs\": ").append(formatMetricForJson(snapshot.frametimeStddevMs())).append(",\n");
        sb.append("  \"spikeCount\": ").append(snapshot.spikeCount()).append(",\n");
        sb.append("  \"sampleCount\": ").append(snapshot.sampleCount()).append(",\n");
        sb.append("  \"windowSeconds\": ").append(snapshot.windowSeconds()).append(",\n");
        sb.append("  \"timestampMillis\": ").append(snapshot.timestampMillis()).append(",\n");
        if (bound != null) {
            sb.append("  \"bound\": \"").append(bound.name()).append("\",\n");
        }
        if (tickSnapshot != null && tickSnapshot.sufficientData()) {
            sb.append("  \"tickAvgMs\": ").append(formatMetricForJson(tickSnapshot.avgFrametimeMs())).append(",\n");
            sb.append("  \"tickP95Ms\": ").append(formatMetricForJson(tickSnapshot.p95FrametimeMs())).append(",\n");
        }
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

    private static String toDebug(PerfSnapshot snapshot, long[] samplesNanos, Bound bound, PerfSnapshot tickSnapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("NOZH Telemetry Debug\n");
        sb.append("timestamp_ms=").append(snapshot.timestampMillis()).append('\n');
        sb.append("window_seconds=").append(snapshot.windowSeconds()).append('\n');
        sb.append("avg_ms=").append(formatMetricForCsv(snapshot.avgFrametimeMs())).append('\n');
        sb.append("p95_ms=").append(formatMetricForCsv(snapshot.p95FrametimeMs())).append('\n');
        sb.append("p99_ms=").append(formatMetricForCsv(snapshot.p99FrametimeMs())).append('\n');
        sb.append("stddev_ms=").append(formatMetricForCsv(snapshot.frametimeStddevMs())).append('\n');
        sb.append("spikes=").append(snapshot.spikeCount()).append('\n');
        sb.append("samples=").append(snapshot.sampleCount()).append('\n');
        sb.append("bound=").append(bound != null ? bound.name() : "UNKNOWN").append('\n');
        if (tickSnapshot != null && tickSnapshot.sufficientData()) {
            sb.append("tick_avg_ms=").append(formatMetricForCsv(tickSnapshot.avgFrametimeMs())).append('\n');
            sb.append("tick_p95_ms=").append(formatMetricForCsv(tickSnapshot.p95FrametimeMs())).append('\n');
        }
        sb.append("samples_ms=[");
        for (int i = 0; i < samplesNanos.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("%.3f", samplesNanos[i] / 1_000_000.0));
        }
        sb.append("]\n");
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
