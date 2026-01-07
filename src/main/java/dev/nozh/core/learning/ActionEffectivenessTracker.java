package dev.nozh.core.learning;

import dev.nozh.core.governor.DecisionReasoning;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks effectiveness of actions to enable learning.
 * 
 * Measures:
 * - Expected FPS delta vs actual FPS delta
 * - Action success rate
 * - Side effects (visual/gameplay impact)
 * - Rollback frequency
 * 
 * Uses this data to improve future decisions.
 * 
 * TASK 9: Performance learning - outcome tracking
 */
public final class ActionEffectivenessTracker {

    private final Map<String, ActionStats> actionStats = new ConcurrentHashMap<>();
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();

    /**
     * Record action execution start.
     */
    public void recordActionStart(String actionId, double expectedFpsDelta, DecisionReasoning reasoning) {
        PendingAction pending = new PendingAction(
                actionId,
                System.currentTimeMillis(),
                expectedFpsDelta,
                reasoning
        );
        pendingActions.put(actionId, pending);
    }

    /**
     * Record action completion with actual results.
     */
    public void recordActionResult(String actionId, double actualFpsDelta, boolean success) {
        PendingAction pending = pendingActions.remove(actionId);
        if (pending == null) {
            return; // Action not tracked or already completed
        }

        long duration = System.currentTimeMillis() - pending.startTime;
        ActionStats stats = actionStats.computeIfAbsent(actionId, k -> new ActionStats(actionId));

        stats.recordExecution(
                pending.expectedFpsDelta,
                actualFpsDelta,
                success,
                duration
        );
    }

    /**
     * Get effectiveness score for an action (0.0-1.0).
     * Higher = more effective historically.
     */
    public double getEffectivenessScore(String actionId) {
        ActionStats stats = actionStats.get(actionId);
        if (stats == null || stats.executionCount < 3) {
            return 0.5; // Neutral score for unknown actions
        }

        return stats.getEffectivenessScore();
    }

    /**
     * Get prediction accuracy for an action (0.0-1.0).
     * How well does expected FPS match actual FPS?
     */
    public double getPredictionAccuracy(String actionId) {
        ActionStats stats = actionStats.get(actionId);
        if (stats == null || stats.executionCount < 3) {
            return 0.5;
        }

        return 1.0 - Math.min(1.0, stats.getAveragePredictionError() / 20.0);
    }

    /**
     * Get action statistics.
     */
    public ActionStats getStats(String actionId) {
        return actionStats.get(actionId);
    }

    /**
     * Get all tracked action IDs.
     */
    public Map<String, ActionStats> getAllStats() {
        return new HashMap<>(actionStats);
    }

    /**
     * Clear all tracking data.
     */
    public void clear() {
        actionStats.clear();
        pendingActions.clear();
    }

    /**
     * Pending action record.
     */
    private static class PendingAction {
        final String actionId;
        final long startTime;
        final double expectedFpsDelta;
        final DecisionReasoning reasoning;

        PendingAction(String actionId, long startTime, double expectedFpsDelta, DecisionReasoning reasoning) {
            this.actionId = actionId;
            this.startTime = startTime;
            this.expectedFpsDelta = expectedFpsDelta;
            this.reasoning = reasoning;
        }
    }

    /**
     * Action statistics.
     */
    public static class ActionStats {
        final String actionId;
        int executionCount = 0;
        int successCount = 0;
        double totalExpectedFps = 0.0;
        double totalActualFps = 0.0;
        double totalPredictionError = 0.0;
        long totalDurationMs = 0;

        ActionStats(String actionId) {
            this.actionId = actionId;
        }

        void recordExecution(double expectedFps, double actualFps, boolean success, long durationMs) {
            executionCount++;
            if (success) {
                successCount++;
            }
            totalExpectedFps += expectedFps;
            totalActualFps += actualFps;
            totalPredictionError += Math.abs(expectedFps - actualFps);
            totalDurationMs += durationMs;
        }

        public double getSuccessRate() {
            return executionCount == 0 ? 0.0 : (double) successCount / executionCount;
        }

        public double getAveragePredictionError() {
            return executionCount == 0 ? 0.0 : totalPredictionError / executionCount;
        }

        public double getEffectivenessScore() {
            if (executionCount == 0) return 0.5;

            double successRate = getSuccessRate();
            double predictionAccuracy = 1.0 - Math.min(1.0, getAveragePredictionError() / 20.0);
            double avgActualFps = totalActualFps / executionCount;
            double fpsBonus = Math.min(1.0, avgActualFps / 15.0);

            return (successRate * 0.4) + (predictionAccuracy * 0.3) + (fpsBonus * 0.3);
        }

        public int getExecutionCount() {
            return executionCount;
        }
    }
}
