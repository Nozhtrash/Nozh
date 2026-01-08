package dev.nozh.core.learning;

import dev.nozh.NozhConstants;
import dev.nozh.core.governor.DecisionReasoning;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Iterator;

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
 * AUDIT FIX #20: Implemented timeout cleanup for stale pending actions to prevent memory leak.
 */
public final class ActionEffectivenessTracker {

    private static final long PENDING_ACTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final int MAX_PENDING_ACTIONS = 100;
    
    private final Map<String, ActionStats> actionStats = new ConcurrentHashMap<>();
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    
    // AUDIT FIX #20: Cleanup thread for stale pending actions
    private final ScheduledExecutorService cleanupExecutor = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ActionTracker-Cleanup");
            t.setDaemon(true);
            return t;
        });

    public ActionEffectivenessTracker() {
        // AUDIT FIX #20: Start periodic cleanup of stale pending actions
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupStalePendingActions,
            30, 30, TimeUnit.SECONDS
        );
    }

    /**
     * Record action execution start.
     * AUDIT FIX #20: Added limit check to prevent unbounded growth.
     */
    public void recordActionStart(String actionId, double expectedFpsDelta, DecisionReasoning reasoning) {
        // Check limit to prevent memory issues
        if (pendingActions.size() >= MAX_PENDING_ACTIONS) {
            NozhConstants.LOGGER.warn(
                "Too many pending actions ({}), cleaning up before adding: {}",
                pendingActions.size(), actionId
            );
            cleanupStalePendingActions();
            
            // If still at limit after cleanup, reject the new action
            if (pendingActions.size() >= MAX_PENDING_ACTIONS) {
                NozhConstants.LOGGER.error(
                    "Cannot track action {}: pending actions limit reached ({})",
                    actionId, MAX_PENDING_ACTIONS
                );
                return;
            }
        }
        
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
     * AUDIT FIX #20: Clean up stale pending actions that have timed out.
     * This prevents memory leaks when actions start but never complete.
     */
    private void cleanupStalePendingActions() {
        long now = System.currentTimeMillis();
        int removedCount = 0;
        
        Iterator<Map.Entry<String, PendingAction>> iterator = pendingActions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PendingAction> entry = iterator.next();
            PendingAction pending = entry.getValue();
            
            if (now - pending.startTime > PENDING_ACTION_TIMEOUT_MS) {
                iterator.remove();
                removedCount++;
                
                // Record as failed action
                ActionStats stats = actionStats.computeIfAbsent(
                    pending.actionId, 
                    k -> new ActionStats(pending.actionId)
                );
                stats.recordExecution(
                    pending.expectedFpsDelta,
                    0.0, // No FPS improvement
                    false, // Failed due to timeout
                    PENDING_ACTION_TIMEOUT_MS
                );
                
                NozhConstants.LOGGER.warn(
                    "Cleaned up stale pending action: {} (age: {}ms)",
                    pending.actionId,
                    now - pending.startTime
                );
            }
        }
        
        if (removedCount > 0) {
            NozhConstants.LOGGER.info(
                "Cleanup removed {} stale pending actions. Remaining: {}",
                removedCount,
                pendingActions.size()
            );
        }
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
     * Get count of currently pending actions.
     */
    public int getPendingActionCount() {
        return pendingActions.size();
    }

    /**
     * Clear all tracking data.
     */
    public void clear() {
        actionStats.clear();
        pendingActions.clear();
    }

    /**
     * Shutdown the cleanup executor.
     * AUDIT FIX #20: Proper resource cleanup.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
