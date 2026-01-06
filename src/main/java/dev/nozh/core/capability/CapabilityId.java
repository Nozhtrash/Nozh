package dev.nozh.core.capability;

public enum CapabilityId {
    RENDER_DISTANCE,
    ENTITY_DISTANCE,
    SHADOW_DISTANCE,
    CHUNK_UPDATE_THREADS,
    PARTICLES,
    VSYNC,
    MAX_FPS,
    ENTITY_CULLING,
    ARMOR_STAND_CULLING,
    ITEM_FRAME_CULLING,
    BLOCK_ENTITY_CULLING,
    CLOUDS,
    FOG,
    WEATHER,
    BIOME_BLEND,
    SMOOTH_LIGHTING,
    MIPMAP_LEVELS,
    ANISOTROPIC_FILTERING,
    GRASS_DETAIL,
    LEAF_QUALITY,
    WATER_QUALITY,
    SHADER_QUALITY,
    DYNAMIC_LIGHTING;
    
    public String getDisplayName() {
        return name().replace('_', ' ');
    }
}
