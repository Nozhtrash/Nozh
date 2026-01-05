package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.telemetry.TelemetryExportFormat;

import java.nio.file.Path;

/**
 * Report metadata for a benchmark session export.
 */
public record BenchmarkSessionReport(
        PerfSnapshot snapshot,
        long[] samplesNanos,
        Path reportPath,
        TelemetryExportFormat format,
        int windowSeconds,
        int capacity,
        long startedAtMillis) {
}
