package dev.nozh.core.profiler;

public record PerfTraceEvent(
        long timestampMillis,
        PerfTraceType type,
        double durationMs,
        String detail,
        String category,
        String severity) {

    public static PerfTraceEvent render(long timestampMillis, double durationMs, String detail) {
        return new PerfTraceEvent(timestampMillis, PerfTraceType.RENDER, durationMs, detail, null, null);
    }

    public static PerfTraceEvent tick(long timestampMillis, double durationMs, String detail) {
        return new PerfTraceEvent(timestampMillis, PerfTraceType.TICK, durationMs, detail, null, null);
    }

    public static PerfTraceEvent gc(long timestampMillis, double durationMs, String detail) {
        return new PerfTraceEvent(timestampMillis, PerfTraceType.GC, durationMs, detail, null, null);
    }

    public static PerfTraceEvent critical(long timestampMillis, String detail, String category, String severity) {
        return new PerfTraceEvent(timestampMillis, PerfTraceType.CRITICAL, 0.0, detail, category, severity);
    }
}
