package dev.nozh.core.compat;

import dev.nozh.NozhConstants;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects presence of other mods for compatibility.
 * Uses FabricLoader.isModLoaded() for reliable detection.
 */
public final class ModDetector {
    
    // Known performance mods
    private static final String[] KNOWN_MODS = {
        "sodium",
        "iris",
        "starlight",
        "lithium",
        "ferritecore",
        "modernfix",
        "entityculling",
        "moreculling",
        "indium",
        "phosphor",
        "krypton"
    };
    
    private static List<String> detectedMods = null;
    
    private ModDetector() {
        // Utility class
    }
    
    /**
     * Detect all known performance mods
     */
    public static void detect() {
        detectedMods = new ArrayList<>();
        
        for (String modId : KNOWN_MODS) {
            if (isModLoaded(modId)) {
                detectedMods.add(modId);
                NozhConstants.LOGGER.info("Detected mod: {}", modId);
            }
        }
        
        if (detectedMods.isEmpty()) {
            NozhConstants.LOGGER.info("No known performance mods detected");
        } else {
            NozhConstants.LOGGER.info("Detected {} performance mods", detectedMods.size());
        }
    }
    
    /**
     * Check if a specific mod is loaded
     */
    public static boolean isModLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Error checking mod {}: {}", modId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if Sodium is present
     */
    public static boolean hasSodium() {
        return isModLoaded("sodium");
    }
    
    /**
     * Check if Iris is present
     */
    public static boolean hasIris() {
        return isModLoaded("iris");
    }
    
    /**
     * Check if Lithium is present
     */
    public static boolean hasLithium() {
        return isModLoaded("lithium");
    }
    
    /**
     * Check if Starlight is present
     */
    public static boolean hasStarlight() {
        return isModLoaded("starlight");
    }
    
    /**
     * Get list of all detected mods
     */
    public static List<String> getDetectedMods() {
        if (detectedMods == null) {
            detect();
        }
        return new ArrayList<>(detectedMods);
    }
    
    /**
     * Get mod detection summary
     */
    public static String getSummary() {
        List<String> mods = getDetectedMods();
        if (mods.isEmpty()) {
            return "No performance mods detected";
        }
        return "Detected: " + String.join(", ", mods);
    }
}
