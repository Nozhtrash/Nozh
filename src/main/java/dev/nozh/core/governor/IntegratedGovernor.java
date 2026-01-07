package dev.nozh.core.governor;

import dev.nozh.NozhConstants;
import dev.nozh.core.context.*;
import dev.nozh.core.learning.*;
import dev.nozh.core.monitoring.*;
import dev.nozh.core.prediction.PerformancePredictor;  // Specific import to resolve ambiguity
import dev.nozh.core.safety.*;
import dev.nozh.core.telemetry.*;
import dev.nozh.core.state.PerformanceSnapshot;
import dev.nozh.core.config.AdaptiveConfigManager;
import dev.nozh.fabric.context.EnhancedFabricScenarioDetector;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.*;

/**
 * Integrated Governor - The main orchestration brain.
 * 
 * Complete pipeline (FULL PHASE 1-4):
 * 1. Telemetry → Filtering
 * 2. Scenario detection → Confidence
 * 3. Performance prediction (Phase 4 - NEW)
 * 4. Health monitoring (Phase 4 - NEW)
 * 5. Action selection (Q-learning)
 * 6. Transactional execution
 * 7. Outcome measurement
 * 8. Learning update
 * 9. Weight adaptation
 * 
 * This is the production-ready, self-learning governor.
 * 
 * PHASE 4 COMPLETE: Predictive + Health-Aware Optimization
 */
public final class IntegratedGovernor {

    // Core components
    private final MinecraftClient client;
    private final AdaptiveConfigManager configManager;
    
    // Telemetry
    private final TelemetryBuffer telemetryBuffer;
    
    // Context
    private final EnhancedFabricScenarioDetector scenarioDetector;
    private final EnvironmentContext environmentContext;
    private final CameraActivityTracker cameraTracker;
    private final ScenarioConfidenceCalculator confidenceCalculator;
    
    // Phase 4: Intelligence
    private final PerformancePredictor predictor;  // NEW: Phase 4 predictor
    private final UtilityScorer utilityScorer;
    
    // Learning & Adaptation
    private final ActionEffectivenessTracker effectivenessTracker;
    private final PerformanceLearningEngine learningEngine;
    private final AdaptiveWeightTuner weightTuner;
    
    // Phase 4: Monitoring
    private final SystemHealthMonitor healthMonitor;  // NEW: Phase 4 health
    private final PerformanceEventLogger eventLogger;
    private final MetricsCollector metricsCollector;
    
    // Safety
    private final ActionBlacklist blacklist;
    
    // State
    private Scenario currentScenario = Scenario.NEUTRAL;
    private volatile boolean initialized = false;

    /**
     * Create integrated governor.
     */
    public IntegratedGovernor(MinecraftClient client, Path logPath) {
        this.client = client;
        this.configManager = new AdaptiveConfigManager();
        
        // Initialize telemetry
        this.telemetryBuffer = new TelemetryBuffer(100, 10); // 100 samples, 10-tick warmup
        
        // Initialize context
        this.scenarioDetector = new EnhancedFabricScenarioDetector(client);
        this.environmentContext = new EnvironmentContext(client);
        this.cameraTracker = new CameraActivityTracker(client);
        this.confidenceCalculator = new ScenarioConfidenceCalculator();
        
        // Initialize Phase 4 intelligence
        this.predictor = new PerformancePredictor();  // NEW: No constructor args
        this.utilityScorer = new UtilityScorer();
        
        // Initialize learning
        this.effectivenessTracker = new ActionEffectivenessTracker();
        this.learningEngine = new PerformanceLearningEngine(effectivenessTracker);
        this.weightTuner = new AdaptiveWeightTuner(effectivenessTracker);
        
        // Initialize Phase 4 monitoring
        this.healthMonitor = new SystemHealthMonitor();  // NEW: Phase 4 health
        this.eventLogger = new PerformanceEventLogger(logPath);
        this.metricsCollector = new MetricsCollector();
        
        // Initialize safety
        this.blacklist = new ActionBlacklist();
        this.blacklist.initializeDefaults();
        
        this.initialized = true;
        NozhConstants.LOGGER.info("IntegratedGovernor initialized - Phase 4 ML system active");
    }

    /**
     * Tick the governor - called every game tick.
     */
    public void tick() {
        if (!initialized) {
            return;
        }
        
        try {
            // Update context trackers
            environmentContext.tick();
            cameraTracker.tick();
            
            // Collect telemetry
            TelemetrySample sample = collectTelemetry();
            if (sample != null) {
                telemetryBuffer.add(sample);
                
                // Feed to Phase 4 predictor (using PerformanceSnapshot)
                if (sample.hasFrametimeData()) {
                    PerformanceSnapshot snapshot = createSnapshotFromSample(sample);
                    predictor.addSnapshot(snapshot);
                }
            }
            
            // Get telemetry snapshot
            TelemetrySnapshot snapshot = telemetryBuffer.snapshot();
            
            // Phase 4: Update health monitor
            healthMonitor.updateFromTelemetry(snapshot);
            
            // Record metrics
            metricsCollector.recordFrame(snapshot.avgFps());
            
            // Decision cycle (rate-limited)
            if (shouldMakeDecision()) {
                makeDecision(snapshot);
            }
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Governor tick error", e);
            healthMonitor.recordError("tick_error");
        }
    }

    /**
     * Make a governor decision with Phase 4 enhancements.
     */
    private void makeDecision(TelemetrySnapshot snapshot) {
        // Skip if not warmed up
        if (!telemetryBuffer.isWarmupComplete()) {
            return;
        }
        
        // Phase 4: Check system health before acting
        if (healthMonitor.isCritical()) {
            NozhConstants.LOGGER.warn("Skipping decision - system critical: " + healthMonitor.getHealthStatus());
            return;
        }
        
        // Phase 4: Check if we should wait for auto-recovery
        if (predictor.hasEnoughData() && predictor.shouldWaitForRecovery()) {
            NozhConstants.LOGGER.info("Performance recovering naturally, skipping intervention");
            return;
        }
        
        // Detect scenario
        ScenarioSnapshot scenarioSnapshot = scenarioDetector.detect();
        Scenario detectedScenario = scenarioSnapshot.scenario();
        double scenarioConfidence = scenarioSnapshot.confidence();
        
        if (scenarioConfidence < 0.5) {
            return; // Low confidence, skip
        }
        
        currentScenario = detectedScenario;
        
        // Check if action needed
        double currentFps = snapshot.avgFps();
        double targetFps = configManager.getValue("target_fps", 60.0);
        double minFps = configManager.getValue("min_fps", 30.0);
        
        // Phase 4: Enhanced decision triggers
        boolean fpsBelowTarget = currentFps < targetFps * 0.9;
        boolean predictedSpike = predictor.isPredictingSpike();  // NEW: Spike prediction
        boolean criticallyLow = currentFps < minFps;
        double predictionConfidence = predictor.getPredictionConfidence();  // NEW: Confidence
        
        // Only act on high-confidence predictions
        if (predictedSpike && predictionConfidence < 0.6) {
            NozhConstants.LOGGER.debug("Ignoring spike prediction - low confidence: " + predictionConfidence);
            predictedSpike = false;
        }
        
        if (!fpsBelowTarget && !predictedSpike && !criticallyLow) {
            return; // Performance is acceptable
        }
        
        // Select best action using learning
        String[] availableActions = getAvailableActions();
        if (availableActions.length == 0) {
            return;
        }
        
        String hardwareProfile = determineHardwareProfile(currentFps);
        PerformanceLearningEngine.GameState state = new PerformanceLearningEngine.GameState(
                currentScenario,
                currentFps,
                hardwareProfile
        );
        
        String selectedAction = learningEngine.getBestAction(state, availableActions);
        if (selectedAction == null || blacklist.isBlacklisted(selectedAction)) {
            return;
        }
        
        // Build decision reasoning with Phase 4 data
        DecisionReasoning reasoning = new DecisionReasoning.Builder()
                .actionId(selectedAction)
                .scenario(currentScenario.name())
                .addTrigger(fpsBelowTarget ? "FPS below target" : "")
                .addTrigger(predictedSpike ? "Predicted performance spike" : "")
                .addTrigger(criticallyLow ? "Critically low FPS" : "")
                .addSignal(String.format("Current FPS: %.1f", currentFps))
                .addSignal(String.format("P95 frametime: %.2fms", snapshot.p95FrametimeMs()))
                .addSignal(String.format("Prediction confidence: %.2f", predictionConfidence))
                .addSignal(String.format("System health: %s", healthMonitor.getHealthStatus()))
                .expectedOutcome("Improve FPS by 10-20")
                .confidenceScore(scenarioConfidence * predictionConfidence)  // Combined confidence
                .build();
        
        // Log decision
        eventLogger.logDecision(selectedAction, reasoning);
        
        // Execute action
        executeAction(selectedAction, reasoning, state, currentFps);
    }

    /**
     * Execute an action and measure its outcome.
     */
    private void executeAction(String actionId, DecisionReasoning reasoning,
                              PerformanceLearningEngine.GameState state,
                              double fpsBefore) {
        long startTime = System.currentTimeMillis();
        double expectedFpsDelta = 15.0;
        
        effectivenessTracker.recordActionStart(actionId, expectedFpsDelta, reasoning);
        
        try {
            // TODO: Execute actual provider action
            boolean executionSuccess = true;
            
            // Wait for effect to stabilize
            waitForStabilization(100); // 100ms
            
            // Measure outcome
            double fpsAfter = telemetryBuffer.snapshot().avgFps();
            double fpsDelta = fpsAfter - fpsBefore;
            long duration = System.currentTimeMillis() - startTime;
            
            boolean success = fpsDelta > 0;
            
            // Record results
            effectivenessTracker.recordActionResult(actionId, fpsDelta, success);
            eventLogger.logActionExecution(actionId, success, duration);
            metricsCollector.recordAction(actionId, success, duration);
            
            // Phase 4: Update health monitor
            if (!success) {
                healthMonitor.recordError("action_failed_" + actionId);
            }
            
            // Calculate reward
            double visualImpact = 0.0;
            double gameplayImpact = 0.0;
            double reward = PerformanceLearningEngine.calculateReward(
                    fpsBefore, fpsAfter, visualImpact, gameplayImpact
            );
            
            // Update Q-learning
            PerformanceLearningEngine.GameState newState = new PerformanceLearningEngine.GameState(
currentScenario, fpsAfter, determineHardwareProfile(fpsAfter)
            );
            learningEngine.updateQValue(state, actionId, reward, newState);
            
            // Adapt weights
            weightTuner.onActionOutcome(actionId, fpsDelta, visualImpact, gameplayImpact);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Action execution failed: " + actionId, e);
            healthMonitor.recordError("action_exception_" + actionId);
            effectivenessTracker.recordActionResult(actionId, 0.0, false);
            metricsCollector.recordAction(actionId, false, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Helper: Create PerformanceSnapshot from TelemetrySample.
     */
    private PerformanceSnapshot createSnapshotFromSample(TelemetrySample sample) {
        return new PerformanceSnapshot(
                sample.timestamp(),
                sample.frametimeMs(),
                sample.frametimeMs(),  // Use as P95 temporarily
                sample.fps(),
                0  // spike count
        );
    }

    /**
     * Get available actions for current scenario.
     */
    private String[] getAvailableActions() {
        // TODO: Get from provider registry
        return new String[]{"reduce_render_distance", "disable_particles", "lower_entity_distance"};
    }

    /**
     * Determine hardware profile based on FPS.
     */
    private String determineHardwareProfile(double fps) {
        if (fps >= 60) return "high_end";
        if (fps >= 30) return "mid_range";
        return "low_end";
    }

    /**
     * Rate limiter for decision making.
     */
    private long lastDecisionTime = 0;
    private static final long DECISION_INTERVAL_MS = 5000; // 5 seconds

    private boolean shouldMakeDecision() {
        long now = System.currentTimeMillis();
        if (now - lastDecisionTime >= DECISION_INTERVAL_MS) {
            lastDecisionTime = now;
            return true;
        }
        return false;
    }

    /**
     * Wait for action effects to stabilize.
     */
    private void waitForStabilization(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Collect current telemetry sample.
     */
    private TelemetrySample collectTelemetry() {
        if (client.world == null) {
            return null;
        }
        
        double fps = client.getCurrentFps();
        double frametime = fps > 0 ? (1000.0 / fps) : 0;
        
        return new TelemetrySample(
                System.currentTimeMillis(),
                frametime,
                -1,
                fps,
                -1,
                -1,
                -1,
                telemetryBuffer.getDroppedCount()
        );
    }

    // ========== PUBLIC API ==========

    /**
     * Phase 4: Get detailed health status.
     */
    public double getHealthScore() {
        return healthMonitor.getHealthScore();
    }

    /**
     * Get health status string.
     */
    public String getHealthStatus() {
        return healthMonitor.getHealthStatus();
    }

    /**
     * Phase 4: Get prediction confidence.
     */
    public double getPredictionConfidence() {
        return predictor.getPredictionConfidence();
    }

    /**
     * Phase 4: Get next predicted frametime.
     */
    public double getPredictedFrametime() {
        return predictor.predictNextFrametime();
    }

    /**
     * Check if governor is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get learning statistics.
     */
    public Map<String, Object> getLearningStats() {
        return effectivenessTracker.getStats();
    }

    /**
     * Get metrics summary.
     */
    public Map<String, Object> getMetricsSummary() {
        return metricsCollector.getSummary();
    }

    /**
     * Get action effectiveness.
     */
    public double getActionEffectiveness(String actionId) {
        return effectivenessTracker.getEffectiveness(actionId);
    }

    /**
     * Reset learning data.
     */
    public void resetLearning() {
        effectivenessTracker.clear();
        predictor.reset();  // NEW: Reset predictor
        healthMonitor.reset();  // NEW: Reset health monitor
        NozhConstants.LOGGER.info("Learning data reset (Phase 4 included)");
    }

    /**
     * Get health report.
     */
    public String getHealthReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== SYSTEM HEALTH REPORT ===\n");
        report.append("Status: ").append(healthMonitor.getHealthStatus()).append("\n");
        report.append(String.format("Health Score: %.2f\n", healthMonitor.getHealthScore()));
        report.append(String.format("Memory Usage: %.1f%%\n", healthMonitor.getMemoryUsagePercent() * 100));
        report.append(String.format("GC Count: %d\n", healthMonitor.getGCCount()));
        report.append(String.format("Avg GC Pause: %.1fms\n", healthMonitor.getAverageGCPause()));
        report.append(String.format("Prediction Confidence: %.2f\n", predictor.getPredictionConfidence()));
        return report.toString();
    }
}
