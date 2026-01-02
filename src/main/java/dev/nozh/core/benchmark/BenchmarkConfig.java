package dev.nozh.core.benchmark;

/**
 * Benchmark configuration (Contract 8 - Phase 3).
 */
public record BenchmarkConfig(
        int warmupSeconds,
        int measureSeconds,
        int cooldownSeconds) {
    /**
     * Default configuration.
     */
    public static BenchmarkConfig DEFAULT = new BenchmarkConfig(10, 30, 5);

    /**
     * Strict configuration (longer measure).
     */
    public static BenchmarkConfig STRICT = new BenchmarkConfig(15, 60, 10);
}
