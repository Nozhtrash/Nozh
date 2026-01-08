package dev.nozh.core.learning;

import dev.nozh.NozhConstants;
import dev.nozh.core.governor.DecisionReasoning;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

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
 * MEMORY-SAFETY: Fixed in audit - automatic cleanup of stale pending actions.
 * 
 * TASK 9: Performance learning - outcome tracking
 */
public final class ActionEffectivenessTracker {

    private static final long PENDING_ACTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final int MAX_PENDING_ACTIONS = 100; // Prevent unbounded growth
    
    private final Map<String, ActionStats> actionStats = new ConcurrentHashMap<>();
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    
    // Cleanup thread to prevent memory leaks
    private final ScheduledExecutorService cleanupExecutor;
    private volatile boolean shutdown = false;

    public ActionEffectivenessTracker() {
        // Initialize cleanup daemon thread
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ActionTracker-Cleanup");
            t.setDaemon(true); // Won't prevent JVM shutdown
            return t;
        });
        
        // Schedule periodic cleanup every 30 seconds
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupStalePendingActions,
            30, 30, TimeUnit.SECONDS
        );
    }

    /**
     * Record action execution start.
     * 
     * @throws IllegalStateException if too many pending actions (> MAX_PENDING_ACTIONS)
     */
    public void recordActionStart(String actionId, double expectedFpsDelta, DecisionReasoning reasoning) {
        // Check capacity limit to prevent unbounded growth
        if (pendingActions.size() >= MAX_PENDING_ACTIONS) {
            // Try cleanup first
            cleanupStalePendingActions();
            
            // If still over limit, warn and reject
            if (pendingActions.size() >= MAX_PENDING_ACTIONS) {
                NozhConstants.LOGGER.warn(
                    "Too many pending actions ({}), action {} not tracked. " +
                    "This may indicate actions not being completed properly.",
                    pendingActions.size(), actionId
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
            // Action not tracked or already completed/timed out
            return;
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
     * Get count of pending actions.
     * Useful for monitoring and detecting issues.
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
     * Shutdown cleanup thread and release resources.
     * Should be called when tracker is no longer needed.
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        
        try {
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        NozhConstants.LOGGER.info("ActionEffectivenessTracker shutdown complete");
    }
    
    /**
     * Clean up stale pending actions that have timed out.
     * Prevents memory leak from actions that never complete.
     * 
     * Called periodically by cleanup thread and on-demand when hitting limits.
     */
    private void cleanupStalePendingActions() {
        if (shutdown) {
            return;
        }
        
        long now = System.currentTimeMillis();
        int removedCount = 0;
        
        // Find and remove timed-out actions
        for (Map.Entry<String, PendingAction> entry : pendingActions.entrySet()) {
            PendingAction pending = entry.getValue();
            long age = now - pending.startTime;
            
            if (age > PENDING_ACTION_TIMEOUT_MS) {
                if (pendingActions.remove(entry.getKey()) != null) {
                    removedCount++;
                    
                    // Record as failed execution
                    ActionStats stats = actionStats.computeIfAbsent(
                        pending.actionId, 
                        k -> new ActionStats(pending.actionId)
                    );
                    stats.recordExecution(
                        pending.expectedFpsDelta,
                        0.0, // No actual delta since it timed out
                        false, // Failed
                        age
                    );
                }
            }
        }
        
        if (removedCount > 0) {
            NozhConstants.LOGGER.warn(
                "Cleaned up {} stale pending actions (timeout: {}ms). " +
                "This may indicate actions hanging or not completing properly.",
                removedCount, PENDING_ACTION_TIMEOUT_MS
            );
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
