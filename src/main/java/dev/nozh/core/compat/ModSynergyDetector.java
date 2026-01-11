package dev.nozh.core.compat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects mod combinations and applies optimal strategies.
 * Some mod combinations work better together.
 * 
 * INTEGRATION: Compatibility and optimization
 * CONTRACT: Thread-safe, mod detection
 */
public final class ModSynergyDetector {

    /**
     * Mod synergy configuration.
     */
    public record ModSynergy(
        Set<String> mods,
        String description,
        Map<String, Double> biasAdjustments,
        List<String> recommendations
    ) {
        public ModSynergy {
            mods = Set.copyOf(mods);
            biasAdjustments = Map.copyOf(biasAdjustments);
            recommendations = List.copyOf(recommendations);
        }
    }

    private static final Map<String, ModSynergy> KNOWN_SYNERGIES = new HashMap<>();
    
    static {
        // Sodium + Iris = GPU focus
        KNOWN_SYNERGIES.put("sodium_iris", new ModSynergy(
            Set.of("sodium", "iris"),
            "Sodium + Iris: Complete rendering stack optimization",
            Map.of(
                "gpu_bias", 1.3,
                "render_quality", 1.2
            ),
            List.of(
                "Increase render distance for better visuals",
                "Enable shader optimizations",
                "GPU-focused optimizations active"
            )
        ));

        // Lithium + C2ME = CPU threading
        KNOWN_SYNERGIES.put("lithium_c2me", new ModSynergy(
            Set.of("lithium", "c2me"),
            "Lithium + C2ME: Enhanced multi-threading",
            Map.of(
                "cpu_bias", 1.4,
                "chunk_threading", 1.5
            ),
            List.of(
                "Increase simulation distance",
                "Enable aggressive chunk loading",
                "CPU threading optimized"
            )
        ));

        // Sodium + Indium + Iris = Full graphics stack
        KNOWN_SYNERGIES.put("sodium_indium_iris", new ModSynergy(
            Set.of("sodium", "indium", "iris"),
            "Sodium + Indium + Iris: Complete graphics pipeline",
            Map.of(
                "gpu_bias", 1.5,
                "render_quality", 1.3,
                "compatibility", 1.2
            ),
            List.of(
                "Maximum render quality available",
                "Full shader support enabled",
                "Best-in-class rendering performance"
            )
        ));

        // Sodium + Lithium = Balanced optimization
        KNOWN_SYNERGIES.put("sodium_lithium", new ModSynergy(
            Set.of("sodium", "lithium"),
            "Sodium + Lithium: Balanced CPU/GPU optimization",
            Map.of(
                "gpu_bias", 1.2,
                "cpu_bias", 1.2,
                "overall", 1.25
            ),
            List.of(
                "Balanced optimizations active",
                "Recommended for most systems",
                "Best overall performance mod combo"
            )
        ));
    }

    private final Map<String, Boolean> loadedMods = new ConcurrentHashMap<>();
    private final List<ModSynergy> detectedSynergies = new ArrayList<>();

    /**
     * Register a loaded mod.
     */
    public void registerMod(String modId) {
        loadedMods.put(modId.toLowerCase(), true);
    }

    /**
     * Check if a mod is loaded.
     */
    public boolean isModLoaded(String modId) {
        return loadedMods.getOrDefault(modId.toLowerCase(), false);
    }

    /**
     * Detect all synergies based on loaded mods.
     */
    public List<ModSynergy> detectSynergies() {
        detectedSynergies.clear();

        for (ModSynergy synergy : KNOWN_SYNERGIES.values()) {
            boolean allLoaded = true;
            for (String requiredMod : synergy.mods()) {
                if (!isModLoaded(requiredMod)) {
                    allLoaded = false;
                    break;
                }
            }

            if (allLoaded) {
                detectedSynergies.add(synergy);
            }
        }

        return new ArrayList<>(detectedSynergies);
    }

    /**
     * Apply synergy optimizations (stub - integration point).
     */
    public void applySynergyOptimizations() {
        List<ModSynergy> synergies = detectSynergies();
        
        // TODO: Integration with governor to apply bias adjustments
        // For now, just detect and report
    }

    /**
     * Get synergy report as formatted string.
     */
    public String getSynergyReport() {
        List<ModSynergy> synergies = detectSynergies();
        
        if (synergies.isEmpty()) {
            return "No mod synergies detected.\n" +
                   "Loaded mods: " + String.join(", ", loadedMods.keySet());
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Detected Mod Synergies ===\n\n");
        
        for (ModSynergy synergy : synergies) {
            report.append("● ").append(synergy.description()).append("\n");
            report.append("  Mods: ").append(String.join(", ", synergy.mods())).append("\n");
            
            if (!synergy.biasAdjustments().isEmpty()) {
                report.append("  Adjustments:\n");
                for (Map.Entry<String, Double> entry : synergy.biasAdjustments().entrySet()) {
                    report.append(String.format("    - %s: %.1fx\n", entry.getKey(), entry.getValue()));
                }
            }
            
            if (!synergy.recommendations().isEmpty()) {
                report.append("  Recommendations:\n");
                for (String rec : synergy.recommendations()) {
                    report.append("    - ").append(rec).append("\n");
                }
            }
            
            report.append("\n");
        }

        report.append("Total synergies: ").append(synergies.size()).append("\n");
        return report.toString();
    }

    /**
     * Get total bias multiplier for a specific type.
     */
    public double getTotalBias(String biasType) {
        double total = 1.0;
        
        for (ModSynergy synergy : detectedSynergies) {
            Double bias = synergy.biasAdjustments().get(biasType);
            if (bias != null) {
                total *= bias;
            }
        }
        
        return total;
    }

    /**
     * Get all loaded mods.
     */
    public Set<String> getLoadedMods() {
        return new HashSet<>(loadedMods.keySet());
    }

    /**
     * Get count of detected synergies.
     */
    public int getSynergyCount() {
        return detectedSynergies.size();
    }

    /**
     * Clear all mod registrations.
     */
    public void clear() {
        loadedMods.clear();
        detectedSynergies.clear();
    }
}
