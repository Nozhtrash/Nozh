package dev.nozh.core.context;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionTypes;

/**
 * Captures environmental context for scenario detection.
 * 
 * Tracks:
 * - Current dimension (Overworld/Nether/End)
 * - Biome type
 * - Weather conditions (rain, thunder)
 * - Time of day
 * - GUI state
 * 
 * TASK 3: Scenario confidence - environmental signals
 */
public final class EnvironmentContext {

    private final MinecraftClient client;

    public EnvironmentContext(MinecraftClient client) {
        this.client = client;
    }

    /**
     * Get current dimension type.
     */
    public DimensionType getDimension() {
        ClientWorld world = client.world;
        if (world == null) {
            return DimensionType.UNKNOWN;
        }

        if (world.getDimensionEntry().matchesKey(DimensionTypes.OVERWORLD)) {
            return DimensionType.OVERWORLD;
        }
        if (world.getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER)) {
            return DimensionType.NETHER;
        }
        if (world.getDimensionEntry().matchesKey(DimensionTypes.THE_END)) {
            return DimensionType.END;
        }

        return DimensionType.UNKNOWN;
    }

    /**
     * Check if it's currently raining.
     */
    public boolean isRaining() {
        ClientWorld world = client.world;
        return world != null && world.isRaining();
    }

    /**
     * Check if there's a thunderstorm.
     */
    public boolean isThundering() {
        ClientWorld world = client.world;
        return world != null && world.isThundering();
    }

    /**
     * Get weather severity (0.0-1.0).
     * 0.0 = clear, 0.5 = rain, 1.0 = thunderstorm
     */
    public double getWeatherSeverity() {
        if (isThundering()) {
            return 1.0;
        }
        if (isRaining()) {
            ClientWorld world = client.world;
            if (world != null) {
                float rainGradient = world.getRainGradient(1.0f);
                return 0.5 + (rainGradient * 0.5);
            }
            return 0.5;
        }
        return 0.0;
    }

    /**
     * Check if player is in a GUI/menu.
     */
    public boolean isInGui() {
        return client.currentScreen != null;
    }

    /**
     * Get GUI type if in GUI.
     */
    public String getGuiType() {
        if (client.currentScreen == null) {
            return "none";
        }
        return client.currentScreen.getClass().getSimpleName();
    }

    /**
     * Get current biome at player position.
     */
    public String getBiomeName() {
        if (client.player == null || client.world == null) {
            return "unknown";
        }

        RegistryEntry<Biome> biomeEntry = client.world.getBiome(client.player.getBlockPos());
        if (biomeEntry.getKey().isPresent()) {
            return biomeEntry.getKey().get().getValue().toString();
        }

        return "unknown";
    }

    /**
     * Check if biome is dangerous (Nether, End, Deep Dark).
     */
    public boolean isDangerousBiome() {
        String biome = getBiomeName().toLowerCase();
        return biome.contains("nether") 
            || biome.contains("end") 
            || biome.contains("deep_dark")
            || biome.contains("basalt");
    }

    /**
     * Get time of day (0.0-1.0).
     * 0.0 = midnight, 0.5 = noon
     */
    public double getTimeOfDay() {
        ClientWorld world = client.world;
        if (world == null) {
            return 0.0;
        }

        long timeOfDay = world.getTimeOfDay() % 24000;
        return timeOfDay / 24000.0;
    }

    /**
     * Check if it's night time (monsters spawn).
     */
    public boolean isNight() {
        double time = getTimeOfDay();
        // Night is roughly 13000-23000 ticks (0.54-0.96)
        return time > 0.54 && time < 0.96;
    }

    public enum DimensionType {
        OVERWORLD,
        NETHER,
        END,
        UNKNOWN
    }
}
