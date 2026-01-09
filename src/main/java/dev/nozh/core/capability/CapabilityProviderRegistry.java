package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.CloudRenderMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all capability providers.
 * 
 * Providers are the actual implementations that modify game settings.
 * This registry manages their lifecycle and provides safe execution.
 * 
 * @since 1.0.0
 */
public class CapabilityProviderRegistry {
    
    private static final Map<String, CapabilityProvider> providers = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    
    /**
     * Initialize all built-in providers.
     * Should be called during mod initialization.
     */
    public static synchronized void initialize() {
        if (initialized) {
            NozhConstants.LOGGER.warn("CapabilityProviderRegistry already initialized");
            return;
        }
        
        try {
            // Register all built-in providers
            register("render_distance", new RenderDistanceProvider());
            register("simulation_distance", new SimulationDistanceProvider());
            register("particles", new ParticlesProvider());
            register("entity_distance", new EntityDistanceProvider());
            register("graphics_mode", new GraphicsModeProvider());
            register("mipmap_levels", new MipmapLevelsProvider());
            register("smooth_lighting", new SmoothLightingProvider());
            register("clouds", new CloudsProvider());
            register("vsync", new VsyncProvider());
            register("max_fps", new MaxFpsProvider());
            
            initialized = true;
            NozhConstants.LOGGER.info("Registered {} capability providers", providers.size());
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to initialize CapabilityProviderRegistry", e);
            throw new RuntimeException("Critical: Provider registry initialization failed", e);
        }
    }
    
    /**
     * Register a custom capability provider.
     * 
     * @param actionId unique identifier for the action
     * @param provider the provider implementation
     * @throws IllegalArgumentException if actionId is null, empty, or already registered
     * @throws NullPointerException if provider is null
     */
    public static void register(String actionId, CapabilityProvider provider) {
        if (actionId == null || actionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Action ID cannot be null or empty");
        }
        if (provider == null) {
            throw new NullPointerException("Provider cannot be null");
        }
        
        if (providers.containsKey(actionId)) {
            NozhConstants.LOGGER.warn("Overwriting existing provider: {}", actionId);
        }
        
        providers.put(actionId, provider);
        NozhConstants.LOGGER.debug("Registered provider: {}", actionId);
    }
    
    /**
     * Execute an action with the registered provider.
     * 
     * @param actionId the action to execute
     * @param client Minecraft client instance
     * @param params additional parameters for the action
     * @return ActionResult indicating success or failure
     */
    public static ActionResult execute(String actionId, MinecraftClient client, Object... params) {
        // Validation
        if (actionId == null) {
            NozhConstants.LOGGER.error("Cannot execute action with null actionId");
            return ActionResult.error("Action ID is null");
        }
        
        if (client == null) {
            NozhConstants.LOGGER.error("Cannot execute action with null client");
            return ActionResult.error("MinecraftClient is null");
        }
        
        // Check initialization
        if (!initialized) {
            NozhConstants.LOGGER.error("CapabilityProviderRegistry not initialized");
            return ActionResult.error("Provider registry not initialized");
        }
        
        // Get provider
        CapabilityProvider provider = providers.get(actionId);
        if (provider == null) {
            NozhConstants.LOGGER.warn("No provider found for action: {}", actionId);
            return ActionResult.error("Provider not found: " + actionId);
        }
        
        // Execute with safety
        try {
            ActionResult result = provider.execute(client, params);
            
            if (result.isSuccess()) {
                NozhConstants.LOGGER.info("Successfully executed action: {}", actionId);
            } else {
                NozhConstants.LOGGER.warn("Action failed: {} - {}", actionId, result.getError());
            }
            
            return result;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Exception during action execution: {}", actionId, e);
            return ActionResult.error("Execution exception: " + e.getMessage());
        }
    }
    
    /**
     * Restore a setting to a previous value.
     * Used by rollback system.
     * 
     * @param actionId the action that was executed
     * @param snapshot the snapshot containing the old value
     * @return true if restored successfully
     */
    public static boolean restore(String actionId, StateSnapshot snapshot) {
        if (actionId == null || snapshot == null) {
            NozhConstants.LOGGER.error("Cannot restore with null parameters");
            return false;
        }
        
        CapabilityProvider provider = providers.get(actionId);
        if (provider == null) {
            NozhConstants.LOGGER.warn("Cannot restore unknown action: {}", actionId);
            return false;
        }
        
        if (!provider.canRollback()) {
            NozhConstants.LOGGER.warn("Provider {} does not support rollback", actionId);
            return false;
        }
        
        try {
            provider.rollback(snapshot);
            NozhConstants.LOGGER.info("Rolled back action: {}", actionId);
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to rollback action: {}", actionId, e);
            return false;
        }
    }
    
    /**
     * Check if a provider exists for the given action.
     * 
     * @param actionId the action to check
     * @return true if provider exists
     */
    public static boolean hasProvider(String actionId) {
        return providers.containsKey(actionId);
    }
    
    /**
     * Get all registered action IDs.
     * 
     * @return array of action IDs
     */
    public static String[] getRegisteredActions() {
        return providers.keySet().toArray(new String[0]);
    }
    
    /**
     * Check if registry is initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Clear all providers (for testing).
     */
    static void clearForTesting() {
        providers.clear();
        initialized = false;
    }
}
