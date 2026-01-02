package dev.nozh.api;

/**
 * Immutable performance snapshot.
 * 
 * Phase 3: Minimal definition (Measurement Only)
 * Phase 11 (ULTRA): Added spikeCount for outlier tracking
 * Contains strictly objective measurements, no classification.
 */
public record PerfSnapshot(
        double avgFrametimeMs,
        double p95FrametimeMs,
        int sampleCount,
        int spikeCount, // Number of samples >500ms (filtered from avg/p95)
        int windowSeconds,
        boolean sufficientData,
        long timestampMillis) {

    /**
     * Create an empty/invalid snapshot for initialization.
     */
    public static PerfSnapshot empty() {
        return new PerfSnapshot(Double.NaN, Double.NaN, 0, 0, 0, false, System.currentTimeMillis());
    }

    /**
     * Helper to estimate FPS from frametime if data is sufficient.
     * Returns 0 if insufficient data.
     */
    public double estimatedFps() {
        return (sufficientData && avgFrametimeMs > 0) ? 1000.0 / avgFrametimeMs : 0.0;
    }
}
