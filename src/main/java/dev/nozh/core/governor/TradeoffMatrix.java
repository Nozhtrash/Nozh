package dev.nozh.core.governor;

/**
 * Tradeoff matrix for FPS vs Visual vs Gameplay impact.
 * 
 * Each optimization action has a profile:
 * - FPS gain (expected frametime reduction)
 * - Visual loss (quality degradation 0.0-1.0)
 * - Gameplay loss (mechanic impact 0.0-1.0)
 * 
 * Matrix helps governor choose optimal actions based on scenario.
 * 
 * TASK 7: Multi-objective scoring - tradeoff analysis
 */
public final class TradeoffMatrix {

    /**
     * Get tradeoff profile for an action.
     */
    public static ActionProfile getProfile(String actionId) {
        return switch (actionId) {
            // Render distance adjustments
            case "render_distance_reduce_4" -> new ActionProfile(8.0, 0.3, 0.1);
            case "render_distance_reduce_2" -> new ActionProfile(4.0, 0.15, 0.05);
            
            // Entity culling
            case "entity_distance_reduce" -> new ActionProfile(5.0, 0.2, 0.05);
            case "entity_culling_aggressive" -> new ActionProfile(7.0, 0.25, 0.15);
            
            // Visual quality
            case "clouds_off" -> new ActionProfile(2.0, 0.15, 0.0);
            case "smooth_lighting_off" -> new ActionProfile(3.0, 0.4, 0.0);
            case "particles_minimal" -> new ActionProfile(4.0, 0.2, 0.1);
            
            // Shadows/lighting
            case "shadows_off" -> new ActionProfile(6.0, 0.5, 0.0);
            
            // Chunk updates
            case "chunk_updates_reduce" -> new ActionProfile(3.0, 0.1, 0.2);
            
            // FOV adjustment (emergency)
            case "fov_reduce" -> new ActionProfile(2.0, 0.15, 0.3);
            
            default -> new ActionProfile(0.0, 0.0, 0.0);
        };
    }

    /**
     * Calculate net utility considering tradeoffs.
     */
    public static double calculateNetUtility(
            String actionId,
            double fpsWeight,
            double visualWeight,
            double gameplayWeight) {
        
        ActionProfile profile = getProfile(actionId);
        
        return (profile.fpsGain * fpsWeight)
             - (profile.visualLoss * visualWeight)
             - (profile.gameplayLoss * gameplayWeight);
    }

    /**
     * Get best action for given weights.
     */
    public static String getBestAction(
            String[] candidateActions,
            double fpsWeight,
            double visualWeight,
            double gameplayWeight) {
        
        String bestAction = null;
        double bestUtility = Double.NEGATIVE_INFINITY;
        
        for (String action : candidateActions) {
            double utility = calculateNetUtility(action, fpsWeight, visualWeight, gameplayWeight);
            if (utility > bestUtility) {
                bestUtility = utility;
                bestAction = action;
            }
        }
        
        return bestAction;
    }

    /**
     * Action tradeoff profile.
     */
    public record ActionProfile(
            double fpsGain,      // Expected FPS improvement (ms)
            double visualLoss,   // Visual quality loss (0.0-1.0)
            double gameplayLoss  // Gameplay impact (0.0-1.0)
    ) {}
}
