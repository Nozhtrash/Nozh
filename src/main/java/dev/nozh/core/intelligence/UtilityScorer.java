package dev.nozh.core.intelligence;

import dev.nozh.core.context.Scenario;
import dev.nozh.core.context.ScenarioSnapshot;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculates utility scores for actions based on context.
 * 
 * Utility = Expected FPS Gain × Context Appropriateness × Safety
 * 
 * Higher utility = better action for current situation.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public final class UtilityScorer {

    private static final double MIN_UTILITY_THRESHOLD = 0.3;
    
    // Action impact estimates (FPS gain per action)
    private static final Map<String, Double> ACTION_FPS_IMPACT = new HashMap<>();
    
    // Action appropriateness per scenario
    private static final Map<Scenario, Map<String, Double>> SCENARIO_WEIGHTS = new HashMap<>();
    
    static {
        // Initialize FPS impact estimates
        ACTION_FPS_IMPACT.put("reduce_render_distance", 15.0);
        ACTION_FPS_IMPACT.put("lower_particles", 5.0);
        ACTION_FPS_IMPACT.put("disable_clouds", 3.0);
        ACTION_FPS_IMPACT.put("reduce_shadows", 8.0);
        ACTION_FPS_IMPACT.put("lower_entity_distance", 10.0);
        ACTION_FPS_IMPACT.put("disable_animations", 4.0);
        ACTION_FPS_IMPACT.put("reduce_simulation_distance", 12.0);
        ACTION_FPS_IMPACT.put("lower_graphics_quality", 7.0);
        
        // Initialize scenario-specific weights
        initializeScenarioWeights();
    }
    
    private static void initializeScenarioWeights() {
        // COMBAT: Prioritize visibility and responsiveness
        Map<String, Double> combatWeights = new HashMap<>();
        combatWeights.put("reduce_render_distance", 0.6); // Risky in combat
        combatWeights.put("lower_particles", 1.0);
        combatWeights.put("disable_clouds", 1.0);
        combatWeights.put("reduce_shadows", 0.8);
        combatWeights.put("lower_entity_distance", 0.4); // Very risky
        combatWeights.put("disable_animations", 0.9);
        combatWeights.put("reduce_simulation_distance", 0.7);
        combatWeights.put("lower_graphics_quality", 0.9);
        SCENARIO_WEIGHTS.put(Scenario.COMBAT, combatWeights);
        
        // BUILDING: Safe to reduce most things
        Map<String, Double> buildingWeights = new HashMap<>();
        buildingWeights.put("reduce_render_distance", 0.8);
        buildingWeights.put("lower_particles", 1.0);
        buildingWeights.put("disable_clouds", 1.0);
        buildingWeights.put("reduce_shadows", 1.0);
        buildingWeights.put("lower_entity_distance", 0.9);
        buildingWeights.put("disable_animations", 0.7); // Blocks matter
        buildingWeights.put("reduce_simulation_distance", 0.9);
        buildingWeights.put("lower_graphics_quality", 0.8);
        SCENARIO_WEIGHTS.put(Scenario.BUILDING, buildingWeights);
        
        // EXPLORING: Need visibility
        Map<String, Double> exploringWeights = new HashMap<>();
        exploringWeights.put("reduce_render_distance", 0.5); // Bad for exploring
        exploringWeights.put("lower_particles", 1.0);
        exploringWeights.put("disable_clouds", 1.0);
        exploringWeights.put("reduce_shadows", 0.9);
        exploringWeights.put("lower_entity_distance", 0.7);
        exploringWeights.put("disable_animations", 0.9);
        exploringWeights.put("reduce_simulation_distance", 0.6);
        exploringWeights.put("lower_graphics_quality", 0.8);
        SCENARIO_WEIGHTS.put(Scenario.EXPLORING, exploringWeights);
        
        // AFK: Aggressive optimization OK
        Map<String, Double> afkWeights = new HashMap<>();
        afkWeights.put("reduce_render_distance", 1.0);
        afkWeights.put("lower_particles", 1.0);
        afkWeights.put("disable_clouds", 1.0);
        afkWeights.put("reduce_shadows", 1.0);
        afkWeights.put("lower_entity_distance", 1.0);
        afkWeights.put("disable_animations", 1.0);
        afkWeights.put("reduce_simulation_distance", 1.0);
        afkWeights.put("lower_graphics_quality", 1.0);
        SCENARIO_WEIGHTS.put(Scenario.AFK, afkWeights);
        
        // LOADING: Need to be cautious
        Map<String, Double> loadingWeights = new HashMap<>();
        loadingWeights.put("reduce_render_distance", 0.9);
        loadingWeights.put("lower_particles", 1.0);
        loadingWeights.put("disable_clouds", 1.0);
        loadingWeights.put("reduce_shadows", 0.9);
        loadingWeights.put("lower_entity_distance", 0.8);
        loadingWeights.put("disable_animations", 1.0);
        loadingWeights.put("reduce_simulation_distance", 0.7);
        loadingWeights.put("lower_graphics_quality", 0.9);
        SCENARIO_WEIGHTS.put(Scenario.LOADING, loadingWeights);
        
        // STANDARD: Balanced
        Map<String, Double> standardWeights = new HashMap<>();
        standardWeights.put("reduce_render_distance", 0.8);
        standardWeights.put("lower_particles", 1.0);
        standardWeights.put("disable_clouds", 1.0);
        standardWeights.put("reduce_shadows", 0.9);
        standardWeights.put("lower_entity_distance", 0.8);
        standardWeights.put("disable_animations", 0.9);
        standardWeights.put("reduce_simulation_distance", 0.8);
        standardWeights.put("lower_graphics_quality", 0.9);
        SCENARIO_WEIGHTS.put(Scenario.STANDARD, standardWeights);
        
        // MENU: Minimal optimization needed
        Map<String, Double> menuWeights = new HashMap<>();
        menuWeights.put("reduce_render_distance", 0.5);
        menuWeights.put("lower_particles", 0.8);
        menuWeights.put("disable_clouds", 0.8);
        menuWeights.put("reduce_shadows", 0.7);
        menuWeights.put("lower_entity_distance", 0.5);
        menuWeights.put("disable_animations", 0.6);
        menuWeights.put("reduce_simulation_distance", 0.5);
        menuWeights.put("lower_graphics_quality", 0.6);
        SCENARIO_WEIGHTS.put(Scenario.MENU, menuWeights);
    }

    /**
     * Calculate utility score for an action.
     * 
     * @param actionId action to evaluate
     * @param scenario current scenario context
     * @param telemetry current performance metrics
     * @return utility score (0.0 - 1.0+, higher is better)
     */
    public double calculateUtility(String actionId, ScenarioSnapshot scenario, TelemetrySnapshot telemetry) {
        if (actionId == null || scenario == null || telemetry == null) {
            return 0.0;
        }
        
        // 1. Base FPS impact
        double expectedFpsGain = ACTION_FPS_IMPACT.getOrDefault(actionId, 5.0);
        
        // 2. Scenario appropriateness
        double scenarioWeight = getScenarioWeight(actionId, scenario.scenario());
        
        // 3. Urgency based on current FPS
        double currentFps = 1000.0 / telemetry.avgFrametimeMs();
        double urgency = calculateUrgency(currentFps);
        
        // 4. Spike severity (if having frequent spikes, more urgent)
        double spikeMultiplier = 1.0 + (telemetry.spikeCount() * 0.1);
        
        // Final utility = Impact × Appropriateness × Urgency × SpikeFactor
        double utility = (expectedFpsGain / 20.0) * scenarioWeight * urgency * spikeMultiplier;
        
        return Math.max(0.0, utility);
    }
    
    /**
     * Get scenario-specific weight for action.
     */
    private double getScenarioWeight(String actionId, Scenario scenario) {
        Map<String, Double> weights = SCENARIO_WEIGHTS.get(scenario);
        if (weights == null) {
            return 0.7; // Default moderate weight
        }
        return weights.getOrDefault(actionId, 0.7);
    }
    
    /**
     * Calculate urgency multiplier based on current FPS.
     * 
     * Low FPS = high urgency
     * High FPS = low urgency
     */
    private double calculateUrgency(double currentFps) {
        if (currentFps >= 60) {
            return 0.3; // Low urgency, already smooth
        } else if (currentFps >= 45) {
            return 0.6; // Medium urgency
        } else if (currentFps >= 30) {
            return 0.9; // High urgency
        } else {
            return 1.2; // Critical urgency
        }
    }
    
    /**
     * Check if utility meets minimum threshold.
     */
    public boolean meetsThreshold(double utility) {
        return utility >= MIN_UTILITY_THRESHOLD;
    }
    
    /**
     * Get minimum utility threshold.
     */
    public double getMinThreshold() {
        return MIN_UTILITY_THRESHOLD;
    }
}
