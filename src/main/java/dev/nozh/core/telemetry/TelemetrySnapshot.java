package dev.nozh.core.telemetry;

/**
 * Telemetry snapshot (Contract 4).
 * 
 * Cheap copy of aggregated telemetry data for HUD/Governor consumption.
 * PURE - no mutable state, no nested objects.
 */
public record TelemetrySnapshot(
        double avgFrametimeMs,
        double p95FrametimeMs,
        int spikeCount,
        int sampleCount,
        int droppedSamples,
        boolean sufficientData) {
    /**
     * Empty snapshot for when no data is available.
     */
    public static TelemetrySnapshot EMPTY = new TelemetrySnapshot(
            0, 0, 0, 0, 0, false);

    /**
     * Minimum samples required for sufficient data.
     */
    private static final int MIN_SAMPLES = 30;

    /**
     * Create snapshot with automatic sufficiency check.
     */
    public static TelemetrySnapshot of(
            double avgFrametimeMs,
            double p95FrametimeMs,
            int spikeCount,
            int sampleCount,
            int droppedSamples) {
        boolean sufficient = sampleCount >= MIN_SAMPLES;
        return new TelemetrySnapshot(
                avgFrametimeMs,
                p95FrametimeMs,
                spikeCount,
                sampleCount,
                droppedSamples,
                sufficient);
    }
}
