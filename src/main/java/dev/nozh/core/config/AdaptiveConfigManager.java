package dev.nozh.core.config;

import dev.nozh.core.context.Scenario;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages adaptive runtime configuration.
 * 
 * Adjusts system parameters based on:
 * - Hardware capability
 * - Current performance
 * - Learning outcomes
 * - Player behavior patterns
 * 
 * Enables personalized optimization.
 * 
 * INTEGRATION: Adaptive configuration
 */
public final class AdaptiveConfigManager {

    private final Map<String, ConfigValue> config = new ConcurrentHashMap<>();
    private final Map<Scenario, ScenarioConfig> scenarioConfigs = new EnumMap<>(Scenario.class);

    public AdaptiveConfigManager() {
        initializeDefaults();
    }

    /**
     * Get configuration value.
     */
    public double getValue(String key, double defaultValue) {
        ConfigValue value = config.get(key);
        return value != null ? value.value : defaultValue;
    }

    /**
     * Set configuration value.
     */
    public void setValue(String key, double value) {
        config.put(key, new ConfigValue(value, System.currentTimeMillis()));
    }

    /**
     * Adapt value based on performance.
     */
    public void adaptValue(String key, double performanceMetric, double targetMetric) {
        ConfigValue current = config.get(key);
        if (current == null) {
            return;
        }

        // Simple adaptive adjustment
        double error = targetMetric - performanceMetric;
        double adjustment = error * 0.1; // 10% correction

        double newValue = Math.max(0.0, Math.min(200.0, current.value + adjustment));
        setValue(key, newValue);
    }

    /**
     * Get scenario-specific configuration.
     */
    public ScenarioConfig getScenarioConfig(Scenario scenario) {
        return scenarioConfigs.computeIfAbsent(scenario, k -> new ScenarioConfig());
    }

    /**
     * Reset to defaults.
     */
    public void resetToDefaults() {
        config.clear();
        scenarioConfigs.clear();
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Default values
        setValue("target_fps", 60.0);
        setValue("min_fps", 30.0);
        setValue("decision_interval_ms", 2000.0);
        setValue("warmup_duration_s", 30.0);
        setValue("exploration_rate", 0.15);
        setValue("learning_rate", 0.1);
    }

    /**
     * Configuration value with timestamp.
     */
    private static class ConfigValue {
        double value;
        long lastModified;

        ConfigValue(double value, long lastModified) {
            this.value = value;
            this.lastModified = lastModified;
        }
    }

    /**
     * Scenario-specific configuration.
     */
    public static class ScenarioConfig {
        public double aggressiveness = 0.5;
        public double visualPriority = 0.5;
        public double gameplayPriority = 0.8;

        public void adjust(double performance, double target) {
            if (performance < target) {
                aggressiveness = Math.min(1.0, aggressiveness + 0.1);
            } else if (performance > target * 1.2) {
                aggressiveness = Math.max(0.0, aggressiveness - 0.05);
            }
        }
    }
}
