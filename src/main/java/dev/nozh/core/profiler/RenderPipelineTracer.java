package dev.nozh.core.profiler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class RenderPipelineTracer {

    private static final long WINDOW_MS = 1000L;

    private final EnumMap<RenderPhase, PhaseAccumulator> accumulators = new EnumMap<>(RenderPhase.class);
    private final EnumMap<RenderPhase, Long> phaseStartNanos = new EnumMap<>(RenderPhase.class);

    private long windowStartMillis = System.currentTimeMillis();
    private RenderPipelineSnapshot lastSnapshot = RenderPipelineSnapshot.empty();

    private long frameStartNanos = 0L;

    public RenderPipelineTracer() {
        for (RenderPhase phase : RenderPhase.values()) {
            accumulators.put(phase, new PhaseAccumulator());
        }
    }

    public synchronized void beginPhase(RenderPhase phase) {
        if (phase == null) {
            return;
        }
        phaseStartNanos.put(phase, System.nanoTime());
    }

    public synchronized long endPhase(RenderPhase phase) {
        if (phase == null) {
            return 0L;
        }
        Long start = phaseStartNanos.remove(phase);
        if (start == null) {
            return 0L;
        }
        long duration = System.nanoTime() - start;
        PhaseAccumulator accumulator = accumulators.get(phase);
        if (accumulator != null) {
            accumulator.addSample(duration);
        }
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
            return buildSnapshot(windowStartMillis, System.currentTimeMillis(), accumulators);
        }
        return lastSnapshot;
    }

    private void rollWindowIfNeeded(long nowMillis) {
        if (nowMillis - windowStartMillis < WINDOW_MS) {
            return;
        }
        lastSnapshot = buildSnapshot(windowStartMillis, nowMillis, accumulators);
        windowStartMillis = nowMillis;
        for (PhaseAccumulator accumulator : accumulators.values()) {
            accumulator.reset();
        }
    }

    private RenderPipelineSnapshot buildSnapshot(long startMillis, long endMillis,
            EnumMap<RenderPhase, PhaseAccumulator> data) {
        List<RenderPhaseMetrics> metrics = new ArrayList<>();
        RenderPhaseMetrics hottest = RenderPhaseMetrics.empty(RenderPhase.UNKNOWN);
        for (var entry : data.entrySet()) {
            RenderPhase phase = entry.getKey();
            PhaseAccumulator accumulator = entry.getValue();
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
