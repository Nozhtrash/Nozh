package dev.nozh.core.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nozh.NozhConstants;
import dev.nozh.core.cloud.RemoteConfigFetcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mod Knowledge Base - Centralized intelligence about other mods.
 * 
 * Instead of hardcoding mod IDs in various detectors, we use this registry.
 * It is populated by defaults and updated via Cloud.
 */
public final class ModKnowledgeBase {

    private static final ModKnowledgeBase INSTANCE = new ModKnowledgeBase();
    
    // Knowledge Store
    private final Map<String, ModInfo> knownMods = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> categoryIndex = new ConcurrentHashMap<>();

    // Categories
    public static final String CAT_OPTIMIZATION = "optimization";
    public static final String CAT_CONTENT_TECH = "content_tech";
    public static final String CAT_CONTENT_MAGIC = "content_magic";
    public static final String CAT_RENDERING = "rendering";
    public static final String CAT_UTILITY = "utility";

    private ModKnowledgeBase() {
        loadDefaults();
    }

    public static ModKnowledgeBase getInstance() {
        return INSTANCE;
    }

    /**
     * initialize and fetch updates asynchronously.
     */
    public void init() {
        RemoteConfigFetcher.getInstance().fetch().thenAccept(this::updateFromCloud);
    }

    private void loadDefaults() {
        // Optimization Mods
        register("sodium", CAT_OPTIMIZATION, true);
        register("lithium", CAT_OPTIMIZATION, true);
        register("starlight", CAT_OPTIMIZATION, true);
        register("ferritecore", CAT_OPTIMIZATION, false); // Memory, not pure FPS
        register("modernfix", CAT_OPTIMIZATION, true);
        
        // Rendering Mods (Potentially conflicting)
        register("iris", CAT_RENDERING, true); // Shaders = Heavy
        register("canvas", CAT_RENDERING, true);
        register("vulkanmod", CAT_RENDERING, true);
        register("distant-horizons", CAT_RENDERING, true);
        
        // Content Mods (Heavy)
        register("create", CAT_CONTENT_TECH, true);
        register("mekanism", CAT_CONTENT_TECH, true);
        register("botania", CAT_CONTENT_MAGIC, true);
        register("iceandfire", CAT_CONTENT_MAGIC, true);
        
        // Utility
        register("jei", CAT_UTILITY, false);
        register("rei", CAT_UTILITY, false);
    }

    private void updateFromCloud(JsonObject root) {
        if (root == null || !root.has("mods")) return;
        
        try {
            JsonArray modsArray = root.getAsJsonArray("mods");
            int updated = 0;
            
            for (JsonElement el : modsArray) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String category = obj.get("category").getAsString();
                boolean isHeavy = obj.has("heavy") && obj.get("heavy").getAsBoolean();
                
                register(id, category, isHeavy);
                updated++;
            }
            
            NozhConstants.LOGGER.info("[NOZH] Updated knowledge base with {} mods from cloud", updated);
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("[NOZH] Failed to process cloud mod updates", e);
        }
    }

    private void register(String id, String category, boolean isHeavy) {
        knownMods.put(id, new ModInfo(id, category, isHeavy));
        categoryIndex.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(id);
    }

    public boolean isModKnown(String modId) {
        return knownMods.containsKey(modId);
    }

    public boolean isOptimizationMod(String modId) {
        ModInfo info = knownMods.get(modId);
        return info != null && CAT_OPTIMIZATION.equals(info.category);
    }

    public boolean isHeavyMod(String modId) {
        ModInfo info = knownMods.get(modId);
        return info != null && info.isHeavy;
    }

    public String getCategory(String modId) {
        ModInfo info = knownMods.get(modId);
        return info != null ? info.category : "unknown";
    }
    
    public Set<String> getModsByCategory(String category) {
        return categoryIndex.getOrDefault(category, Collections.emptySet());
    }

    public record ModInfo(String id, String category, boolean isHeavy) {}
}
