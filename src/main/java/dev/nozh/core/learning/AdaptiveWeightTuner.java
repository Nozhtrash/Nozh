package dev.nozh.core.learning;

import dev.nozh.core.context.Scenario;
import java.util.EnumMap;
import java.util.Map;

/**
 * Adaptive tuning of UtilityScorer weights based on learning.
 * 
 * Adjusts FPS/Visual/Gameplay weight ratios based on:
 * - User hardware profile
 * - Historical action effectiveness
 * - Scenario-specific patterns
 * 
 * Enables personalized optimization strategies.
 * 
 * TASK 10: Adaptive learning - weight optimization
 */
public final class AdaptiveWeightTuner {

    private final Map<Scenario, ScenarioWeights> scenarioWeights = new EnumMap<>(Scenario.class);
    private final ActionEffectivenessTracker effectivenessTracker;

    private static final double TUNING_RATE = 0.05; // How fast to adapt
    private static final double MIN_WEIGHT = 0.1;
    private static final double MAX_WEIGHT = 1.5;

    public AdaptiveWeightTuner(ActionEffectivenessTracker tracker) {
        this.effectivenessTracker = tracker;
        initializeDefaultWeights();
    }

    /**
     * Get current weights for scenario.
     */
    public ScenarioWeights getWeights(Scenario scenario) {
        return scenarioWeights.getOrDefault(scenario, getDefaultWeights(scenario));
    }

    /**
     * Adapt weights based on action performance.
     */
    public void adaptWeights(Scenario scenario, String actionId, double actualFpsGain, double visualImpact, double gameplayImpact) {
        ScenarioWeights current = getWeights(scenario);
        double effectiveness = effectivenessTracker.getEffectivenessScore(actionId);

        // If action was effective, reinforce its weight profile
        // If action was ineffective, adjust weights away from it
        double adjustment = (effectiveness - 0.5) * TUNING_RATE;

        // Determine action type and adjust corresponding weight
        if (actualFpsGain > 5.0) {
            // FPS-focused action
            current.fpsWeight = clampWeight(current.fpsWeight + adjustment);
        }
        if (visualImpact < 0.2) {
            // Visual-preserving action
            current.visualWeight = clampWeight(current.visualWeight + adjustment);
        }
        if (gameplayImpact < 0.1) {
            // Gameplay-preserving action
            current.gameplayWeight = clampWeight(current.gameplayWeight + adjustment);
        }

        scenarioWeights.put(scenario, current);
    }

    /**
     * Reset weights to defaults.
     */
    public void resetWeights(Scenario scenario) {
        scenarioWeights.put(scenario, getDefaultWeights(scenario));
    }

    /**
     * Get tuning statistics.
     */
    public Map<Scenario, ScenarioWeights> getAllWeights() {
        return new EnumMap<>(scenarioWeights);
    }

    private void initializeDefaultWeights() {
        for (Scenario scenario : Scenario.values()) {
            scenarioWeights.put(scenario, getDefaultWeights(scenario));
        }
    }

    private ScenarioWeights getDefaultWeights(Scenario scenario) {
        return switch (scenario) {
            case COMBAT -> new ScenarioWeights(1.0, 0.3, 0.1);
            case BUILDING -> new ScenarioWeights(0.6, 0.8, 0.2);
            case EXPLORING -> new ScenarioWeights(0.8, 0.6, 0.1);
            case AFK -> new ScenarioWeights(0.3, 0.1, 0.0);
            case MENU, LOADING -> new ScenarioWeights(0.5, 0.0, 0.0);
            default -> new ScenarioWeights(0.7, 0.5, 0.15);
        };
    }

    private double clampWeight(double weight) {
        return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weight));
    }

    /**
     * Scenario-specific weights.
     */
    public static class ScenarioWeights {
        public double fpsWeight;
        public double visualWeight;
        public double gameplayWeight;

        public ScenarioWeights(double fps, double visual, double gameplay) {
            this.fpsWeight = fps;
            this.visualWeight = visual;
            this.gameplayWeight = gameplay;
        }

        public ScenarioWeights copy() {
            return new ScenarioWeights(fpsWeight, visualWeight, gameplayWeight);
        }
    }
}
