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
                RenderPipelineSnapshot.empty(), StutterCause.unknown(),
                dev.nozh.core.profiler.PerfTraceSnapshot.empty(),
                dev.nozh.core.profiler.SpikeCausalityReport.unknown()), outputFile, format);
    }

    public static Path write(PerfReport report, Path outputFile, TelemetryExportFormat format) throws Exception {
        if (format == TelemetryExportFormat.CSV) {
            Files.writeString(outputFile, toCsv(report), StandardCharsets.UTF_8);
        } else if (format == TelemetryExportFormat.COMPACT_CSV) {
            Files.writeString(outputFile, toCompactCsv(report), StandardCharsets.UTF_8);
        } else if (format == TelemetryExportFormat.COMPACT_JSON) {
            Files.writeString(outputFile, toCompactJson(report), StandardCharsets.UTF_8);
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
        appendTraceCsv(sb, report);
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
        appendTraceJson(sb, report);
        sb.append("}\n");
        return sb.toString();
    }

    private static String toCompactCsv(PerfReport report) {
        PerfSnapshot snapshot = report.frameSnapshot();
        PerfSnapshot tickSnapshot = report.tickSnapshot();
        long[] samplesNanos = report.frameSamplesNanos();
        DeltaSamples delta = DeltaSamples.from(samplesNanos);
        StringBuilder sb = new StringBuilder();
        sb.append("samples_base_us,").append(delta.baseMicros()).append('\n');
        sb.append("index,delta_us\n");
        for (int i = 0; i < delta.deltaMicros().length; i++) {
            sb.append(i + 1).append(',').append(delta.deltaMicros()[i]).append('\n');
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
        appendTraceCsv(sb, report);
        return sb.toString();
    }

    private static String toCompactJson(PerfReport report) {
        PerfSnapshot snapshot = report.frameSnapshot();
        PerfSnapshot tickSnapshot = report.tickSnapshot();
        long[] samplesNanos = report.frameSamplesNanos();
        DeltaSamples delta = DeltaSamples.from(samplesNanos);
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
        sb.append("  \"samplesBaseUs\": ").append(delta.baseMicros()).append(",\n");
        sb.append("  \"samplesDeltaUs\": [");
        for (int i = 0; i < delta.deltaMicros().length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(delta.deltaMicros()[i]);
        }
        sb.append("],\n");
        appendDiagnosticsJson(sb, report);
        appendTraceJson(sb, report);
        sb.append("}\n");
        return sb.toString();
    }

    private record DeltaSamples(long baseMicros, long[] deltaMicros) {
        private static DeltaSamples from(long[] samplesNanos) {
            if (samplesNanos == null || samplesNanos.length == 0) {
                return new DeltaSamples(0, new long[0]);
            }
            long baseMicros = nanosToMicros(samplesNanos[0]);
            long[] deltaMicros = new long[Math.max(0, samplesNanos.length - 1)];
            long previous = samplesNanos[0];
            for (int i = 1; i < samplesNanos.length; i++) {
                long current = samplesNanos[i];
                deltaMicros[i - 1] = nanosToMicros(current - previous);
                previous = current;
            }
            return new DeltaSamples(baseMicros, deltaMicros);
        }
    }

    private static void appendDiagnosticsCsv(StringBuilder sb, PerfReport report) {
        GcMetricsSnapshot gc = report.gcMetrics();
        FramePauseSnapshot pauses = report.pauses();
        RenderPipelineSnapshot pipeline = report.renderPipeline();
        StutterCause cause = report.stutterCause();
        RenderPhaseMetrics hottest = pipeline != null ? pipeline.hottestPhase() : null;
        dev.nozh.core.profiler.SpikeCausalityReport spikeCause = report.spikeCausality();

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
        sb.append("spike_cause,").append(spikeCause != null ? spikeCause.cause().name() : "UNKNOWN").append('\n');
        sb.append("spike_confidence,")
                .append(formatMetricForCsvAllowZero(spikeCause != null ? spikeCause.confidence() : 0.0))
                .append('\n');
    }

    private static void appendDiagnosticsJson(StringBuilder sb, PerfReport report) {
        GcMetricsSnapshot gc = report.gcMetrics();
        FramePauseSnapshot pauses = report.pauses();
        RenderPipelineSnapshot pipeline = report.renderPipeline();
        StutterCause cause = report.stutterCause();
        RenderPhaseMetrics hottest = pipeline != null ? pipeline.hottestPhase() : null;
        dev.nozh.core.profiler.SpikeCausalityReport spikeCause = report.spikeCausality();

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
                .append(formatMetricForJsonAllowZero(hottest != null ? hottest.maxMs() : 0.0)).append(",\n");
        sb.append("    \"spikeCause\": \"")
                .append(spikeCause != null ? spikeCause.cause().name() : "UNKNOWN").append("\",\n");
        sb.append("    \"spikeConfidence\": ")
                .append(formatMetricForJsonAllowZero(spikeCause != null ? spikeCause.confidence() : 0.0))
                .append("\n");
        sb.append("  },\n");
    }

    private static void appendTraceCsv(StringBuilder sb, PerfReport report) {
        dev.nozh.core.profiler.PerfTraceSnapshot trace = report.traceSnapshot();
        if (trace == null || trace.events().isEmpty()) {
            return;
        }
        sb.append("\n");
        sb.append("trace_timestamp_ms,trace_type,trace_duration_ms,trace_detail,trace_category,trace_severity\n");
        for (dev.nozh.core.profiler.PerfTraceEvent event : trace.events()) {
            sb.append(event.timestampMillis()).append(',')
                    .append(event.type()).append(',')
                    .append(formatMetricForCsvAllowZero(event.durationMs())).append(',')
                    .append(event.detail() != null ? event.detail() : "").append(',')
                    .append(event.category() != null ? event.category() : "").append(',')
                    .append(event.severity() != null ? event.severity() : "").append('\n');
        }
    }

    private static void appendTraceJson(StringBuilder sb, PerfReport report) {
        dev.nozh.core.profiler.PerfTraceSnapshot trace = report.traceSnapshot();
        sb.append("  \"trace\": {\n");
        if (trace == null) {
            sb.append("    \"windowStartMillis\": 0,\n");
            sb.append("    \"windowEndMillis\": 0,\n");
            sb.append("    \"events\": []\n");
            sb.append("  }\n");
            return;
        }
        sb.append("    \"windowStartMillis\": ").append(trace.windowStartMillis()).append(",\n");
        sb.append("    \"windowEndMillis\": ").append(trace.windowEndMillis()).append(",\n");
        sb.append("    \"events\": [");
        if (!trace.events().isEmpty()) {
            sb.append('\n');
        }
        for (int i = 0; i < trace.events().size(); i++) {
            dev.nozh.core.profiler.PerfTraceEvent event = trace.events().get(i);
            sb.append("      {")
                    .append("\"timestampMillis\": ").append(event.timestampMillis()).append(", ")
                    .append("\"type\": \"").append(event.type()).append("\", ")
                    .append("\"durationMs\": ").append(formatMetricForJsonAllowZero(event.durationMs())).append(", ")
                    .append("\"detail\": \"").append(escapeJson(event.detail())).append("\", ")
                    .append("\"category\": \"").append(escapeJson(event.category())).append("\", ")
                    .append("\"severity\": \"").append(escapeJson(event.severity())).append("\"")
                    .append("}");
            if (i < trace.events().size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("    ]\n");
        sb.append("  }\n");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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

    private static long nanosToMicros(long nanos) {
        return Math.round(nanos / 1000.0);
    }
}
