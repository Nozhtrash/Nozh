package dev.nozh.core.preset;

import dev.nozh.core.context.Scenario;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manages performance presets.
 * 
 * Presets:
 * - ULTRA_PERFORMANCE: Maximum FPS, minimal quality
 * - BALANCED: Good FPS with acceptable quality
 * - QUALITY: Best visuals, playable FPS
 * - CUSTOM: User-defined settings
 * 
 * PRESETS: Quick performance profiles
 */
public final class PresetManager {

    private final Map<Scenario, PerformancePreset> scenarioPresets = new EnumMap<>(Scenario.class);
    private PerformancePreset globalPreset = PerformancePreset.BALANCED;

    public PresetManager() {
        initializeDefaults();
    }

    /**
     * Get preset for scenario.
     */
    public PerformancePreset getPreset(Scenario scenario) {
        return scenarioPresets.getOrDefault(scenario, globalPreset);
    }

    /**
     * Set preset for scenario.
     */
    public void setPreset(Scenario scenario, PerformancePreset preset) {
        scenarioPresets.put(scenario, preset);
    }

    /**
     * Set global preset (applies to all scenarios without specific preset).
     */
    public void setGlobalPreset(PerformancePreset preset) {
        this.globalPreset = preset;
    }

    /**
     * Get global preset.
     */
    public PerformancePreset getGlobalPreset() {
        return globalPreset;
    }

    /**
     * Reset to defaults.
     */
    public void resetToDefaults() {
        scenarioPresets.clear();
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Combat needs maximum performance
        scenarioPresets.put(Scenario.COMBAT, PerformancePreset.ULTRA_PERFORMANCE);
        
        // Building can tolerate lower FPS for better visuals
        scenarioPresets.put(Scenario.BUILDING, PerformancePreset.BALANCED);
        
        // Exploring benefits from quality
        scenarioPresets.put(Scenario.EXPLORING, PerformancePreset.QUALITY);
        
        // AFK can be very aggressive
        scenarioPresets.put(Scenario.AFK, PerformancePreset.ULTRA_PERFORMANCE);
    }

    /**
     * Performance presets.
     */
    public enum PerformancePreset {
        ULTRA_PERFORMANCE("Ultra Performance", 120, 0.1, 0.05),
        BALANCED("Balanced", 60, 0.5, 0.3),
        QUALITY("Quality", 30, 0.8, 0.7),
        CUSTOM("Custom", 60, 0.5, 0.5);

        private final String displayName;
        private final int targetFps;
        private final double visualPriority;
        private final double gameplayPriority;

        PerformancePreset(String displayName, int targetFps, double visualPriority, double gameplayPriority) {
            this.displayName = displayName;
            this.targetFps = targetFps;
            this.visualPriority = visualPriority;
            this.gameplayPriority = gameplayPriority;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getTargetFps() {
            return targetFps;
        }

        public double getVisualPriority() {
            return visualPriority;
        }

        public double getGameplayPriority() {
            return gameplayPriority;
        }

        public String getDescription() {
            return switch (this) {
                case ULTRA_PERFORMANCE -> "Maximum FPS at any cost. Best for competitive combat.";
                case BALANCED -> "Good FPS with acceptable quality. Recommended for most players.";
                case QUALITY -> "Best visuals with playable FPS. For screenshots and building.";
                case CUSTOM -> "User-defined settings.";
            };
        }
    }
}
