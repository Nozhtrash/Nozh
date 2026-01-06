package dev.nozh.core.profiler;

public record RenderPhaseMetrics(
        RenderPhase phase,
        int ticks,
        double totalMs,
        double maxMs,
        double avgMs) {

    public static RenderPhaseMetrics empty(RenderPhase phase) {
        return new RenderPhaseMetrics(phase, 0, 0.0, 0.0, 0.0);
    }
}
