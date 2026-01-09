package dev.nozh.core.scenario;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * Analyzes dimensional and environmental context.
 * 
 * ROADMAP: Phase 2, Sprint 3 - Dimensional Context Analysis
 * 
 * Provides dimension-specific optimization recommendations.
 */
public class DimensionalContext {
    
    public enum Dimension {
        OVERWORLD,
        NETHER,
        END,
        CUSTOM
    }
    
    public enum OptimizationProfile {
        HEAVY_CULLING,      // Nether: lots of lava rendering
        AGGRESSIVE_DISTANCE, // End: void rendering
        ENTITY_FOCUSED,     // Ocean: many water entities
        BALANCED,           // Default overworld
        CONSERVATIVE        // Unknown dimensions
    }
    
    private final Dimension dimension;
    private final String biomeName;
    private final boolean isRaining;
    private final int skylightLevel;
    
    private DimensionalContext(Dimension dim, String biome, boolean rain, int light) {
        this.dimension = dim;
        this.biomeName = biome;
        this.isRaining = rain;
        this.skylightLevel = light;
    }
    
    /**
     * Analyze current dimensional context.
     */
    public static DimensionalContext analyze(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            return null;
        }
        
        RegistryKey<World> dimKey = client.world.getRegistryKey();
        Dimension dim = identifyDimension(dimKey);
        
        String biomeName = "unknown";
        try {
            Biome biome = client.world.getBiome(client.player.getBlockPos()).value();
            biomeName = biome.toString();
        } catch (Exception e) {
            // Biome detection failed, use default
        }
        
        boolean raining = client.world.isRaining();
        int skylight = client.world.getAmbientDarkness();
        
        return new DimensionalContext(dim, biomeName, raining, skylight);
    }
    
    /**
     * Identify dimension from registry key.
     */
    private static Dimension identifyDimension(RegistryKey<World> key) {
        if (key == World.OVERWORLD) {
            return Dimension.OVERWORLD;
        } else if (key == World.NETHER) {
            return Dimension.NETHER;
        } else if (key == World.END) {
            return Dimension.END;
        } else {
            return Dimension.CUSTOM;
        }
    }
    
    /**
     * Get recommended optimization profile for this dimension.
     */
    public OptimizationProfile getRecommendedProfile() {
        switch (dimension) {
            case NETHER:
                // Nether: Heavy lava rendering, particles
                return OptimizationProfile.HEAVY_CULLING;
                
            case END:
                // End: Void rendering, fewer chunks needed
                return OptimizationProfile.AGGRESSIVE_DISTANCE;
                
            case OVERWORLD:
                // Check biome
                if (biomeName.toLowerCase().contains("ocean")) {
                    return OptimizationProfile.ENTITY_FOCUSED;
                }
                return OptimizationProfile.BALANCED;
                
            default:
                return OptimizationProfile.CONSERVATIVE;
        }
    }
    
    public Dimension getDimension() { return dimension; }
    public String getBiomeName() { return biomeName; }
    public boolean isRaining() { return isRaining; }
    public int getSkylightLevel() { return skylightLevel; }
}