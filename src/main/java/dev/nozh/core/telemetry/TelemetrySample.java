package dev.nozh.core.telemetry;

/**
 * Telemetry sample (Contract 4).
 * 
 * PURE record - primitives only, no Optional, no nested objects.
 * Sentinel values: -1 for unavailable/unknown metrics, NaN for invalid
 * calculations.
 * 
 * Rule 4.6: Absolute simplicity for zero-allocation sampling.
 */
public record TelemetrySample(
        long timestampMillis,
        double frametimeMs, // -1 if unavailable
        double tickMs, // -1 if unavailable
        int fps, // -1 if unavailable
        int entities, // -1 if unavailable
        int chunks, // -1 if unavailable
        int drawCalls, // -1 if unavailable
        int droppedSamples // Cumulative count of dropped samples
) {
    /**
     * Sentinel sample for unavailable data.
     */
    public static TelemetrySample UNAVAILABLE = new TelemetrySample(
            System.currentTimeMillis(),
            -1, -1, -1, -1, -1, -1, 0);

    /**
     * Check if this sample has valid frametime data.
     */
    public boolean hasFrametimeData() {
        return frametimeMs >= 0;
    }

    /**
     * Check if this sample has valid tick data.
     */
    public boolean hasTickData() {
        return tickMs >= 0;
    }
}
