package dev.nozh.core.profiler;

public record GcMetricsSnapshot(
        double recentGcMs,
        double pressureScore,
        boolean gcPauses) {

    public static GcMetricsSnapshot empty() {
        return new GcMetricsSnapshot(0.0, 0.0, false);
    }
}
