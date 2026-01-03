package dev.nozh.fabric.capability;

import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.fabric.capability.providers.*;

/**
 * Provider bootstrap for production mod initialization.
 * 
 * Registers all capability providers in the registry.
 * 
 * Phase B: Provider Registration
 */
public final class ProviderBootstrap {

    private ProviderBootstrap() {
        // Static utility
    }

    /**
     * Register all providers in the registry.
     * 
     * @param registry Provider registry to populate
     * @param options  Minecraft options adapter
     */
    public static void registerAll(ProviderRegistry registry, MinecraftOptionsAdapter options) {
        // Register existing providers
        registry.register(new ParticlesProvider(options));
        registry.register(new CloudsProvider(options));
        registry.register(new EntityShadowsProvider(options));
        registry.register(new FpsCapProvider(options));

        // Register high-impact providers (v0.2-alpha)
        registry.register(new RenderDistanceProvider(options)); // +10-20 FPS
        registry.register(new SimulationDistanceProvider(options)); // +5-10 FPS
        registry.register(new BiomeBlendRadiusProvider(options)); // +2-4 FPS
        registry.register(new EntityDistanceProvider(options)); // +3-5 FPS
        registry.register(new MipmapLevelsProvider(options)); // +1-3 FPS

        registry.register(new VsyncProvider(options));
        registry.register(new GraphicsModeProvider(options));
        registry.register(new SmoothLightingProvider(options));
        registry.register(new FogProvider(options));
        registry.register(new DistortionEffectProvider(options));
        registry.register(new DynamicLightingProvider());

        // Enabled for GOD MODE
        registry.register(new ArmorStandProvider(options));
        registry.register(new ItemFrameProvider(options));
        registry.register(new BlockEntityProvider(options));
        registry.register(new AnimationProvider(options));
    }
}
