package dev.nozh.core.testing;

/**
 * Chaos scenario enum (Phase 4 - Contract 11.1).
 * 
 * Defines stress test scenarios for system resilience.
 */
public enum ChaosScenario {
    /**
     * Stress provider init with intermittent failures.
     */
    PROVIDER_INIT_FAILURE,
    /**
     * Attempt repeated invariant violations to ensure rejection.
     */
    INVARIANT_VIOLATION_ATTEMPT,
    /**
     * Flood command queue to validate overflow handling.
     */
    QUEUE_OVERFLOW,
    /**
     * Starve telemetry buffer under massive sample load.
     */
    TELEMETRY_STARVATION,
    /**
     * Force governor state changes to test flapping control.
     */
    GOVERNOR_FLAPPING,
    /**
     * Violate preset bounds to confirm enforcement.
     */
    PRESET_VIOLATION,
    /**
     * Dispatch while safe mode active to validate rejection.
     */
    SAFEMODE_DISPATCH,
    /**
     * Corrupt HUD snapshot to ensure safe fallback.
     */
    HUD_SNAPSHOT_CORRUPTION,
    /**
     * Trigger crash loop recovery escalation.
     */
    CRASH_LOOP_RECOVERY,
    /**
     * Megabase entity swarm scenario (300+ entities).
     */
    ENTITY_SWARM,
    /**
     * Chunk loading spam under mining stress.
     */
    CHUNK_SPAM,
    /**
     * Shader load toggling under rain-like pressure.
     */
    SHADER_LOAD
}
