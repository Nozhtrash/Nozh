package dev.nozh.core.capability;

/**
 * Capability identifiers: what can be touched.
 * 
 * Contract 2, Rule 2.3: CapabilityId ≠ CommandType
 * CapabilityId = "WHAT to touch"
 * CommandType = "WHAT to do"
 */
public enum CapabilityId {
    // Tier 0/1: Runtime, Safe
    PARTICLES,
    CLOUDS,
    ENTITY_SHADOWS,
    BIOME_BLEND,
    FPS_CAP,
    VSYNC,

    // Tier 2: Requires reload
    GRAPHICS_MODE,
    SMOOTH_LIGHTING,
    MIPMAP_LEVEL,
    FOG,
    CHUNK_LOADING,

    // NOZH Specialties (Phase 2)impact
    RENDER_DISTANCE,
    SIMULATION_DISTANCE,
    ENTITY_DISTANCE,

    // Advanced (mod-dependent)
    RESOLUTION_SCALE,
    DISTORTION_EFFECT_SCALE,

    // Advanced Lighting
    DYNAMIC_LIGHTING,

    // God Mode: Precise Entity Control
    ARMOR_STANDS,
    ITEM_FRAMES,
    BLOCK_ENTITIES,
    ANIMATIONS
}
