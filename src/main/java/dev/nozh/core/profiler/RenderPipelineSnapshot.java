package dev.nozh.core.profiler;

import java.util.List;

public record RenderPipelineSnapshot(
        long windowStartMillis,
        long windowEndMillis,
        List<RenderPhaseMetrics> phases,
        RenderPhaseMetrics hottestPhase) {

    public static RenderPipelineSnapshot empty() {
        return new RenderPipelineSnapshot(0L, 0L, List.of(), RenderPhaseMetrics.empty(RenderPhase.UNKNOWN));
    }
}
