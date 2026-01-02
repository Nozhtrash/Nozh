package dev.nozh.api;

/**
 * Performance bottleneck classification.
 * 
 * Phase 2: Enum definition
 * Phase 4: Actual classification logic
 */
public enum Bound {
    /**
     * Performance limited by GPU (rendering).
     * Typical indicators: high frametime, low tick time
     */
    GPU_BOUND,

    /**
     * Performance limited by CPU (game logic).
     * Typical indicators: low frametime, high tick time
     */
    CPU_BOUND,

    /**
     * Both GPU and CPU are bottlenecks.
     * Typical indicators: both frametime and tick time high
     */
    MIXED,

    /**
     * Insufficient data to determine bound.
     * Used when profiler hasn't collected enough samples.
     */
    UNKNOWN
}
