package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.providers.*;
import dev.nozh.core.state.StateSnapshot;
import net.minecraft.client.MinecraftClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all capability providers.
 * Manages provider lifecycle and execution.
 * 
 * <p>Thread-safe singleton registry with concurrent provider access.
 * 
 * <p>Providers are registered statically and can be executed by ID.
 * Each provider can modify game settings and return execution results
 * with rollback support.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 1)
 */
public final class CapabilityProviderRegistry {
    private static final Map<String, CapabilityProvider> providers = new ConcurrentHashMap<>();
    private static final Map<String, StateSnapshot> snapshots = new ConcurrentHashMap<>();
    
    static {
        // Register all essential providers (Phase 1 Sprint 1)
        register("render_distance", new RenderDistanceProvider());
        register("simulation_distance", new SimulationDistanceProvider());
        register("particles", new ParticlesProvider());
        register("entity_distance", new EntityDistanceProvider());
        register("graphics_mode", new GraphicsModeProvider());
        register("mipmap_levels", new MipmapLevelsProvider());
        register("smooth_lighting", new SmoothLightingProvider());
        register("clouds", new CloudsProvider());
        
        // Reduction actions (aliases for optimization)
        register("reduce_render_distance", new RenderDistanceProvider());
        register("lower_particles", new ParticlesProvider());
        register("disable_clouds", new CloudsProvider());
        register("lower_entity_distance", new EntityDistanceProvider());
        
        NozhConstants.LOGGER.info("CapabilityProviderRegistry initialized with {} providers", providers.size());
    }
    
    /**
     * Register a capability provider.
     * 
     * @param actionId unique identifier for the action
     * @param provider provider implementation
     */
    public static void register(String actionId, CapabilityProvider provider) {
        if (actionId == null || provider == null) {
            throw new IllegalArgumentException("actionId and provider must not be null");
        }
        providers.put(actionId, provider);
    }
    
    /**
     * Execute a capability provider action.
     * 
     * @param actionId action identifier
     * @param client Minecraft client instance
     * @param params optional parameters for the provider
     * @return action result with success status and snapshot
     */
    public static ActionResult execute(String actionId, MinecraftClient client, Object... params) {
        if (actionId == null) {
            return ActionResult.error("Action ID cannot be null");
        }
        
        if (client == null) {
            return ActionResult.error("MinecraftClient cannot be null");
        }
        
        CapabilityProvider provider = providers.get(actionId);
        if (provider == null) {
            return ActionResult.error("Provider not found: " + actionId);
        }
        
        try {
            ActionResult result = provider.execute(client, params);
            
            // Store snapshot for potential rollback
            if (result.isSuccess() && result.getSnapshot() != null) {
                snapshots.put(actionId, result.getSnapshot());
            }
            
            return result;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Provider execution failed for: {}", actionId, e);
            return ActionResult.error("Execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Restore a specific setting to its previous value.
     * 
     * @param key setting key
     * @param value value to restore
     */
    public static void restore(String key, Object value) {
        if (key == null || value == null) {
            NozhConstants.LOGGER.warn("Cannot restore null key or value");
            return;
        }
        
        // Delegate to appropriate provider
        CapabilityProvider provider = providers.get(key);
        if (provider != null && provider.canRollback()) {
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put(key, value);
            provider.rollback(snapshot);
        } else {
            NozhConstants.LOGGER.warn("No rollback support for key: {}", key);
        }
    }
    
    /**
     * Check if a provider exists.
     */
    public static boolean hasProvider(String actionId) {
        return providers.containsKey(actionId);
    }
    
    /**
     * Get all registered action IDs.
     */
    public static String[] getRegisteredActions() {
        return providers.keySet().toArray(new String[0]);
    }
    
    private CapabilityProviderRegistry() {
        // Private constructor to prevent instantiation
    }
}
