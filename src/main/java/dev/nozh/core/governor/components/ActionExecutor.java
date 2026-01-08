package dev.nozh.core.governor.components;

import dev.nozh.NozhConstants;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.intelligence.DecisionReasoning;
import dev.nozh.core.learning.*;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import java.util.concurrent.*;

/**
 * Manages asynchronous action execution and learning.
 * 
 * Extracted from IntegratedGovernor as part of God Class refactoring.
 * This class handles all action execution and result measurement.
 * 
 * <p><b>Thread Safety:</b> This class is thread-safe. Uses concurrent collections
 * and scheduled executor for async operations.
 * 
 * <p><b>Null Safety:</b> All methods validate inputs.
 * 
 * AUDIT FIX #24: Async execution without blocking game thread.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public final class ActionExecutor {
    
    private final ScheduledExecutorService asyncExecutor;
    private final ConcurrentHashMap<String, CompletableFuture<ActionResult>> pendingActions;
    
    // Dependencies for learning
    private final ActionEffectivenessTracker effectivenessTracker;
    private final PerformanceLearningEngine learningEngine;
    private final AdaptiveWeightTuner weightTuner;
    
    // For telemetry measurement
    private final TelemetryManager telemetryManager;
    private final MonitoringFacade monitoring;
    
    /**
     * Constructs a new ActionExecutor.
     * 
     * @param effectivenessTracker effectiveness tracker (must not be null)
     * @param learningEngine learning engine (must not be null)
     * @param weightTuner weight tuner (must not be null)
     * @param telemetryManager telemetry manager (must not be null)
     * @param monitoring monitoring facade (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    public ActionExecutor(
            ActionEffectivenessTracker effectivenessTracker,
            PerformanceLearningEngine learningEngine,
            AdaptiveWeightTuner weightTuner,
            TelemetryManager telemetryManager,
            MonitoringFacade monitoring) {
        
        if (effectivenessTracker == null || learningEngine == null || 
            weightTuner == null || telemetryManager == null || monitoring == null) {
            throw new NullPointerException("All dependencies must be non-null");
        }
        
        this.effectivenessTracker = effectivenessTracker;
        this.learningEngine = learningEngine;
        this.weightTuner = weightTuner;
        this.telemetryManager = telemetryManager;
        this.monitoring = monitoring;
        
        this.asyncExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ActionExecutor-Async");
            t.setDaemon(true);
            return t;
        });
        
        this.pendingActions = new ConcurrentHashMap<>();
        
        NozhConstants.LOGGER.info("ActionExecutor initialized");
    }
    
    /**
     * Executes an action asynchronously.
     * 
     * @param actionId action ID (must not be null)
     * @param reasoning decision reasoning (must not be null)
     * @param currentScenario current scenario (must not be null)
     * @param state game state (must not be null)
     * @param fpsBefore FPS before action (must be positive)
     */
    public void executeAsync(
            String actionId,
            DecisionReasoning reasoning,
            Scenario currentScenario,
            PerformanceLearningEngine.GameState state,
            double fpsBefore) {
        
        // Validate inputs
        if (actionId == null || reasoning == null || currentScenario == null || state == null) {
            NozhConstants.LOGGER.error("Cannot execute action with null parameters");
            return;
        }
        
        if (fpsBefore <= 0 || !Double.isFinite(fpsBefore)) {
            NozhConstants.LOGGER.error("Invalid FPS before: {}", fpsBefore);
            return;
        }
        
        // Check if action is already pending
        if (pendingActions.containsKey(actionId)) {
            NozhConstants.LOGGER.warn("Action already pending: {}", actionId);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        double expectedFpsDelta = 15.0; // Expected improvement
        
        // Record action start
        try {
            effectivenessTracker.recordActionStart(actionId, expectedFpsDelta, reasoning);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to record action start", e);
        }
        
        // Execute action asynchronously
        CompletableFuture<ActionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Execute actual provider action
                // For now, simulate success
                boolean executionSuccess = true;
                return new ActionResult(executionSuccess, startTime);
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Action execution failed: {}", actionId, e);
                return new ActionResult(false, startTime);
            }
        }, asyncExecutor);
        
        // Schedule measurement after stabilization period (1s)
        asyncExecutor.schedule(() -> {
            try {
                ActionResult result = future.get();
                measureAndLearn(
                        actionId, reasoning, currentScenario, state,
                        fpsBefore, expectedFpsDelta,
                        result.executionSuccess, result.startTime
                );
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to measure action results: {}", actionId, e);
            } finally {
                pendingActions.remove(actionId);
            }
        }, 1000, TimeUnit.MILLISECONDS);
        
        pendingActions.put(actionId, future);
    }
    
    /**
     * Gets the number of pending actions.
     * 
     * @return pending count
     */
    public int getPendingCount() {
        return pendingActions.size();
    }
    
    /**
     * Shuts down the executor.
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        pendingActions.clear();
        NozhConstants.LOGGER.info("ActionExecutor shutdown complete");
    }
    
    // Private helper methods
    
    private void measureAndLearn(
            String actionId,
            DecisionReasoning reasoning,
            Scenario currentScenario,
            PerformanceLearningEngine.GameState state,
            double fpsBefore,
            double expectedFpsDelta,
            boolean executionSuccess,
            long startTime) {
        
        try {
            // Measure results
            TelemetrySnapshot afterSnapshot = telemetryManager.getSnapshot();
            if (afterSnapshot == null) {
                NozhConstants.LOGGER.warn("No telemetry after action execution");
                return;
            }
            
            double fpsAfter = 1000.0 / afterSnapshot.avgFrametimeMs();
            double actualFpsDelta = fpsAfter - fpsBefore;
            
            long duration = System.currentTimeMillis() - startTime;
            boolean success = executionSuccess && actualFpsDelta > 0;
            
            // Record results
            try {
                effectivenessTracker.recordActionResult(actionId, actualFpsDelta, success);
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to record action result", e);
            }
            
            monitoring.logActionExecution(actionId, success, duration);
            
            if (!success) {
                monitoring.recordError("action_failed: " + actionId);
            }
            
            // Calculate reward for learning
            double visualImpact = 0.0; // TODO: Measure from provider
            double gameplayImpact = 0.0; // TODO: Measure from provider
            double reward = PerformanceLearningEngine.calculateReward(
                    fpsBefore, fpsAfter, visualImpact, gameplayImpact
            );
            
            // Update learning
            try {
                PerformanceLearningEngine.GameState newState = new PerformanceLearningEngine.GameState(
                        currentScenario,
                        fpsAfter,
                        determineHardwareProfile(fpsAfter)
                );
                learningEngine.updateFromExperience(state, actionId, reward, newState);
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to update learning", e);
            }
            
            // Adapt weights
            try {
                weightTuner.adaptWeights(
                        currentScenario, actionId,
                        actualFpsDelta, visualImpact, gameplayImpact
                );
            } catch (Exception e) {
                NozhConstants.LOGGER.error("Failed to adapt weights", e);
            }
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to measure and learn from action: {}", actionId, e);
        }
    }
    
    private String determineHardwareProfile(double fps) {
        if (fps <= 0 || !Double.isFinite(fps)) {
            return "medium";
        }
        if (fps >= 120) return "high";
        if (fps >= 60) return "medium";
        return "low";
    }
    
    /**
     * Simple result holder for async action execution.
     */
    private static class ActionResult {
        final boolean executionSuccess;
        final long startTime;
        
        ActionResult(boolean executionSuccess, long startTime) {
            this.executionSuccess = executionSuccess;
            this.startTime = startTime;
        }
    }
}
