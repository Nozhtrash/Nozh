package dev.nozh.core.preset;

import dev.nozh.NozhConstants;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashSet;
import java.util.Set;

/**
 * Modpack Detector - Identifies the runtime environment.
 * 
 * Purpose:
 * 1. Detect if running in a known heavy modpack (ATM, RLCraft equivalent)
 * 2. Detect conflicting mods (OptiFine, Sodium)
 * 3. Provide context for auto-configuration
 */
public final class ModpackDetector {

    private static final ModpackDetector INSTANCE = new ModpackDetector();
    
    private final Set<String> loadedMods = new HashSet<>();
    private ModpackType detectedType = ModpackType.UNKNOWN;

    public enum ModpackType {
        UNKNOWN,
        VANILLA_PLUS,   // < 50 mods
        KITCHEN_SINK,   // > 150 mods, mix of tech/magic
        HEAVY_TECH,     // Create, Mekanism dominating
        HEAVY_MAGIC,    // Botania, Ars Nouveau dominating
        PERFORMANCE_FOCUSED // Sodium, Lithium, etc.
    }

    private ModpackDetector() {
        scanMods();
    }

    public static ModpackDetector getInstance() {
        return INSTANCE;
    }

    private void scanMods() {
        // In a real env, we iterate FabricLoader.getAllMods()
        // Here we simulate detection or check for specific IDs if possible
        try {
            FabricLoader.getInstance().getAllMods().forEach(mod -> {
                loadedMods.add(mod.getMetadata().getId());
            });
            
            analyzeType();
            NozhConstants.LOGGER.info("[NOZH] Detected Environment: {} ({} mods loaded)", 
                detectedType, loadedMods.size());
            
        } catch (Throwable t) {
            // Fallback for tests or missing FabricLoader
            NozhConstants.LOGGER.warn("[NOZH] Mod scanning failed, assuming Vanilla");
            detectedType = ModpackType.UNKNOWN;
        }
    }

    private void analyzeType() {
        int count = loadedMods.size();
        
        boolean hasSodium = loadedMods.contains("sodium") || loadedMods.contains("embeddium");
        boolean hasCreate = loadedMods.contains("create");
        boolean hasMekanism = loadedMods.contains("mekanism");
        boolean hasBotania = loadedMods.contains("botania");
        
        if (count < 50) {
            if (hasSodium) {
                detectedType = ModpackType.PERFORMANCE_FOCUSED;
            } else {
                detectedType = ModpackType.VANILLA_PLUS;
            }
            return;
        }
        
        if (count > 150) {
            if (hasCreate || hasMekanism) {
                detectedType = ModpackType.HEAVY_TECH;
            } else if (hasBotania) {
                detectedType = ModpackType.HEAVY_MAGIC;
            } else {
                detectedType = ModpackType.KITCHEN_SINK;
            }
            return;
        }
        
        detectedType = ModpackType.VANILLA_PLUS;
    }

    public ModpackType getDetectedType() {
        return detectedType;
    }
    
    public boolean isModLoaded(String modId) {
        return loadedMods.contains(modId);
    }
}
