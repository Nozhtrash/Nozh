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
 * Provider IDs come from registered OptimizationProviders (lowercased CapabilityId names).
 * <p>
 * MAPPING RULES:
 * - Provider IDs = CapabilityId.name().toLowerCase()
 * - Action IDs = descriptive names from learning engine
 * - One action can map to one provider
 */
public final class ActionProviderMapping {

    private static final Map<String, String> ACTION_TO_PROVIDER = new HashMap<>();

    static {
        // HIGH-IMPACT RENDERING (Tier 1)
        ACTION_TO_PROVIDER.put("reduce_render_distance", "render_distance");
        ACTION_TO_PROVIDER.put("reduce_simulation_distance", "simulation_distance");
        ACTION_TO_PROVIDER.put("reduce_entity_distance", "entity_distance");
        
        // PARTICLES AND VISUAL EFFECTS (Tier 0 - Safe, no reload)
        ACTION_TO_PROVIDER.put("lower_particles", "particles");
        ACTION_TO_PROVIDER.put("disable_clouds", "clouds");
        ACTION_TO_PROVIDER.put("reduce_shadows", "entity_shadows");
        ACTION_TO_PROVIDER.put("lower_entity_distance", "entity_distance");
        
        // GRAPHICS QUALITY (Tier 2 - May require reload)
        ACTION_TO_PROVIDER.put("lower_graphics_quality", "graphics_mode");
        ACTION_TO_PROVIDER.put("disable_smooth_lighting", "smooth_lighting");
        ACTION_TO_PROVIDER.put("lower_mipmap_levels", "mipmap_level");
        ACTION_TO_PROVIDER.put("reduce_biome_blend", "biome_blend");
        ACTION_TO_PROVIDER.put("disable_fog", "fog");
        
        // PERFORMANCE TUNING
        ACTION_TO_PROVIDER.put("enable_vsync", "vsync");
        ACTION_TO_PROVIDER.put("increase_fps_cap", "fps_cap");
        
        // ADVANCED EFFECTS (Mod-dependent)
        ACTION_TO_PROVIDER.put("disable_distortion", "distortion_effect_scale");
        ACTION_TO_PROVIDER.put("disable_dynamic_lighting", "dynamic_lighting");
        
        // GOD MODE (Phase 2 - Precise entity control)
        ACTION_TO_PROVIDER.put("disable_armor_stands", "armor_stands");
        ACTION_TO_PROVIDER.put("disable_item_frames", "item_frames");
        ACTION_TO_PROVIDER.put("disable_block_entities", "block_entities");
        ACTION_TO_PROVIDER.put("disable_animations", "animations");
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
