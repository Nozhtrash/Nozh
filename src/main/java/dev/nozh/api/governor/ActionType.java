package dev.nozh.api.governor;

/**
 * Types of actions the Governor can propose.
 * In Phase 5, these are strictly proposals and will NOT be executed.
 */
public enum ActionType {
    NONE,

    // Phase 6+ (Proposed)
    DECREASE_PARTICLES,
    DISABLE_CLOUDS,
    DISABLE_ENTITY_SHADOWS,
    DECREASE_BIOME_BLEND
}
