package dev.nozh.core.governor;

import dev.nozh.core.context.Scenario;

/**
 * Multi-objective utility scorer for action selection.
 * 
 * Formula:
 * U = (ΔFPSᵢ × w₁) - (VisualImpactᵢ × w₂) - (GameplayImpactᵢ × w₃)
 * 
 * Where:
 * - ΔFPS = Expected FPS gain (0-100)
 * - VisualImpact = Quality loss (0.0-1.0)
 * - GameplayImpact = Gameplay degradation (0.0-1.0)
 * - w₁, w₂, w₃ = Scenario-adaptive weights
 * 
 * Weights adapt by scenario:
 * - Combat: Prioritize FPS (w₁=1.0, w₂=0.3, w₃=0.1)
 * - Building: Balance visual (w₁=0.6, w₂=0.8, w₃=0.2)
 * - Exploring: Prioritize FPS + visual (w₁=0.8, w₂=0.6, w₃=0.1)
 * 
 * TASK 7: Multi-objective scoring
 */
public final class UtilityScorer {

    /**
     * Calculate utility score for an action.
     */
    public static double calculateUtility(
            double fpsDelta,
            double visualImpact,
            double gameplayImpact,
            Scenario scenario) {

        ScenarioWeights weights = getWeights(scenario);

        double utility = (fpsDelta * weights.fpsWeight)
                       - (visualImpact * weights.visualWeight)
                       - (gameplayImpact * weights.gameplayWeight);

        return utility;
    }

    /**
     * Get scenario-adaptive weights.
     */
    private static ScenarioWeights getWeights(Scenario scenario) {
        return switch (scenario) {
            case COMBAT -> new ScenarioWeights(1.0, 0.3, 0.1); // Max FPS priority
            case BUILDING -> new ScenarioWeights(0.6, 0.8, 0.2); // Visual priority
            case EXPLORING -> new ScenarioWeights(0.8, 0.6, 0.1); // Balanced FPS+Visual
            case AFK -> new ScenarioWeights(0.3, 0.1, 0.0); // Minimal optimization
            case MENU, LOADING -> new ScenarioWeights(0.5, 0.0, 0.0); // No visual/gameplay impact
            default -> new ScenarioWeights(0.7, 0.5, 0.15); // Balanced default
        };
    }

    /**
     * Rank multiple actions by utility.
     */
    public static ActionScore[] rankActions(
            ActionCandidate[] candidates,
            Scenario scenario) {

        ActionScore[] scores = new ActionScore[candidates.length];

        for (int i = 0; i < candidates.length; i++) {
            ActionCandidate candidate = candidates[i];
            double utility = calculateUtility(
                    candidate.expectedFpsDelta,
                    candidate.visualImpact,
                    candidate.gameplayImpact,
                    scenario
            );
            scores[i] = new ActionScore(candidate.actionId, utility);
        }

        // Sort by utility (descending)
        java.util.Arrays.sort(scores, (a, b) -> Double.compare(b.utility, a.utility));

        return scores;
    }

    /**
     * Scenario weight configuration.
     */
    private static class ScenarioWeights {
        final double fpsWeight;
        final double visualWeight;
        final double gameplayWeight;

        ScenarioWeights(double fpsWeight, double visualWeight, double gameplayWeight) {
            this.fpsWeight = fpsWeight;
            this.visualWeight = visualWeight;
            this.gameplayWeight = gameplayWeight;
        }
    }

    /**
     * Action candidate for scoring.
     */
    public static class ActionCandidate {
        public final String actionId;
        public final double expectedFpsDelta;
        public final double visualImpact;
        public final double gameplayImpact;

        public ActionCandidate(String actionId, double fpsDelta, double visual, double gameplay) {
            this.actionId = actionId;
            this.expectedFpsDelta = fpsDelta;
            this.visualImpact = Math.max(0.0, Math.min(1.0, visual));
            this.gameplayImpact = Math.max(0.0, Math.min(1.0, gameplay));
        }
    }

    /**
     * Action score result.
     */
    public static class ActionScore {
        public final String actionId;
        public final double utility;

        ActionScore(String actionId, double utility) {
            this.actionId = actionId;
            this.utility = utility;
        }
    }
}
