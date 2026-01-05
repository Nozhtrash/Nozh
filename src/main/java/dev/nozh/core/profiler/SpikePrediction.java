package dev.nozh.core.profiler;

/**
 * Prediction output for spike risk.
 */
public record SpikePrediction(
        boolean spikeLikely,
        double confidence,
        String reason) {
}
