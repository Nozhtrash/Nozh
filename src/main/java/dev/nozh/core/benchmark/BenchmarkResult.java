package dev.nozh.core.benchmark;

/**
 * Benchmark result (Contract 8 - Phase 3).
 * 
 * Complete benchmark execution result with validity classification.
 */
public record BenchmarkResult(
        double avgFrametimeMs,
        double p95FrametimeMs,
        double p99FrametimeMs,
        int spikeCount,
        int sampleCount,
        BenchmarkValidity validity,
        double noiseMetric, // Coefficient of variation
        String notes // Human-readable notes (empty string if none)
) {
    /**
     * Empty result (benchmark not run).
     */
    public static BenchmarkResult EMPTY = new BenchmarkResult(
            0, 0, 0, 0, 0,
            BenchmarkValidity.INCONCLUSIVE,
            0,
            "Not run");
}
