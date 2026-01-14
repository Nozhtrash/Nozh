package dev.nozh.core.governor;

import dev.nozh.NozhConstants;
import dev.nozh.core.config.AdaptiveConfigManager;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.intelligence.AnomalyDetector;
import dev.nozh.core.intelligence.NeuralLagPredictor;
import dev.nozh.core.intelligence.ScenarioPredictor;
import dev.nozh.core.learning.ActionEffectivenessTracker;
import dev.nozh.core.learning.AdaptiveWeightTuner;
import dev.nozh.core.learning.PerformanceLearningEngine;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import dev.nozh.core.prediction.PerformancePredictor;
import dev.nozh.core.telemetry.MetricLogger;

import java.util.Arrays;
import java.util.Set;

/**
 * Encapsulates the intelligence logic for making optimization decisions.
 * Handles prediction, scenario analysis, and reinforcement learning lookup.
 */
public class DecisionEngine {

    // Intelligence Components
    private final PerformancePredictor perfPredictor;
    private final ScenarioPredictor scenarioPredictor;
    private final NeuralLagPredictor neuralPredictor;

    // Learning Components
    private final ActionEffectivenessTracker effectivenessTracker;
    private final PerformanceLearningEngine learningEngine;
    @SuppressWarnings("unused") // Future proofing for reinforcement learning
    private final AdaptiveWeightTuner weightTuner; // Kept for completeness, though specific usage might be internal to
                                                   // engine

    // Detection
    private final AnomalyDetector anomalyDetector;

    // State
    private int lastEntityCount = 0;
    private int lastParticleCount = 0;
    private int lastChunkUpdates = 0;
    private double lastPlayerSpeed = 0.0;
    private boolean lastFramePredictionReady = false;

    // Configuration dependencies
    private final AdaptiveConfigManager configManager;
    private final double cpuBias;
    private final double gpuBias;

    // New fields for hysteresis and logging
    private final MetricLogger metricLogger;
    private GovernorState currentState = GovernorState.STABLE;
    private long lastDecisionTime = 0; // Timestamp of the last decision (System.currentTimeMillis())

    public DecisionEngine(AdaptiveConfigManager configManager, AnomalyDetector anomalyDetector,
            double targetFps, double cpuBias, double gpuBias) {
        this.configManager = configManager;
        this.anomalyDetector = anomalyDetector;
        // this.targetFps removed as it was shadowed and unused
        this.cpuBias = cpuBias;
        this.gpuBias = gpuBias;

        this.perfPredictor = new PerformancePredictor((int) targetFps);
        this.scenarioPredictor = new ScenarioPredictor();
        this.neuralPredictor = new NeuralLagPredictor();

        this.effectivenessTracker = new ActionEffectivenessTracker();
        this.learningEngine = new PerformanceLearningEngine(effectivenessTracker);
        this.weightTuner = new AdaptiveWeightTuner(effectivenessTracker);
        this.metricLogger = new MetricLogger(); // Initialize new field

        if (NozhConstants.LOGGER != null) {
            NozhConstants.LOGGER.info("DecisionEngine initialized with bias CPU:{} GPU:{}", cpuBias, gpuBias);
        }
    }

    public void feedPredictors(TelemetrySnapshot snapshot, int tick, int entityCount, int particleCount,
            int chunkUpdates, double speed) {
        if (snapshot != null && snapshot.sampleCount() > 0) {
            perfPredictor.addSample(snapshot.avgFrametimeMs());
        }

        // Train neural predictor periodically (Optional, default OFF for performance)
        // Train neural predictor periodically (Optional, default OFF for performance)
        NozhConfig config = ConfigManager.getConfig();
        if (config != null && config.enableNeuralPredictor) {
            if (lastFramePredictionReady && tick % 5 == 0 && snapshot != null) {
                boolean actuallyLagged = snapshot.avgFrametimeMs() > 30.0;
                try {
                    neuralPredictor.train(actuallyLagged, lastEntityCount, lastParticleCount, lastChunkUpdates,
                            lastPlayerSpeed);
                } catch (Exception e) {
                    // Ignore training errors
                }
            }
        }

        // Update state calls for next tick
        lastEntityCount = entityCount;
        lastParticleCount = particleCount;
        lastChunkUpdates = chunkUpdates;
        lastPlayerSpeed = speed;
        lastFramePredictionReady = true;
    }

    public boolean shouldOptimize(TelemetrySnapshot snapshot, double currentFps, double targetFps) {
        if (snapshot == null)
            return false;

        // 0. Cooldown Check
        long now = System.currentTimeMillis();
        NozhConfig config = ConfigManager.getConfig();
        long cooldown = config != null ? config.decisionCooldownMs : 12000L;

        if (now - lastDecisionTime < cooldown) {
            return false;
        }

        double p99 = snapshot.p99FrametimeMs();
        double variance = snapshot.frametimeVariance();

        // 0.5. Anomaly Check (Network Lag prevention)
        AnomalyDetector.LagType anomaly = anomalyDetector.analyze(p99);
        if (anomaly == AnomalyDetector.LagType.NETWORK_LAG) {
            // If the lag is network-related, do not attempt to optimize rendering
            return false;
        }

        // 1. Get Configured Thresholds
        double mildP99 = config != null ? config.thresholdP99Mild : 22.0;
        double mildVar = config != null ? config.thresholdVarianceMild : 8.0;
        double severeP99 = config != null ? config.thresholdP99Severe : 33.0;
        double severeVar = config != null ? config.thresholdVarianceSevere : 25.0;
        double resetP99 = config != null ? config.thresholdP99Reset : 18.0;
        double resetVar = config != null ? config.thresholdVarianceReset : 6.0;

        // 2. Schmitt Trigger State Machine
        GovernorState nextState = currentState;
        String reason = "CHECK";

        // Layer 1: Frame Pacing Trigger
        int consecutiveSlow = snapshot.consecutiveSlowFrames();
        boolean pacingIssueMild = consecutiveSlow > 8; // ~8 frames of stutter (approx 130ms+ total lag time)
        boolean pacingIssueSevere = consecutiveSlow > 15;

        // Layer 2: Entity Density Trigger
        int denseChunks = snapshot.denseChunkCount();
        boolean densityIssue = denseChunks > 0 && currentFps < targetFps * 0.8;

        switch (currentState) {
            case STABLE -> {
                if (p99 > severeP99 || pacingIssueSevere || densityIssue) {
                    nextState = GovernorState.OPTIMIZING_SEVERE;
                    if (densityIssue)
                        reason = "ENTITY_DENSITY_CRITICAL";
                    else
                        reason = pacingIssueSevere ? "PACING_SEVERE" : "P99_SEVERE_EXCEEDED";
                } else if (variance > severeVar) {
                    nextState = GovernorState.OPTIMIZING_SEVERE;
                    reason = "VARIANCE_SEVERE_EXCEEDED";
                } else if (p99 > mildP99 || pacingIssueMild) {
                    nextState = GovernorState.OPTIMIZING_MILD;
                    reason = pacingIssueMild ? "PACING_MILD" : "P99_MILD_EXCEEDED";
                } else if (variance > mildVar) {
                    nextState = GovernorState.OPTIMIZING_MILD;
                    reason = "VARIANCE_MILD_EXCEEDED";
                }
            }
            case OPTIMIZING_MILD -> {
                if (p99 > severeP99 || pacingIssueSevere || densityIssue) {
                    nextState = GovernorState.OPTIMIZING_SEVERE;
                    reason = densityIssue ? "ENTITY_DENSITY_ESCALATION"
                            : (pacingIssueSevere ? "PACING_ESCALATION" : "ESCALATION_P99");
                } else if (variance > severeVar) {
                    nextState = GovernorState.OPTIMIZING_SEVERE;
                    reason = "ESCALATION_VARIANCE";
                } else if (p99 < resetP99 && variance < resetVar && consecutiveSlow == 0 && denseChunks == 0) {
                    nextState = GovernorState.STABLE;
                    reason = "RESET_CONDITION_MET";
                }
            }
            case OPTIMIZING_SEVERE -> {
                // Hysteresis: Must drop all the way to RESET levels to exit severe directly
                if (p99 < resetP99 && variance < resetVar && consecutiveSlow == 0 && denseChunks == 0) {
                    nextState = GovernorState.STABLE;
                    reason = "RESET_CONDITION_MET";
                }
            }
        }

        boolean stateChanged = (nextState != currentState);
        currentState = nextState;

        // 3. Silent Logging
        if (config != null && (config.enableSilentLogging || stateChanged)) {
            // Log with specific reason if state changed, otherwise just "CHECK" or context
            String logTrigger = stateChanged ? "STATE_CHANGE: " + reason : "MONITOR";
            metricLogger.log(snapshot, currentState, logTrigger, now - lastDecisionTime);
        }

        // 4. Decision
        if (stateChanged && currentState != GovernorState.STABLE) {
            // State change triggered optimization immediately
            lastDecisionTime = now; // Reset cooldown
            return true;
        }

        // Return true if we are in an optimizing state, allowing further actions if
        // needed (though Governor throttle handles freq)
        // Actually, IntegratedGovernor uses this boolean to decide whether to call
        // selectAction.
        // If we are in OPTIMIZING state, we generally want to return true to allow
        // action selection.
        return currentState != GovernorState.STABLE;
    }

    public String selectAction(Scenario scenario, double currentFps, String[] availableActions) {
        if (availableActions == null || availableActions.length == 0)
            return null;

        // Apply director bias
        String[] sortedActions = applyDirectorBias(availableActions);

        String hardwareProfile = determineHardwareProfile(currentFps);
        PerformanceLearningEngine.GameState state = new PerformanceLearningEngine.GameState(
                scenario, currentFps, hardwareProfile);

        String selected = null;
        try {
            selected = selectBestActionWithBias(state, sortedActions);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Selection failed", e);
        }

        if (selected == null && sortedActions.length > 0) {
            selected = sortedActions[0]; // Fallback
        }

        return selected;
    }

    public PerformanceLearningEngine getLearningEngine() {
        return learningEngine;
    }

    public ScenarioPredictor getScenarioPredictor() {
        return scenarioPredictor;
    }

    public ActionEffectivenessTracker getEffectivenessTracker() {
        return effectivenessTracker;
    }

    // --- Private Helpers from original Governor ---

    private String selectBestActionWithBias(PerformanceLearningEngine.GameState state, String[] actions) {
        String best = null;
        double bestScore = -Double.MAX_VALUE;

        // Dynamic situational bias
        boolean entityOverload = lastEntityCount > 200; // Heuristic fallback if snapshot not avail here, but we can do
                                                        // better

        for (String action : actions) {
            double q = 0.0;
            try {
                q = learningEngine.getActionValue(state, action);
            } catch (Exception e) {
                /* default 0 */ }

            double score = q * getActionBiasMultiplier(action);

            // Layer 2: Targeted Bias
            if (action.contains("entity") && entityOverload) {
                score *= 2.0;
            }

            if (best == null || score > bestScore) {
                best = action;
                bestScore = score;
            }
        }
        return best;
    }

    private String[] applyDirectorBias(String[] actions) {
        try {
            return Arrays.stream(actions)
                    .sorted((a, b) -> Double.compare(getActionBiasMultiplier(b), getActionBiasMultiplier(a)))
                    .toArray(String[]::new);
        } catch (Exception e) {
            return actions;
        }
    }

    private static final Set<String> GPU_ACTIONS = Set.of(
            "disable_clouds", "reduce_shadows", "lower_particles", "reduce_render_distance", "graphics_mode");
    private static final Set<String> CPU_ACTIONS = Set.of(
            "lower_entity_distance");

    private double getActionBiasMultiplier(String actionId) {
        if (actionId == null)
            return 1.0;
        double mult = 1.0;
        if (GPU_ACTIONS.contains(actionId))
            mult *= gpuBias;
        if (CPU_ACTIONS.contains(actionId))
            mult *= cpuBias;
        return mult;
    }

    private String determineHardwareProfile(double fps) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        int processors = Runtime.getRuntime().availableProcessors();

        if (maxMemory < 3L * 1024 * 1024 * 1024 || processors <= 2) {
            return "low";
        }
        if (fps <= 0 || !Double.isFinite(fps)) {
            return "medium";
        }

        double highThreshold = configManager != null ? configManager.getValue("hw_profile_high_fps", 120.0) : 120.0;
        double mediumThreshold = configManager != null ? configManager.getValue("hw_profile_medium_fps", 60.0) : 60.0;

        if (fps >= highThreshold)
            return "high";
        if (fps >= mediumThreshold)
            return "medium";
        return "low";
    }
}
