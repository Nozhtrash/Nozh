package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.telemetry.TelemetryExportFormat;
import dev.nozh.core.telemetry.TelemetryExportWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Controlled-input benchmark session for performance reports.
 *
 * Allows tests and tooling to inject deterministic frametime samples and
 * generate an exportable report without relying on live frame hooks.
 */
public final class BenchmarkSession {

    private static final int MIN_CAPACITY = 60;
    private static final int MAX_CAPACITY = 600;

    private final RollingWindowStats stats;
    private final int windowSeconds;
    private final int capacity;
    private final long startedAtMillis;

    public BenchmarkSession(int targetFps, int windowSeconds) {
        if (targetFps <= 0) {
            throw new IllegalArgumentException("targetFps must be positive");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        this.windowSeconds = windowSeconds;
        this.capacity = calculateCapacity(targetFps, windowSeconds);
        this.stats = new RollingWindowStats(capacity, windowSeconds);
        this.startedAtMillis = System.currentTimeMillis();
    }

    public void addSampleNanos(long nanos) {
        if (nanos <= 0) {
            return;
        }
        stats.addSample(nanos);
    }

    public void addSampleMillis(double millis) {
        if (!Double.isFinite(millis) || millis <= 0.0) {
            return;
        }
        stats.addSample((long) (millis * 1_000_000.0));
    }

    public void addSamplesNanos(long[] samples) {
        if (samples == null) {
            return;
        }
        for (long sample : samples) {
            addSampleNanos(sample);
        }
    }

    public PerfSnapshot snapshot() {
        return stats.snapshot();
    }

    public long[] snapshotSamplesNanos() {
        return stats.snapshotSamplesNanos();
    }

    public BenchmarkSessionReport exportReport(Path outputDir, TelemetryExportFormat format, String label)
            throws Exception {
        Objects.requireNonNull(outputDir, "outputDir");
        Objects.requireNonNull(format, "format");
        Files.createDirectories(outputDir);
        PerfSnapshot snapshot = stats.snapshot();
        long[] samples = stats.snapshotSamplesNanos();
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(snapshot.timestampMillis()));
        String sanitizedLabel = label != null && !label.isBlank() ? label.trim().replaceAll("\\s+", "_") : "session";
        String extension = format == TelemetryExportFormat.CSV ? "csv" : "json";
        Path outputFile = outputDir.resolve("benchmark_" + sanitizedLabel + "_" + timestamp + "." + extension);
        TelemetryExportWriter.write(snapshot, samples, outputFile, format);
        return new BenchmarkSessionReport(snapshot, samples, outputFile, format, windowSeconds, capacity, startedAtMillis);
    }

    private int calculateCapacity(int targetFps, int windowSeconds) {
        int calcCapacity = targetFps * windowSeconds;
        return Math.max(MIN_CAPACITY, Math.min(calcCapacity, MAX_CAPACITY));
    }
}
