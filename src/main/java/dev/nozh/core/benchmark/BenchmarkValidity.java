package dev.nozh.core.benchmark;

/**
 * Benchmark validity classification (Contract 8 - Phase 3).
 */
public enum BenchmarkValidity {
    /**
     * Results are stable and reliable.
     */
    VALID,

    /**
     * High variance detected - results unreliable.
     */
    NOISY,

    /**
     * Insufficient data or other issues.
     */
    INCONCLUSIVE
}
