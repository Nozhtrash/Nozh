package dev.nozh.core.intelligence;

import dev.nozh.core.context.Scenario;
import dev.nozh.core.governor.TradeoffMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects preventive actions before performance degrades.
 * 
 * Works with PerformancePredictor to take action BEFORE spike occurs:
 * - Trend shows degradation → suggest lightweight optimizations
 * - Spike predicted → suggest aggressive optimizations
 * 
 * Prioritizes actions by:
 * 1. Urgency (how soon spike will occur)
 * 2. Effectiveness (expected FPS gain)
 * 3. Reversibility (can we undo easily?)
 * 
 * TASK 8: Predictive intelligence - preventive action
 */
public final class PreventiveActionSelector {

    private final PerformancePredictor predictor;

    public PreventiveActionSelector(PerformancePredictor predictor) {
        this.predictor = predictor;
    }

    /**
     * Get recommended preventive actions.
     */
    public List<String> getRecommendedActions(Scenario scenario) {
        List<String> actions = new ArrayList<>();

        PerformancePredictor.ActionUrgency urgency = predictor.getRecommendedUrgency();
        double confidence = predictor.getPredictionConfidence();

        // Only act if confidence is reasonable
        if (confidence < 0.5) {
            return actions; // Not confident enough
        }

        // Select actions based on urgency and scenario
        switch (urgency) {
            case CRITICAL:
                // Aggressive actions needed NOW
                actions.addAll(getAggressiveActions(scenario));
                break;

            case HIGH:
                // Moderate actions needed soon
                actions.addAll(getModerateActions(scenario));
                break;

            case MEDIUM:
                // Preventive actions (light)
                actions.addAll(getLightActions(scenario));
                break;

            case LOW:
                // Just monitor
                break;
        }

        return actions;
    }

    /**
     * Get aggressive optimization actions.
     */
    private List<String> getAggressiveActions(Scenario scenario) {
        List<String> actions = new ArrayList<>();

        if (scenario == Scenario.COMBAT) {
            // Combat: maximize FPS at all costs
            actions.add("render_distance_reduce_4");
            actions.add("entity_culling_aggressive");
            actions.add("particles_minimal");
            actions.add("shadows_off");
        } else if (scenario == Scenario.EXPLORING) {
            // Exploring: balance FPS and visuals
            actions.add("render_distance_reduce_2");
            actions.add("entity_distance_reduce");
            actions.add("clouds_off");
        } else {
            // Default: moderate reductions
            actions.add("render_distance_reduce_2");
            actions.add("particles_minimal");
            actions.add("clouds_off");
        }

        return actions;
    }

    /**
     * Get moderate optimization actions.
     */
    private List<String> getModerateActions(Scenario scenario) {
        List<String> actions = new ArrayList<>();

        if (scenario == Scenario.COMBAT) {
            actions.add("render_distance_reduce_2");
            actions.add("particles_minimal");
        } else if (scenario == Scenario.BUILDING) {
            // Building: maintain visuals, reduce entities
            actions.add("entity_distance_reduce");
            actions.add("clouds_off");
        } else {
            actions.add("render_distance_reduce_2");
            actions.add("clouds_off");
        }

        return actions;
    }

    /**
     * Get light optimization actions.
     */
    private List<String> getLightActions(Scenario scenario) {
        List<String> actions = new ArrayList<>();

        // Light actions: minimal visual impact
        actions.add("clouds_off");
        
        if (scenario == Scenario.COMBAT) {
            actions.add("particles_minimal");
        }

        return actions;
    }

    /**
     * Get best single action considering tradeoffs.
     */
    public String getBestAction(Scenario scenario) {
        List<String> candidates = getRecommendedActions(scenario);
        if (candidates.isEmpty()) {
            return null;
        }

        // Use TradeoffMatrix to find optimal action
        double fpsWeight = scenario == Scenario.COMBAT ? 1.0 : 0.7;
        double visualWeight = scenario == Scenario.BUILDING ? 0.8 : 0.5;
        double gameplayWeight = 0.3;

        return TradeoffMatrix.getBestAction(
                candidates.toArray(new String[0]),
                fpsWeight,
                visualWeight,
                gameplayWeight
        );
    }
}
