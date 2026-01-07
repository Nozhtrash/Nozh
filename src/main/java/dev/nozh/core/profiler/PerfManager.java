package dev.nozh.core.profiler;

import dev.nozh.NozhConstants;
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
import java.util.function.Supplier;

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
    private final SpikeTrendPredictor spikePredictor;
    private final DecisionLatencyEvaluator decisionLatencyEvaluator;
    private final GcPauseWatcher gcPauseWatcher;
    private final FramePauseTracker pauseTracker;
    private final RenderPipelineTracer renderPipelineTracer;
    private final StutterCauseAnalyzer stutterCauseAnalyzer;
    private long lastWindowAdjustMillis = 0L;
    private Supplier<PerfSnapshot> tickSnapshotSupplier = null;

    public PerfManager() {
        // Calculate capacity based on strict rules
        NozhConfig config = ConfigManager.getConfig();
        this.windowSeconds = 5; // Default window
        this.windowController = new PerfWindowController(3, 10);
        this.spikePredictor = new SpikeTrendPredictor();
        this.decisionLatencyEvaluator = new DecisionLatencyEvaluator();
        this.gcPauseWatcher = new GcPauseWatcher();
        this.pauseTracker = new FramePauseTracker();
        this.renderPipelineTracer = new RenderPipelineTracer();
        this.stutterCauseAnalyzer = new StutterCauseAnalyzer();

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
        gcPauseWatcher.update();
    }

    public PerfSnapshot getSnapshot() {
        PerfSnapshot snapshot = stats.snapshot();
        spikePredictor.update(snapshot);
        adjustWindowIfNeeded(snapshot);
        return snapshot;
    }

    public void setObservationWindowSeconds(int newWindowSeconds) {
        if (newWindowSeconds <= 0 || newWindowSeconds == windowSeconds) {
            return;
        }

        NozhConfig config = ConfigManager.getConfig();
        int targetFps = Math.max(30, config.targetFps);
        int capacity = calculateCapacity(targetFps, newWindowSeconds);
        windowSeconds = newWindowSeconds;
        stats = new RollingWindowStats(capacity, newWindowSeconds);
        sampler = new FrameTimeSampler(stats);
        lastWindowAdjustMillis = System.currentTimeMillis();
        NozhConstants.LOGGER.debug("PerfManager observation window set to {}s (capacity={})", newWindowSeconds, capacity);
    }

    public Path exportTelemetry(Path outputDir, TelemetryExportFormat format) throws Exception {
        Files.createDirectories(outputDir);
        PerfSnapshot snapshot = stats.snapshot();
        long[] samples = stats.snapshotSamplesNanos();
        PerfSnapshot tickSnapshot = resolveTickSnapshot();
        GcMetricsSnapshot gcSnapshot = buildGcSnapshot();
        FramePauseSnapshot pauses = pauseTracker.snapshot();
        RenderPipelineSnapshot renderSnapshot = renderPipelineTracer.snapshot();
        PerfReport report = new PerfReport(
                snapshot,
                tickSnapshot,
                samples,
                pauses,
                gcSnapshot,
                renderSnapshot,
                stutterCauseAnalyzer.analyze(snapshot, tickSnapshot, gcSnapshot,
                        pauses, renderSnapshot));
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(snapshot.timestampMillis()));
        String extension = format == TelemetryExportFormat.CSV ? "csv" : "json";
        Path outputFile = outputDir.resolve("telemetry_" + timestamp + "." + extension);
        return TelemetryExportWriter.write(report, outputFile, format);
    }

    public void reset() {
        sampler.reset();
        spikePredictor.reset();
    }

    public SpikePrediction getSpikePrediction() {
        return spikePredictor.getLastPrediction();
    }

    public long startDecisionTimer() {
        return decisionLatencyEvaluator.startTimer();
    }

    public void recordDecisionLatency(long startNanos) {
        decisionLatencyEvaluator.recordLatency(startNanos);
    }

    public boolean isDecisionWithinBudget(long startNanos, int budgetMs) {
        return decisionLatencyEvaluator.isWithinBudget(startNanos, budgetMs);
    }

    public long getLastDecisionLatencyMs() {
        return decisionLatencyEvaluator.getLastDecisionLatencyMs();
    }

    public DecisionLatencyStats getDecisionLatencyStats() {
        return decisionLatencyEvaluator.snapshot();
    }

    public void onRenderPhaseStart(RenderPhase phase) {
        renderPipelineTracer.beginPhase(phase);
    }

    public void onRenderPhaseEnd(RenderPhase phase) {
        renderPipelineTracer.endPhase(phase);
    }

    public void onRenderFrameStart() {
        renderPipelineTracer.onFrameStart();
    }

    public void onRenderFrameEnd() {
        long frameDuration = renderPipelineTracer.onFrameEnd();
        if (frameDuration > 0) {
            pauseTracker.recordFrameDuration(frameDuration);
        }
    }

    public void setTickSnapshotSupplier(Supplier<PerfSnapshot> tickSnapshotSupplier) {
        this.tickSnapshotSupplier = tickSnapshotSupplier;
    }

    public PerfDiagnosticsSnapshot getDiagnosticsSnapshot() {
        PerfSnapshot tickSnapshot = resolveTickSnapshot();
        GcMetricsSnapshot gcSnapshot = buildGcSnapshot();
        FramePauseSnapshot pauses = pauseTracker.snapshot();
        RenderPipelineSnapshot renderSnapshot = renderPipelineTracer.snapshot();
        StutterCause cause = stutterCauseAnalyzer.analyze(stats.snapshot(), tickSnapshot,
                gcSnapshot, pauses, renderSnapshot);
        RenderPhaseMetrics hottest = renderSnapshot.hottestPhase();
        String hottestKey = hottest != null && hottest.phase() != null
                ? hottest.phase().translationKey()
                : RenderPhase.UNKNOWN.translationKey();
        return new PerfDiagnosticsSnapshot(
                gcSnapshot.recentGcMs(),
                gcSnapshot.pressureScore(),
                pauses.pauseCount(),
                pauses.maxPauseMs(),
                cause.causeKey(),
                cause.detail(),
                cause.confidence(),
                hottestKey,
                hottest != null ? hottest.maxMs() : 0.0,
                hottest != null ? hottest.ticks() : 0);
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

    private PerfSnapshot resolveTickSnapshot() {
        if (tickSnapshotSupplier == null) {
            return PerfSnapshot.empty();
        }
        PerfSnapshot snapshot = tickSnapshotSupplier.get();
        return snapshot != null ? snapshot : PerfSnapshot.empty();
    }

    private GcMetricsSnapshot buildGcSnapshot() {
        return new GcMetricsSnapshot(
                gcPauseWatcher.getRecentGcMs(),
                gcPauseWatcher.getGcPressureScore(),
                gcPauseWatcher.isGcCausingPauses());
    }
}
