package dev.nozh.core.capability;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps action IDs to their corresponding provider IDs.
 * <p>
 * This is the bridge between the learning engine's action space
 * and the capability providers that execute those actions.
 * <p>
 * Action IDs come from QTable and DecisionMaking.
 * Provider IDs come from registered OptimizationProviders.
 */
public final class ActionProviderMapping {

    private static final Map<String, String> ACTION_TO_PROVIDER = new HashMap<>();

    static {
        // High-impact rendering optimizations
        ACTION_TO_PROVIDER.put("reduce_render_distance", "render_distance");
        ACTION_TO_PROVIDER.put("reduce_simulation_distance", "simulation_distance");
        ACTION_TO_PROVIDER.put("reduce_entity_distance", "entity_distance_scaling");
        ACTION_TO_PROVIDER.put("lower_graphics_quality", "graphics_mode");
        ACTION_TO_PROVIDER.put("reduce_shadows", "entity_shadows");
        ACTION_TO_PROVIDER.put("disable_clouds", "clouds");
        
        // Particle and visual effects
        ACTION_TO_PROVIDER.put("lower_particles", "particles");
        ACTION_TO_PROVIDER.put("disable_fog", "fog");
        ACTION_TO_PROVIDER.put("disable_distortion", "distortion_effects");
        ACTION_TO_PROVIDER.put("disable_animations", "animation");
        ACTION_TO_PROVIDER.put("lower_mipmap_levels", "mipmap_levels");
        ACTION_TO_PROVIDER.put("reduce_biome_blend", "biome_blend_radius");
        
        // Advanced optimizations (GOD MODE)
        ACTION_TO_PROVIDER.put("disable_armor_stands", "armor_stand_renderer");
        ACTION_TO_PROVIDER.put("disable_item_frames", "item_frame_renderer");
        ACTION_TO_PROVIDER.put("disable_block_entities", "block_entity_renderer");
        
        // Performance tuning
        ACTION_TO_PROVIDER.put("enable_vsync", "vsync");
        ACTION_TO_PROVIDER.put("disable_smooth_lighting", "smooth_lighting");
        ACTION_TO_PROVIDER.put("increase_fps_cap", "fps_cap");
    }

    /**
     * Get the provider ID for a given action ID.
     *
     * @param actionId Action ID from learning engine
     * @return Provider ID, or empty if no mapping exists
     */
    public static Optional<String> getProviderIdForAction(String actionId) {
        return Optional.ofNullable(ACTION_TO_PROVIDER.get(actionId));
    }

    /**
     * Check if an action has a registered provider.
     *
     * @param actionId Action ID to check
     * @return true if provider mapping exists
     */
    public static boolean hasProviderForAction(String actionId) {
        return ACTION_TO_PROVIDER.containsKey(actionId);
    }

    /**
     * Get all registered action IDs.
     *
     * @return Set of action IDs with provider mappings
     */
    public static java.util.Set<String> getAllActionIds() {
        return ACTION_TO_PROVIDER.keySet();
    }

    private ActionProviderMapping() {
        // Utility class
    }
}