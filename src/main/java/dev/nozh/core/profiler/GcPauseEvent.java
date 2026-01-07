package dev.nozh.core.profiler;

public record GcPauseEvent(
        long timestampMillis,
        long elapsedMillis,
        double pauseMs) {
}
