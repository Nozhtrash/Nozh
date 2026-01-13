package dev.nozh.client;

import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;

/**
 * Configuration presets for different hardware tiers.
 */
public final class ConfigPresets {

    private ConfigPresets() {
    }

    /**
     * Low-End PC preset - conservative, stability-focused
     */
    public static void applyLowEnd() {
        NozhConfig config = ConfigManager.getConfig();
        config.enabled = true;
        config.allowAutoTuning = true;
        config.targetFps = 30;
        config.allowGameplayImpactActions = true; // Allow more aggressive changes
        config.rollbackEnabled = true;
        config.cooldownActionMillis = 60000; // 1 min
        config.maxChangesPerSession = 5; // Allow more changes
        ConfigManager.saveAndNotify();
    }

    /**
     * Mid-Range PC preset - balanced
     */
    public static void applyMidRange() {
        NozhConfig config = ConfigManager.getConfig();
        config.enabled = true;
        config.allowAutoTuning = true;
        config.targetFps = 60;
        config.allowGameplayImpactActions = false;
        config.rollbackEnabled = true;
        config.cooldownActionMillis = 120000; // 2 min (default)
        config.maxChangesPerSession = 2; // Default
        ConfigManager.saveAndNotify();
    }

    /**
     * High-End PC preset - minimal intervention
     */
    public static void applyHighEnd() {
        NozhConfig config = ConfigManager.getConfig();
        config.enabled = true;
        config.allowAutoTuning = true;
        config.targetFps = 90;
        config.allowGameplayImpactActions = false;
        config.rollbackEnabled = true;
        config.cooldownActionMillis = 180000; // 3 min
        config.maxChangesPerSession = 1; // Minimal changes
        ConfigManager.saveAndNotify();
    }
    /**
     * Extreme Potato Mode - for the absolute lowest end hardware.
     */
    public static void applyExtreme() {
        NozhConfig config = ConfigManager.getConfig();
        config.enabled = true;
        config.allowAutoTuning = true;
        config.optimizationProfile = "EXTREME"; // Explicitly set profile
        config.targetFps = 20; // Lower target to keep expectations real
        config.allowGameplayImpactActions = true;
        config.safeModeForce = true; // Force safe mode features
        config.rollbackEnabled = false; // Disable rollback to force settings to stick
        config.cooldownActionMillis = 30000;
        config.maxChangesPerSession = 10;
        
        // Also apply visual settings directly
        config.adaptiveVisualQualityEnabled = true;
        config.adaptiveVisualQualityMinStep = 0; // Floor it
        
        ConfigManager.saveAndNotify();
    }
}
