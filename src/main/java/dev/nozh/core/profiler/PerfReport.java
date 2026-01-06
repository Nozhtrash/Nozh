package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;

public record PerfReport(
        PerfSnapshot frameSnapshot,
        PerfSnapshot tickSnapshot,
        long[] frameSamplesNanos,
        FramePauseSnapshot pauses,
        GcMetricsSnapshot gcMetrics,
        RenderPipelineSnapshot renderPipeline,
        StutterCause stutterCause) {
}
