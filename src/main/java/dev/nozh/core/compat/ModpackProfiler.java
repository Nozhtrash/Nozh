package dev.nozh.core.compat;

import dev.nozh.NozhConstants;

import java.util.*;

/**
 * Analyzes entire modpack and creates optimal profile.
 * Detects modpack type and adjusts NOZH behavior accordingly.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class ModpackProfiler {

    /**
     * Modpack type classification.
     */
    public enum ModpackType {
        VANILLA_PLUS("Vanilla+", "Few mods enhancing vanilla experience"),
        PERFORMANCE("Performance", "Optimization-focused modpack"),
        TECH_HEAVY("Tech Heavy", "Industrial/automation mods like Create, Mekanism"),
        MAGIC_HEAVY("Magic Heavy", "Magic mods like Botania, Ars Nouveau"),
        ADVENTURE("Adventure", "Exploration, dungeons, quests"),
        KITCHEN_SINK("Kitchen Sink", "Everything included"),
        UNKNOWN("Unknown", "Unclassified modpack type");

        public final String displayName;
        public final String description;

        ModpackType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    /**
     * Analyzed modpack profile.
     */
    public record ModpackProfile(
            ModpackType type,
            int totalMods,
            int optimizationMods,
            int contentMods,
            int libraryMods,
            double estimatedLoad,
            Map<ModKnowledgeBase.ModCategory, Integer> categoryBreakdown,
            List<String> recommendations) {
        /**
         * Gets a summary string.
         *
         * @return human-readable summary
         */
        public String getSummary() {
            return String.format("%s modpack: %d mods (%d optimization, %d content) | Load: %.1f",
                    type.displayName, totalMods, optimizationMods, contentMods, estimatedLoad);
        }
    }

    private final Set<String> loadedMods;

    /**
     * Constructs a new ModpackProfiler.
     */
    public ModpackProfiler() {
        this.loadedMods = new HashSet<>();
    }

    /**
     * Registers a loaded mod.
     *
     * @param modId mod identifier
     */
    public void registerMod(String modId) {
        loadedMods.add(modId.toLowerCase());
    }

    /**
     * Analyzes the current modpack.
     *
     * @return modpack profile
     */
    public ModpackProfile analyzeModpack() {
        Map<ModKnowledgeBase.ModCategory, Integer> categoryBreakdown = new EnumMap<>(
                ModKnowledgeBase.ModCategory.class);
        int optimizationMods = 0;
        int contentMods = 0;
        int libraryMods = 0;
        double loadEstimate = 1.0;

        for (String modId : loadedMods) {
            Optional<ModKnowledgeBase.ModInfo> info = ModKnowledgeBase.getModInfo(modId);

            if (info.isPresent()) {
                ModKnowledgeBase.ModInfo mod = info.get();
                categoryBreakdown.merge(mod.category(), 1, Integer::sum);

                switch (mod.category()) {
                    case OPTIMIZATION -> optimizationMods++;
                    case CONTENT -> contentMods++;
                    case LIBRARY -> libraryMods++;
                    default -> {
                    }
                }

                loadEstimate *= mod.impact().multiplier;
            }
        }

        // Determine modpack type
        ModpackType type = determineType(loadedMods.size(), optimizationMods, contentMods, categoryBreakdown);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(type, optimizationMods, loadedMods);

        return new ModpackProfile(
                type,
                loadedMods.size(),
                optimizationMods,
                contentMods,
                libraryMods,
                loadEstimate,
                categoryBreakdown,
                recommendations);
    }

    /**
     * Determines modpack type based on mod composition.
     */
    private ModpackType determineType(int total, int optimization, int content,
            Map<ModKnowledgeBase.ModCategory, Integer> breakdown) {
        if (total <= 10) {
            return optimization > content ? ModpackType.PERFORMANCE : ModpackType.VANILLA_PLUS;
        }

        if (optimization > total * 0.3) {
            return ModpackType.PERFORMANCE;
        }

        // Check for tech/magic heavy
        int techMods = breakdown.getOrDefault(ModKnowledgeBase.ModCategory.CONTENT, 0);

        if (total > 100) {
            return ModpackType.KITCHEN_SINK;
        }

        if (content > total * 0.5) {
            return ModpackType.ADVENTURE;
        }

        return ModpackType.UNKNOWN;
    }

    /**
     * Generates recommendations for the modpack.
     */
    private List<String> generateRecommendations(ModpackType type, int optimizationMods, Set<String> mods) {
        List<String> recs = new ArrayList<>();

        // Check for missing essential mods
        if (!mods.contains("sodium")) {
            recs.add("Install Sodium for significant FPS improvement");
        }
        if (!mods.contains("lithium")) {
            recs.add("Install Lithium for server-side optimizations");
        }
        if (!mods.contains("ferritecore")) {
            recs.add("Install FerriteCore for memory optimization");
        }

        // Type-specific recommendations
        switch (type) {
            case KITCHEN_SINK -> {
                recs.add("Large modpack detected - consider enabling Potato Mode");
                recs.add("Reduce render distance to 8-10 chunks");
            }
            case TECH_HEAVY -> {
                recs.add("Tech mods can be CPU-intensive - monitor chunk loading");
            }
            case PERFORMANCE -> {
                if (recs.isEmpty())
                    recs.add("Optimization stack looks good!");
            }
            default -> {
            }
        }

        return recs;
    }

    /**
     * Applies optimal settings based on modpack profile.
     *
     * @param profile modpack profile
     */
    public void applyOptimalSettings(ModpackProfile profile) {
        NozhConstants.LOGGER.info("Applying {} profile settings", profile.type().displayName);

        // Here we would adjust NOZH settings based on profile
        // For now, just log the recommendations
        profile.recommendations().forEach(rec -> NozhConstants.LOGGER.info("Recommendation: {}", rec));
    }

    /**
     * Gets count of loaded mods.
     *
     * @return mod count
     */
    public int getLoadedModCount() {
        return loadedMods.size();
    }
}
