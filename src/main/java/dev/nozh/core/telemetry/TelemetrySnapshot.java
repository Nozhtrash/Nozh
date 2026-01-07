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
        double frametimeStddevMs,
        double avgConfidenceIntervalMs,
        double p95ConfidenceIntervalMs,
        int spikeCount,
        int sampleCount,
        int droppedSamples,
        boolean sufficientData) {
    /**
     * Empty snapshot for when no data is available.
     */
    public static TelemetrySnapshot EMPTY = new TelemetrySnapshot(
            0, 0, 0, 0, 0, 0, 0, 0, false);

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
            double frametimeStddevMs,
            int spikeCount,
            int sampleCount,
            int droppedSamples) {
        boolean sufficient = sampleCount >= MIN_SAMPLES;
        double avgCi = confidenceIntervalMs(frametimeStddevMs, sampleCount);
        double p95Ci = p95ConfidenceIntervalMs(frametimeStddevMs, sampleCount);
        return new TelemetrySnapshot(
                avgFrametimeMs,
                p95FrametimeMs,
                frametimeStddevMs,
                avgCi,
                p95Ci,
                spikeCount,
                sampleCount,
                droppedSamples,
                sufficient);
    }

    private static double confidenceIntervalMs(double stddevMs, int sampleCount) {
        if (sampleCount <= 1 || stddevMs <= 0) {
            return 0.0;
        }
        double standardError = stddevMs / Math.sqrt(sampleCount);
        return 1.96 * standardError;
    }

    private static double p95ConfidenceIntervalMs(double stddevMs, int sampleCount) {
        if (sampleCount <= 1 || stddevMs <= 0) {
            return 0.0;
        }
        double standardError = stddevMs / Math.sqrt(sampleCount);
        return 2.58 * standardError;
    }
}
