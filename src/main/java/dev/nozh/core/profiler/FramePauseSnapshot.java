package dev.nozh.core.profiler;

public record FramePauseSnapshot(
        int pauseCount,
        double totalPauseMs,
        double maxPauseMs,
        double lastPauseMs) {

    public static FramePauseSnapshot empty() {
        return new FramePauseSnapshot(0, 0.0, 0.0, 0.0);
    }
}
