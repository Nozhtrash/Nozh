package dev.nozh.core.config;

import dev.nozh.NozhConstants;

import java.util.*;

/**
 * Pre-made configuration presets for different use cases.
 * Provides quick setup for common scenarios.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class ConfigPresetManager {

    /**
     * Available configuration presets.
     */
    public enum Preset {
        DEFAULT("Default", "Balanced for most users"),
        PERFORMANCE("Performance", "Maximum FPS, reduced quality"),
        QUALITY("Quality", "Maximum visuals, stable FPS"),
        POTATO("Potato", "For weak PCs"),
        STREAMING("Streaming", "Optimized for OBS/streaming"),
        RECORDING("Recording", "Stable FPS for recording"),
        COMPETITIVE("Competitive", "Max FPS + low latency for PvP"),
        PHOTOGRAPHY("Photography", "Max quality for screenshots"),
        CUSTOM("Custom", "User-defined settings");

        public final String displayName;
        public final String description;

        Preset(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    /**
     * Preset configuration values.
     */
    public record PresetConfig(
            double targetFps,
            String scalingMode,
            boolean potatoModeEnabled,
            String potatoLevel,
            String hudPreset,
            boolean aggressiveOptimizations,
            int renderDistanceOverride,
            boolean enableSuggestions) {
    }

    private Preset currentPreset;
    private final Map<String, PresetConfig> customPresets;

    /**
     * Constructs a new ConfigPresetManager.
     */
    public ConfigPresetManager() {
        this.currentPreset = Preset.DEFAULT;
        this.customPresets = new HashMap<>();
    }

    /**
     * Gets configuration for a preset.
     * 
     * @param preset the preset
     * @return preset configuration
     */
    public PresetConfig getPresetConfig(Preset preset) {
        return switch (preset) {
            case DEFAULT -> new PresetConfig(
                    60.0, "BALANCED", false, null, "MINIMAL", false, -1, true);
            case PERFORMANCE -> new PresetConfig(
                    120.0, "PERFORMANCE", false, null, "GAMER", true, 8, true);
            case QUALITY -> new PresetConfig(
                    60.0, "QUALITY", false, null, "MINIMAL", false, 16, false);
            case POTATO -> new PresetConfig(
                    30.0, "POTATO", true, "LEVEL_3", "MINIMAL", true, 6, true);
            case STREAMING -> new PresetConfig(
                    60.0, "BALANCED", false, null, "STREAMER", true, 10, false);
            case RECORDING -> new PresetConfig(
                    60.0, "BALANCED", false, null, "MINIMAL", true, 12, false);
            case COMPETITIVE -> new PresetConfig(
                    144.0, "PERFORMANCE", false, null, "GAMER", true, 6, false);
            case PHOTOGRAPHY -> new PresetConfig(
                    30.0, "QUALITY", false, null, "OFF", false, 32, false);
            case CUSTOM -> getCustomConfig();
        };
    }

    /**
     * Applies a preset.
     * 
     * @param preset preset to apply
     */
    /**
     * Applies a preset to the given configuration.
     * 
     * @param preset       the preset to apply
     * @param targetConfig the configuration object to modify
     */
    public void applyPreset(Preset preset, NozhConfig targetConfig) {
        if (targetConfig == null)
            return;

        this.currentPreset = preset;
        PresetConfig config = getPresetConfig(preset);

        targetConfig.targetFps = (int) config.targetFps();
        targetConfig.optimizationProfile = config.scalingMode();
        // Potato mode fields logic
        if (config.potatoModeEnabled()) {
            // In a real scenario we might trigger potato engine,
            // but here we just set config flags compatible with it if they existed in
            // NozhConfig
            // For now we assume NozhConfig might interpret optimizationProfile="POTATO" as
            // enabling it
            if ("POTATO".equals(config.scalingMode())) {
                targetConfig.adaptiveVisualQualityEnabled = true;
            }
        }

        targetConfig.showHudSuggestions = config.enableSuggestions();
        targetConfig.hudMode = config.hudPreset();

        // Handle render distance override if applicable
        // Note: Render distance is usually handled by Minecraft settings, not
        // NozhConfig directly
        // unless Nozh controls it dynamically. We will log it as a suggestion.
        if (config.renderDistanceOverride() > 0) {
            NozhConstants.LOGGER.info("Preset '{}' suggests Render Distance: {}",
                    preset.displayName, config.renderDistanceOverride());
        }

        NozhConstants.LOGGER.info("Applied preset: {} ({})",
                preset.displayName, preset.description);
    }

    /**
     * Gets current preset.
     * 
     * @return current preset
     */
    public Preset getCurrentPreset() {
        return currentPreset;
    }

    /**
     * Saves current settings as a custom preset.
     * 
     * @param name   preset name
     * @param config configuration to save
     */
    public void saveAsCustomPreset(String name, PresetConfig config) {
        customPresets.put(name, config);
        NozhConstants.LOGGER.info("Saved custom preset: {}", name);
    }

    /**
     * Gets list of custom preset names.
     * 
     * @return list of names
     */
    public List<String> getCustomPresets() {
        return new ArrayList<>(customPresets.keySet());
    }

    /**
     * Gets a custom preset by name.
     * 
     * @param name preset name
     * @return preset config or null
     */
    public PresetConfig getCustomPresetConfig(String name) {
        return customPresets.get(name);
    }

    /**
     * Gets custom config or default.
     */
    private PresetConfig getCustomConfig() {
        // Return first custom or default
        if (!customPresets.isEmpty()) {
            return customPresets.values().iterator().next();
        }
        return getPresetConfig(Preset.DEFAULT);
    }

    /**
     * Gets all available presets.
     * 
     * @return array of presets
     */
    public static Preset[] getAvailablePresets() {
        return Preset.values();
    }

    /**
     * Deletes a custom preset.
     * 
     * @param name preset name
     */
    public void deleteCustomPreset(String name) {
        customPresets.remove(name);
        NozhConstants.LOGGER.info("Deleted custom preset: {}", name);
    }
}
