package dev.nozh.core.bus;

/**
 * Command types: what action to perform.
 * 
 * Contract 2, Rule 2.3: CommandType ≠ CapabilityId
 * CommandType = "WHAT to do"
 * CapabilityId = "WHAT to touch"
 */
public enum CommandType {
    /**
     * Apply a capability value.
     */
    APPLY,

    /**
     * Reset a capability to default.
     */
    RESET,

    /**
     * Preview a capability change (no actual application).
     */
    PREVIEW,

    /**
     * Run benchmark.
     */
    BENCHMARK
}
