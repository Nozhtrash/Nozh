package dev.nozh.core.context;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import java.util.Objects;

/**
 * Context information about current dimension and biome.
 * Provides dimension-specific optimization recommendations.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 2 Sprint 3)
 */
public final class DimensionalContext {
    
    public enum Dimension {
        OVERWORLD,
        NETHER,
        END,
        CUSTOM
    }
    
    public enum OptimizationProfile {
        BALANCED,           // Default overworld
        HEAVY_CULLING,      // Nether (lots of lava/particles)
        AGGRESSIVE_DISTANCE, // End (void rendering)
        ENTITY_FOCUSED,     // Ocean biomes
        CONSERVATIVE        // Unknown/custom dimensions
    }
    
    public enum BiomeCategory {
        OCEAN, PLAINS, DESERT, FOREST, TAIGA, SWAMP, 
        MOUNTAIN, NETHER, END, CUSTOM
    }
    
    private final Dimension dimension;
    private final BiomeCategory biomeCategory;
    private final boolean isRaining;
    private final int skylightLevel;
    
    public DimensionalContext(Dimension dimension, BiomeCategory biomeCategory, 
                            boolean isRaining, int skylightLevel) {
        this.dimension = dimension;
        this.biomeCategory = biomeCategory;
        this.isRaining = isRaining;
        this.skylightLevel = skylightLevel;
    }
    
    /**
     * Analyze current dimensional context from client state.
     */
    public static DimensionalContext analyze(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            return null;
        }
        
        RegistryKey<World> dimKey = client.world.getRegistryKey();
        Dimension dim = identifyDimension(dimKey);
        
        // Biome detection
        BiomeCategory biome;
        try {
            var biomeEntry = client.world.getBiome(client.player.getBlockPos());
            biome = categorizeBiome(biomeEntry.getKey().orElse(null));
        } catch (Exception e) {
            biome = BiomeCategory.CUSTOM;
        }
        
        boolean raining = client.world.isRaining();
        int skylight = client.world.getAmbientDarkness();
        
        return new DimensionalContext(dim, biome, raining, skylight);
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
     * Categorize biome for optimization purposes.
     */
    private static BiomeCategory categorizeBiome(RegistryKey<?> biomeKey) {
        if (biomeKey == null) {
            return BiomeCategory.CUSTOM;
        }
        
        String biomeName = biomeKey.getValue().getPath().toLowerCase();
        
        if (biomeName.contains("ocean") || biomeName.contains("sea")) {
            return BiomeCategory.OCEAN;
        } else if (biomeName.contains("desert")) {
            return BiomeCategory.DESERT;
        } else if (biomeName.contains("forest") || biomeName.contains("jungle")) {
            return BiomeCategory.FOREST;
        } else if (biomeName.contains("taiga") || biomeName.contains("snowy")) {
            return BiomeCategory.TAIGA;
        } else if (biomeName.contains("swamp")) {
            return BiomeCategory.SWAMP;
        } else if (biomeName.contains("mountain") || biomeName.contains("peak")) {
            return BiomeCategory.MOUNTAIN;
        } else if (biomeName.contains("nether")) {
            return BiomeCategory.NETHER;
        } else if (biomeName.contains("end")) {
            return BiomeCategory.END;
        } else if (biomeName.contains("plains") || biomeName.contains("meadow")) {
            return BiomeCategory.PLAINS;
        }
        
        return BiomeCategory.CUSTOM;
    }
    
    /**
     * Get recommended optimization profile for current context.
     */
    public OptimizationProfile getRecommendedProfile() {
        return switch (dimension) {
            case NETHER -> OptimizationProfile.HEAVY_CULLING; // Heavy lava rendering
            case END -> OptimizationProfile.AGGRESSIVE_DISTANCE; // Void rendering
            case OVERWORLD -> {
                if (biomeCategory == BiomeCategory.OCEAN) {
                    yield OptimizationProfile.ENTITY_FOCUSED; // Many water entities
                }
                yield OptimizationProfile.BALANCED;
            }
            case CUSTOM -> OptimizationProfile.CONSERVATIVE;
        };
    }
    
    public Dimension getDimension() { return dimension; }
    public BiomeCategory getBiomeCategory() { return biomeCategory; }
    public boolean isRaining() { return isRaining; }
    public int getSkylightLevel() { return skylightLevel; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DimensionalContext that)) return false;
        return dimension == that.dimension && biomeCategory == that.biomeCategory;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dimension, biomeCategory);
    }
    
    @Override
    public String toString() {
        return "DimensionalContext[" + dimension + ", " + biomeCategory + 
               ", rain=" + isRaining + "]";
    }
}
