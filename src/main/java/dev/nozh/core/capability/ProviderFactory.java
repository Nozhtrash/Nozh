package dev.nozh.core.capability;

import dev.nozh.core.capability.providers.*;
import dev.nozh.NozhConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating capability providers.
 * 
 * ROADMAP: Phase 1, Sprint 1 - Provider Registry
 * 
 * Centralizes provider instantiation and registration.
 */
public class ProviderFactory {
    
    private static final Map<String, CapabilityProvider> PROVIDERS = new HashMap<>();
    
    static {
        registerDefaultProviders();
    }
    
    /**
     * Register all default providers.
     */
    private static void registerDefaultProviders() {
        try {
            register("render_distance", new RenderDistanceProvider());
            register("simulation_distance", new SimulationDistanceProvider());
            register("particles", new ParticlesProvider());
            register("entity_distance", new EntityDistanceProvider());
            register("graphics_mode", new GraphicsModeProvider());
            register("mipmap_levels", new MipmapLevelsProvider());
            register("smooth_lighting", new SmoothLightingProvider());
            register("clouds", new CloudsProvider());
            
            NozhConstants.LOGGER.info(
                "Registered {} capability providers", PROVIDERS.size());
        } catch (Exception e) {
            NozhConstants.LOGGER.error(
                "Failed to register providers", e);
        }
    }
    
    /**
     * Register a provider.
     */
    public static void register(String id, CapabilityProvider provider) {
        if (id == null || provider == null) {
            throw new IllegalArgumentException(
                "Provider ID and instance cannot be null");
        }
        
        PROVIDERS.put(id, provider);
        NozhConstants.LOGGER.debug("Registered provider: {}", id);
    }
    
    /**
     * Get provider by ID.
     */
    public static CapabilityProvider getProvider(String id) {
        return PROVIDERS.get(id);
    }
    
    /**
     * Check if provider exists.
     */
    public static boolean hasProvider(String id) {
        return PROVIDERS.containsKey(id);
    }
    
    /**
     * Get all registered provider IDs.
     */
    public static String[] getProviderIds() {
        return PROVIDERS.keySet().toArray(new String[0]);
    }
    
    /**
     * Get provider count.
     */
    public static int getProviderCount() {
        return PROVIDERS.size();
    }
}