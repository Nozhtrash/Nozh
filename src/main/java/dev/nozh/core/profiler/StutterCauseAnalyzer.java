package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;

public class StutterCauseAnalyzer {

    private static final double GC_PAUSE_MS = 40.0;
    private static final double TICK_P95_MS = 50.0;
    private static final double FRAME_P95_MS = 40.0;
    private static final double RENDER_PHASE_MS = 14.0;

    public StutterCause analyze(PerfSnapshot frameSnapshot,
            PerfSnapshot tickSnapshot,
            GcMetricsSnapshot gcMetrics,
            FramePauseSnapshot pauses,
            RenderPipelineSnapshot renderSnapshot) {
        if (gcMetrics != null && gcMetrics.recentGcMs() >= GC_PAUSE_MS) {
            return new StutterCause(
                    "nozh.hud.stutter.gc",
                    String.format("GC %.0fms", gcMetrics.recentGcMs()),
                    confidence(gcMetrics.recentGcMs(), GC_PAUSE_MS, 120.0));
        }

        if (tickSnapshot != null && tickSnapshot.sufficientData()
                && tickSnapshot.p95FrametimeMs() >= TICK_P95_MS) {
            return new StutterCause(
                    "nozh.hud.stutter.tick",
                    String.format("Tick p95 %.1fms", tickSnapshot.p95FrametimeMs()),
                    confidence(tickSnapshot.p95FrametimeMs(), TICK_P95_MS, 150.0));
        }

        if (renderSnapshot != null && renderSnapshot.hottestPhase() != null
                && renderSnapshot.hottestPhase().maxMs() >= RENDER_PHASE_MS) {
            RenderPhaseMetrics hottest = renderSnapshot.hottestPhase();
            return new StutterCause(
                    "nozh.hud.stutter.render",
                    String.format("%s %.1fms", hottest.phase().name(), hottest.maxMs()),
                    confidence(hottest.maxMs(), RENDER_PHASE_MS, 60.0));
        }

        if (pauses != null && pauses.pauseCount() > 0) {
            return new StutterCause(
                    "nozh.hud.stutter.pause",
                    String.format("%d pauses", pauses.pauseCount()),
                    confidence(pauses.maxPauseMs(), 100.0, 500.0));
        }

        if (frameSnapshot != null && frameSnapshot.sufficientData()
                && frameSnapshot.p95FrametimeMs() >= FRAME_P95_MS) {
            return new StutterCause(
                    "nozh.hud.stutter.frame",
                    String.format("Frame p95 %.1fms", frameSnapshot.p95FrametimeMs()),
                    confidence(frameSnapshot.p95FrametimeMs(), FRAME_P95_MS, 120.0));
        }

        return StutterCause.unknown();
    }

    private double confidence(double value, double min, double max) {
        if (value <= min) {
            return 0.25;
        }
        double clamped = Math.max(min, Math.min(value, max));
        return Math.min(0.95, 0.25 + 0.7 * ((clamped - min) / (max - min)));
    }
}
