package dev.nozh.core.profiler;

public record PerfDiagnosticsSnapshot(
        double recentGcMs,
        double gcPressureScore,
        int pauseCount,
        double pauseMaxMs,
        double tickMs,
        double renderMs,
        String stutterCauseKey,
        String stutterDetail,
        double stutterConfidence,
        String hottestRenderPhaseKey,
        double hottestRenderPhaseMs,
        int hottestRenderPhaseTicks) {

    public static PerfDiagnosticsSnapshot empty() {
        return new PerfDiagnosticsSnapshot(
                0.0,
                0.0,
                0,
                0.0,
                -1.0,
                -1.0,
                "nozh.hud.stutter.unknown",
                "",
                0.0,
                "nozh.hud.render_phase.unknown",
                0.0,
                0);
    }
}
