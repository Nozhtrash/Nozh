package dev.nozh.core.testing;

/**
 * Chaos scenario enum (Phase 4 - Contract 11.1).
 * 
 * Defines stress test scenarios for system resilience.
 */
public enum ChaosScenario {
    PROVIDER_INIT_FAILURE,
    INVARIANT_VIOLATION_ATTEMPT,
    QUEUE_OVERFLOW,
    TELEMETRY_STARVATION,
    GOVERNOR_FLAPPING,
    PRESET_VIOLATION,
    SAFEMODE_DISPATCH,
    HUD_SNAPSHOT_CORRUPTION,
    CRASH_LOOP_RECOVERY
}
