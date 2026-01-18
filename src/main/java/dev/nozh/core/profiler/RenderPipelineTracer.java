package dev.nozh.core.profiler;

import java.util.ArrayList;

import java.util.List;

public class RenderPipelineTracer {

    private static final long WINDOW_MS = 1000L;

    // OPTIMIZATION: Use arrays instead of EnumMap to avoid caching/lookup overhead
    // and boxing
    private final PhaseAccumulator[] accumulators; // Indexed by phase.ordinal()
    private final long[] phaseStartNanos; // Indexed by phase.ordinal()

    private long windowStartMillis = System.currentTimeMillis();
    private RenderPipelineSnapshot lastSnapshot = RenderPipelineSnapshot.empty();

    private long frameStartNanos = 0L;

    public RenderPipelineTracer() {
        int phaseCount = RenderPhase.values().length;
        this.accumulators = new PhaseAccumulator[phaseCount];
        this.phaseStartNanos = new long[phaseCount];

        for (RenderPhase phase : RenderPhase.values()) {
            this.accumulators[phase.ordinal()] = new PhaseAccumulator();
        }
    }

    public synchronized void beginPhase(RenderPhase phase) {
        if (phase == null) {
            return;
        }
        // Zero-allocation primitive array store
        phaseStartNanos[phase.ordinal()] = System.nanoTime();
    }

    public synchronized long endPhase(RenderPhase phase) {
        if (phase == null) {
            return 0L;
        }
        int idx = phase.ordinal();
        long start = phaseStartNanos[idx];

        if (start == 0L) {
            return 0L;
        }

        long duration = System.nanoTime() - start;
        phaseStartNanos[idx] = 0L; // Reset

        accumulators[idx].addSample(duration);
        rollWindowIfNeeded(System.currentTimeMillis());
        return duration;
    }

    public synchronized void onFrameStart() {
        frameStartNanos = System.nanoTime();
        beginPhase(RenderPhase.FRAME);
    }

    public synchronized long onFrameEnd() {
        endPhase(RenderPhase.FRAME);
        if (frameStartNanos == 0L) {
            return 0L;
        }
        long duration = System.nanoTime() - frameStartNanos;
        frameStartNanos = 0L;
        return duration;
    }

    public synchronized RenderPipelineSnapshot snapshot() {
        rollWindowIfNeeded(System.currentTimeMillis());
        if (lastSnapshot.phases().isEmpty()) {
            return buildSnapshot(windowStartMillis, System.currentTimeMillis());
        }
        return lastSnapshot;
    }

    private void rollWindowIfNeeded(long nowMillis) {
        if (nowMillis - windowStartMillis < WINDOW_MS) {
            return;
        }
        lastSnapshot = buildSnapshot(windowStartMillis, nowMillis);
        windowStartMillis = nowMillis;
        for (PhaseAccumulator accumulator : accumulators) {
            accumulator.reset();
        }
    }

    private RenderPipelineSnapshot buildSnapshot(long startMillis, long endMillis) {
        List<RenderPhaseMetrics> metrics = new ArrayList<>();
        RenderPhaseMetrics hottest = RenderPhaseMetrics.empty(RenderPhase.UNKNOWN);

        RenderPhase[] phases = RenderPhase.values();
        for (int i = 0; i < phases.length; i++) {
            RenderPhase phase = phases[i];
            PhaseAccumulator accumulator = accumulators[i];
            RenderPhaseMetrics phaseMetrics = accumulator.toMetrics(phase);
            metrics.add(phaseMetrics);
            if (phaseMetrics.maxMs() > hottest.maxMs()) {
                hottest = phaseMetrics;
            }
        }
        return new RenderPipelineSnapshot(startMillis, endMillis, metrics, hottest);
    }

    private static final class PhaseAccumulator {
        private int count = 0;
        private long totalNanos = 0L;
        private long maxNanos = 0L;

        void addSample(long nanos) {
            count++;
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
        }

        RenderPhaseMetrics toMetrics(RenderPhase phase) {
            double totalMs = totalNanos / 1_000_000.0;
            double maxMs = maxNanos / 1_000_000.0;
            double avgMs = count > 0 ? totalMs / count : 0.0;
            return new RenderPhaseMetrics(phase, count, totalMs, maxMs, avgMs);
        }

        void reset() {
            count = 0;
            totalNanos = 0L;
            maxNanos = 0L;
        }
    }
}
