package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;

import java.util.Optional;

public class SpikeCausalityAnalyzer {

    private static final double GC_PAUSE_MS = 40.0;
    private static final double TICK_P95_MS = 50.0;
    private static final double FRAME_P95_MS = 40.0;
    private static final double RENDER_PHASE_MS = 14.0;

    public SpikeCausalityReport analyze(PerfSnapshot frameSnapshot,
            PerfSnapshot tickSnapshot,
            GcMetricsSnapshot gcMetrics,
            FramePauseSnapshot pauses,
            RenderPipelineSnapshot renderSnapshot,
            PerfTraceSnapshot traceSnapshot) {
        Optional<PerfTraceEvent> criticalEvent = traceSnapshot != null
                ? traceSnapshot.latestCriticalEvent()
                : Optional.empty();
        if (criticalEvent.isPresent()) {
            PerfTraceEvent event = criticalEvent.get();
            String detail = event.detail() != null ? event.detail() : "";
            return new SpikeCausalityReport(
                    SpikeCauseType.CRITICAL_EVENT,
                    0.9,
                    detail);
        }

        if (gcMetrics != null && gcMetrics.recentGcMs() >= GC_PAUSE_MS) {
            return new SpikeCausalityReport(
                    SpikeCauseType.GC,
                    confidence(gcMetrics.recentGcMs(), GC_PAUSE_MS, 120.0),
                    String.format("GC %.0fms", gcMetrics.recentGcMs()));
        }

        if (tickSnapshot != null && tickSnapshot.sufficientData()
                && tickSnapshot.p95FrametimeMs() >= TICK_P95_MS) {
            return new SpikeCausalityReport(
                    SpikeCauseType.TICK,
                    confidence(tickSnapshot.p95FrametimeMs(), TICK_P95_MS, 150.0),
                    String.format("Tick p95 %.1fms", tickSnapshot.p95FrametimeMs()));
        }

        if (renderSnapshot != null && renderSnapshot.hottestPhase() != null
                && renderSnapshot.hottestPhase().maxMs() >= RENDER_PHASE_MS) {
            RenderPhaseMetrics hottest = renderSnapshot.hottestPhase();
            return new SpikeCausalityReport(
                    SpikeCauseType.RENDER,
                    confidence(hottest.maxMs(), RENDER_PHASE_MS, 60.0),
                    String.format("%s %.1fms", hottest.phase().name(), hottest.maxMs()));
        }

        if (pauses != null && pauses.pauseCount() > 0) {
            return new SpikeCausalityReport(
                    SpikeCauseType.FRAME,
                    confidence(pauses.maxPauseMs(), 100.0, 500.0),
                    String.format("%d pauses", pauses.pauseCount()));
        }

        if (frameSnapshot != null && frameSnapshot.sufficientData()
                && frameSnapshot.p95FrametimeMs() >= FRAME_P95_MS) {
            return new SpikeCausalityReport(
                    SpikeCauseType.FRAME,
                    confidence(frameSnapshot.p95FrametimeMs(), FRAME_P95_MS, 120.0),
                    String.format("Frame p95 %.1fms", frameSnapshot.p95FrametimeMs()));
        }

        return SpikeCausalityReport.unknown();
    }

    private double confidence(double value, double min, double max) {
        if (value <= min) {
            return 0.25;
        }
        double clamped = Math.max(min, Math.min(value, max));
        return Math.min(0.95, 0.25 + 0.7 * ((clamped - min) / (max - min)));
    }
}
