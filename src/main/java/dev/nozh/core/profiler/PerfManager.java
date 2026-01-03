package dev.nozh.core.profiler;

import dev.nozh.NozhConstants;
import dev.nozh.api.Bound;
import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.safety.CrashLoopGuard;
import dev.nozh.core.telemetry.TelemetryExportFormat;
import dev.nozh.core.telemetry.TelemetryExportWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Orchestrator for performance profiling.
 * 
 * Responsibilities:
 * - Owns Sampler and Stats
 * - Manages lifecycle (reset, update)
 * - Capacity calculation
 * - Exposes Thread-Safe snapshots
 */
public class PerfManager {

    private FrameTimeSampler sampler;
    private RollingWindowStats stats;
    private int windowSeconds;
    private final PerfWindowController windowController;
    private long lastWindowAdjustMillis = 0L;
    private Bound lastBound = Bound.UNKNOWN;
    private PerfSnapshot lastTickSnapshot = PerfSnapshot.empty();

    public PerfManager() {
        // Calculate capacity based on strict rules
        NozhConfig config = ConfigManager.getConfig();
        this.windowSeconds = 5; // Default window
        this.windowController = new PerfWindowController(3, 10);

        int targetFps = Math.max(30, config.targetFps);
        int capacity = calculateCapacity(targetFps, windowSeconds);
        this.stats = new RollingWindowStats(capacity, windowSeconds);
        this.sampler = new FrameTimeSampler(stats);

        NozhConstants.LOGGER.info("PerfManager initialized. Capacity={} ({}s @ {}fps)",
                capacity, windowSeconds, targetFps);
    }

    /**
     * Called once per frame ONLY if enabled.
     */
    public void onFrame() {
        // Check enabled state efficiently
        if (ConfigManager.getConfig().enabled && !CrashLoopGuard.isInSafeMode()) {
            sampler.onFrame();
        } else if (CrashLoopGuard.isInSafeMode()) {
            // Safe mode shouldn't block measurement according to prompt?
            // Prompt says: "Safe mode NO bloquea medición"
            // Prompt says: "Safe mode BLOCKS ACTIONS, NOT MEASUREMENT."
            // Correcting logic:
            sampler.onFrame();
        }
    }

    public PerfSnapshot getSnapshot() {
        PerfSnapshot snapshot = stats.snapshot();
        adjustWindowIfNeeded(snapshot);
        return snapshot;
    }

    public Path exportTelemetry(Path outputDir, TelemetryExportFormat format) throws Exception {
        Files.createDirectories(outputDir);
        PerfSnapshot snapshot = stats.snapshot();
        long[] samples = stats.snapshotSamplesNanos();
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(snapshot.timestampMillis()));
        String extension = switch (format) {
            case CSV, CSV_MINIMAL -> "csv";
            case DEBUG -> "txt";
            default -> "json";
        };
        Path outputFile = outputDir.resolve("telemetry_" + timestamp + "." + extension);
        return TelemetryExportWriter.write(snapshot, samples, outputFile, format, lastBound, lastTickSnapshot);
    }

    public void reset() {
        sampler.reset();
    }

    public void updateDiagnostics(PerfSnapshot frameSnapshot, PerfSnapshot tickSnapshot) {
        if (tickSnapshot != null) {
            lastTickSnapshot = tickSnapshot;
        }
        if (frameSnapshot != null) {
            lastBound = estimateBound(frameSnapshot, tickSnapshot);
        }
    }

    public Bound getLastBound() {
        return lastBound;
    }

    public PerfSnapshot getLastTickSnapshot() {
        return lastTickSnapshot;
    }

    public double[] getRecentSamplesMs(int maxSamples) {
        long[] samples = stats.snapshotSamplesNanos();
        int count = Math.min(samples.length, Math.max(0, maxSamples));
        double[] ms = new double[count];
        int start = samples.length - count;
        for (int i = 0; i < count; i++) {
            ms[i] = samples[start + i] / 1_000_000.0;
        }
        return ms;
    }

    private void adjustWindowIfNeeded(PerfSnapshot snapshot) {
        long now = System.currentTimeMillis();
        if (now - lastWindowAdjustMillis < 1000) {
            return;
        }

        int newWindowSeconds = windowController.evaluate(snapshot, windowSeconds, now);
        if (newWindowSeconds != windowSeconds) {
            NozhConfig config = ConfigManager.getConfig();
            int targetFps = Math.max(30, config.targetFps);
            int capacity = calculateCapacity(targetFps, newWindowSeconds);
            windowSeconds = newWindowSeconds;
            stats = new RollingWindowStats(capacity, newWindowSeconds);
            sampler = new FrameTimeSampler(stats);
            NozhConstants.LOGGER.debug("PerfManager window adjusted to {}s (capacity={})", newWindowSeconds, capacity);
        }
        lastWindowAdjustMillis = now;
    }

    private int calculateCapacity(int targetFps, int windowSeconds) {
        int calcCapacity = targetFps * windowSeconds;
        return Math.max(60, Math.min(calcCapacity, 600));
    }

    private Bound estimateBound(PerfSnapshot frameSnapshot, PerfSnapshot tickSnapshot) {
        if (frameSnapshot == null) {
            return Bound.UNKNOWN;
        }
        double avgMs = frameSnapshot.avgFrametimeMs();
        double p95Ms = frameSnapshot.p95FrametimeMs();
        boolean frameData = frameSnapshot.sufficientData() && avgMs > 0 && p95Ms > 0;
        boolean tickData = tickSnapshot != null && tickSnapshot.sufficientData()
                && tickSnapshot.avgFrametimeMs() > 0 && tickSnapshot.p95FrametimeMs() > 0;

        if (!frameData && !tickData) {
            return Bound.UNKNOWN;
        }

        if (!tickData) {
            if (avgMs > 16.67) {
                return Bound.CPU_BOUND;
            }
            if (p95Ms > avgMs * 1.5) {
                return Bound.GPU_BOUND;
            }
            return Bound.UNKNOWN;
        }

        double tickAvgMs = tickSnapshot.avgFrametimeMs();
        double tickP95Ms = tickSnapshot.p95FrametimeMs();
        boolean tickHigh = tickAvgMs > 50.0 || tickP95Ms > 50.0;
        boolean frameHigh = frameData && (avgMs > 16.67 || p95Ms > 16.67);

        if (tickHigh && frameHigh) {
            return Bound.MIXED;
        }
        if (tickHigh) {
            return Bound.CPU_BOUND;
        }
        if (frameHigh) {
            return Bound.GPU_BOUND;
        }
        return Bound.UNKNOWN;
    }
}
