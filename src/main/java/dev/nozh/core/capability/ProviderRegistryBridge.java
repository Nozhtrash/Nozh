package dev.nozh.core.capability;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.NozhConstants;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridge that automatically registers CapabilityProviders as OptimizationProviders.
 * <p>
 * This allows the new optimization system to work with existing CapabilityProvider
 * implementations without modification.
 * <p>
 * The bridge:
 * - Wraps each CapabilityProvider in a CapabilityProviderAdapter
 * - Assigns appropriate target values for optimization
 * - Registers adapted providers in ProviderRegistry
 */
public final class ProviderRegistryBridge {

    private static final Logger LOGGER = NozhConstants.LOGGER;

    /**
     * Default target values for each capability when optimizing for FPS.
     * These are conservative values that improve performance without drastic quality loss.
     */
    private static final Map<CapabilityId, CapabilityValue> DEFAULT_OPTIMIZATION_VALUES = new HashMap<>();

    static {
        // Rendering optimizations
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.RENDER_DISTANCE, new CapabilityValue.IntValue(8));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.SIMULATION_DISTANCE, new CapabilityValue.IntValue(4));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.ENTITY_DISTANCE, new CapabilityValue.IntValue(50));
        
        // Particle and visual effects (reduced/off)
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.PARTICLES, new CapabilityValue.StringValue("minimal"));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.CLOUDS, new CapabilityValue.BoolValue(false));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.ENTITY_SHADOWS, new CapabilityValue.BoolValue(false));
        
        // Graphics quality
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.GRAPHICS_MODE, new CapabilityValue.StringValue("fast"));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.SMOOTH_LIGHTING, new CapabilityValue.BoolValue(false));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.MIPMAP_LEVEL, new CapabilityValue.IntValue(0));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.BIOME_BLEND, new CapabilityValue.IntValue(1));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.FOG, new CapabilityValue.BoolValue(false));
        
        // Performance tuning
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.VSYNC, new CapabilityValue.BoolValue(false));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.FPS_CAP, new CapabilityValue.IntValue(120));
        
        // Advanced
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.DISTORTION_EFFECT_SCALE, new CapabilityValue.IntValue(0));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.DYNAMIC_LIGHTING, new CapabilityValue.BoolValue(false));
        
        // God mode
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.ARMOR_STANDS, new CapabilityValue.BoolValue(false));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.ITEM_FRAMES, new CapabilityValue.BoolValue(false));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.BLOCK_ENTITIES, new CapabilityValue.BoolValue(false));
        DEFAULT_OPTIMIZATION_VALUES.put(CapabilityId.ANIMATIONS, new CapabilityValue.BoolValue(false));
    }

    /**
     * Register a CapabilityProvider as an OptimizationProvider.
     * <p>
     * This wraps the CapabilityProvider in an adapter and registers it
     * with the default optimization value for its capability.
     *
     * @param registry Registry to register in
     * @param provider CapabilityProvider to register
     */
    public static void registerCapabilityProvider(
            ProviderRegistry registry,
            CapabilityProvider provider
    ) {
        if (provider == null) {
            LOGGER.warn("Attempted to register null CapabilityProvider");
            return;
        }

        CapabilityId capId = provider.id();
        if (capId == null) {
            LOGGER.warn("CapabilityProvider has null id: {}", provider.getClass().getSimpleName());
            return;
        }

        // Get default optimization value for this capability
        CapabilityValue targetValue = DEFAULT_OPTIMIZATION_VALUES.get(capId);
        if (targetValue == null) {
            LOGGER.warn("No default optimization value for CapabilityId: {}", capId);
            // Use a dummy value (won't be used if not in ActionProviderMapping)
            targetValue = new CapabilityValue.BoolValue(false);
        }

        // Create adapter
        String providerId = capId.name().toLowerCase();
        CapabilityProviderAdapter adapter = new CapabilityProviderAdapter(
                provider,
                providerId,
                targetValue
        );

        // Register adapted provider
        registry.register(adapter);
        
        LOGGER.debug("Registered CapabilityProvider: {} as OptimizationProvider: {}",
                capId.name(), providerId);
    }

    /**
     * Register multiple CapabilityProviders at once.
     *
     * @param registry  Registry to register in
     * @param providers CapabilityProviders to register
     */
    public static void registerAll(
            ProviderRegistry registry,
            CapabilityProvider... providers
    ) {
        for (CapabilityProvider provider : providers) {
            registerCapabilityProvider(registry, provider);
        }
    }

    /**
     * Get the default optimization value for a capability.
     *
     * @param capabilityId Capability ID
     * @return Default optimization value, or null if not defined
     */
    public static CapabilityValue getDefaultOptimizationValue(CapabilityId capabilityId) {
        return DEFAULT_OPTIMIZATION_VALUES.get(capabilityId);
    }

    private ProviderRegistryBridge() {
        // Utility class
    }
}