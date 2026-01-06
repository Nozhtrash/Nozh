package dev.nozh.core.telemetry;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.profiler.FramePauseSnapshot;
import dev.nozh.core.profiler.GcMetricsSnapshot;
import dev.nozh.core.profiler.PerfReport;
import dev.nozh.core.profiler.RenderPhaseMetrics;
import dev.nozh.core.profiler.RenderPipelineSnapshot;
import dev.nozh.core.profiler.StutterCause;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TelemetryExportWriter {

    private TelemetryExportWriter() {
    }

    public static Path write(PerfSnapshot snapshot, long[] samplesNanos, Path outputFile,
            TelemetryExportFormat format) throws Exception {
        return write(new PerfReport(snapshot, PerfSnapshot.empty(), samplesNanos,
                FramePauseSnapshot.empty(), GcMetricsSnapshot.empty(),
                RenderPipelineSnapshot.empty(), StutterCause.unknown()), outputFile, format);
    }

    public static Path write(PerfReport report, Path outputFile, TelemetryExportFormat format) throws Exception {
        if (format == TelemetryExportFormat.CSV) {
            Files.writeString(outputFile, toCsv(report), StandardCharsets.UTF_8);
        } else {
            Files.writeString(outputFile, toJson(report), StandardCharsets.UTF_8);
        }
        return outputFile;
    }

    private static String toCsv(PerfReport report) {
        PerfSnapshot snapshot = report.frameSnapshot();
        PerfSnapshot tickSnapshot = report.tickSnapshot();
        long[] samplesNanos = report.frameSamplesNanos();
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
        sb.append("\n");
        sb.append("tick_avg_ms,").append(formatMetricForCsv(tickSnapshot.avgFrametimeMs())).append('\n');
        sb.append("tick_p95_ms,").append(formatMetricForCsv(tickSnapshot.p95FrametimeMs())).append('\n');
        sb.append("tick_samples,").append(tickSnapshot.sampleCount()).append('\n');
        sb.append("tick_window_seconds,").append(tickSnapshot.windowSeconds()).append('\n');
        appendDiagnosticsCsv(sb, report);
        return sb.toString();
    }

    private static String toJson(PerfReport report) {
        PerfSnapshot snapshot = report.frameSnapshot();
        PerfSnapshot tickSnapshot = report.tickSnapshot();
        long[] samplesNanos = report.frameSamplesNanos();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"frame\": {\n");
        sb.append("    \"avgFrametimeMs\": ").append(formatMetricForJson(snapshot.avgFrametimeMs())).append(",\n");
        sb.append("    \"p95FrametimeMs\": ").append(formatMetricForJson(snapshot.p95FrametimeMs())).append(",\n");
        sb.append("    \"spikeCount\": ").append(snapshot.spikeCount()).append(",\n");
        sb.append("    \"sampleCount\": ").append(snapshot.sampleCount()).append(",\n");
        sb.append("    \"windowSeconds\": ").append(snapshot.windowSeconds()).append(",\n");
        sb.append("    \"timestampMillis\": ").append(snapshot.timestampMillis()).append("\n");
        sb.append("  },\n");
        sb.append("  \"tick\": {\n");
        sb.append("    \"avgTickMs\": ").append(formatMetricForJson(tickSnapshot.avgFrametimeMs())).append(",\n");
        sb.append("    \"p95TickMs\": ").append(formatMetricForJson(tickSnapshot.p95FrametimeMs())).append(",\n");
        sb.append("    \"sampleCount\": ").append(tickSnapshot.sampleCount()).append(",\n");
        sb.append("    \"windowSeconds\": ").append(tickSnapshot.windowSeconds()).append("\n");
        sb.append("  },\n");
        sb.append("  \"samplesMs\": [");
        for (int i = 0; i < samplesNanos.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format("%.3f", samplesNanos[i] / 1_000_000.0));
        }
        sb.append("],\n");
        appendDiagnosticsJson(sb, report);
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendDiagnosticsCsv(StringBuilder sb, PerfReport report) {
        GcMetricsSnapshot gc = report.gcMetrics();
        FramePauseSnapshot pauses = report.pauses();
        RenderPipelineSnapshot pipeline = report.renderPipeline();
        StutterCause cause = report.stutterCause();
        RenderPhaseMetrics hottest = pipeline != null ? pipeline.hottestPhase() : null;

        sb.append("gc_recent_ms,").append(formatMetricForCsvAllowZero(gc != null ? gc.recentGcMs() : 0.0))
                .append('\n');
        sb.append("gc_pressure,").append(formatMetricForCsvAllowZero(gc != null ? gc.pressureScore() : 0.0))
                .append('\n');
        sb.append("pause_count,").append(pauses != null ? pauses.pauseCount() : 0).append('\n');
        sb.append("pause_max_ms,").append(formatMetricForCsvAllowZero(pauses != null ? pauses.maxPauseMs() : 0.0))
                .append('\n');
        sb.append("stutter_cause,").append(cause != null ? cause.causeKey() : "nozh.hud.stutter.unknown").append('\n');
        sb.append("stutter_confidence,").append(formatMetricForCsvAllowZero(cause != null ? cause.confidence() : 0.0))
                .append('\n');
        sb.append("hottest_phase,").append(hottest != null && hottest.phase() != null
                ? hottest.phase().name()
                : "UNKNOWN").append('\n');
        sb.append("hottest_phase_max_ms,")
                .append(formatMetricForCsvAllowZero(hottest != null ? hottest.maxMs() : 0.0)).append('\n');
    }

    private static void appendDiagnosticsJson(StringBuilder sb, PerfReport report) {
        GcMetricsSnapshot gc = report.gcMetrics();
        FramePauseSnapshot pauses = report.pauses();
        RenderPipelineSnapshot pipeline = report.renderPipeline();
        StutterCause cause = report.stutterCause();
        RenderPhaseMetrics hottest = pipeline != null ? pipeline.hottestPhase() : null;

        sb.append("  \"diagnostics\": {\n");
        sb.append("    \"gcRecentMs\": ").append(formatMetricForJsonAllowZero(gc != null ? gc.recentGcMs() : 0.0))
                .append(",\n");
        sb.append("    \"gcPressure\": ").append(formatMetricForJsonAllowZero(gc != null ? gc.pressureScore() : 0.0))
                .append(",\n");
        sb.append("    \"pauseCount\": ").append(pauses != null ? pauses.pauseCount() : 0).append(",\n");
        sb.append("    \"pauseMaxMs\": ").append(formatMetricForJsonAllowZero(pauses != null ? pauses.maxPauseMs() : 0.0))
                .append(",\n");
        sb.append("    \"stutterCause\": \"")
                .append(cause != null ? cause.causeKey() : "nozh.hud.stutter.unknown").append("\",\n");
        sb.append("    \"stutterConfidence\": ")
                .append(formatMetricForJsonAllowZero(cause != null ? cause.confidence() : 0.0)).append(",\n");
        sb.append("    \"hottestPhase\": \"")
                .append(hottest != null && hottest.phase() != null ? hottest.phase().name() : "UNKNOWN")
                .append("\",\n");
        sb.append("    \"hottestPhaseMaxMs\": ")
                .append(formatMetricForJsonAllowZero(hottest != null ? hottest.maxMs() : 0.0)).append("\n");
        sb.append("  }\n");
    }

    private static String formatMetricForCsvAllowZero(double value) {
        if (Double.isNaN(value) || value < 0) {
            return "--";
        }
        return String.format("%.3f", value);
    }

    private static String formatMetricForJsonAllowZero(double value) {
        if (Double.isNaN(value) || value < 0) {
            return "\"--\"";
        }
        return String.format("%.3f", value);
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
