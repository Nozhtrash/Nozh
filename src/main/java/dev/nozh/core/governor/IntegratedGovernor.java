package dev.nozh.core.governor;

import dev.nozh.core.NozhConstants;
import dev.nozh.core.capability.ProviderExecutor;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.context.ScenarioDetector;
import dev.nozh.core.context.ScenarioSnapshot;
import dev.nozh.core.intelligence.FramePredictor;
import dev.nozh.core.intelligence.LearningEngine;
import dev.nozh.core.intelligence.UtilityScorer;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Integrated Governor - The Core Decision Engine.
 * <p>
 * Orchestrates all intelligence components to make optimization decisions:
 * 1. Scenario Detection - What is happening?
 * 2. Telemetry Analysis - How is performance?
 * 3. Frame Prediction - What will happen next?
 * 4. Q-Learning Selection - What action to take?
 * 5. Utility Scoring - Is this action appropriate?
 * 6. Provider Execution - Execute the optimization
 * <p>
 * Updated: Now connects to REAL provider execution via ProviderExecutor.
 */
public final class IntegratedGovernor {

    private static final Logger LOGGER = NozhConstants.LOGGER;
    private static final double MIN_UTILITY_THRESHOLD = 0.3;

    private final ScenarioDetector scenarioDetector;
    private final LearningEngine learningEngine;
    private final FramePredictor framePredictor;
    private final UtilityScorer utilityScorer;
    private final ProviderRegistry providerRegistry;
    private final ProviderExecutor providerExecutor;
    private final Executor asyncExecutor;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final Set<String> actionBlacklist;

    public IntegratedGovernor(
            ScenarioDetector scenarioDetector,
            LearningEngine learningEngine,
            FramePredictor framePredictor,
            ProviderRegistry providerRegistry,
            Executor asyncExecutor,
            Set<String> actionBlacklist
    ) {
        this.scenarioDetector = scenarioDetector;
        this.learningEngine = learningEngine;
        this.framePredictor = framePredictor;
        this.providerRegistry = providerRegistry;
        this.providerExecutor = new ProviderExecutor(providerRegistry, asyncExecutor);
        this.asyncExecutor = asyncExecutor;
        this.actionBlacklist = actionBlacklist;
        this.utilityScorer = new UtilityScorer();

        LOGGER.info("IntegratedGovernor initialized with ProviderExecutor");
    }

    /**
     * Core decision-making method.
     * <p>
     * Analyzes current state and decides if optimization is needed.
     * If yes, selects best action via Q-learning and executes it.
     *
     * @param telemetry Current performance telemetry
     * @param targetFps Target FPS to maintain
     */
    public void makeDecision(TelemetrySnapshot telemetry, double targetFps) {
        if (!isRunning.get()) {
            return;
        }

        try {
            // STEP 1: Detect current scenario
            ScenarioSnapshot scenario = scenarioDetector.detectScenario();
            if (scenario == null) {
                LOGGER.warn("Scenario detection returned null, skipping decision");
                return;
            }

            // STEP 2: Evaluate current FPS performance
            double currentFps = telemetry.getCurrentFps();
            boolean isBelowTarget = currentFps < (targetFps * 0.9); // 10% tolerance

            // STEP 3: Check if frame drops are predicted
            boolean frameDropPredicted = framePredictor.predictFrameDrop(telemetry);

            // STEP 4: Determine if optimization is needed
            boolean needsOptimization = isBelowTarget || frameDropPredicted;
            if (!needsOptimization) {
                LOGGER.debug("No optimization needed. FPS: {}/{}, Predicted: {}",
                        currentFps, targetFps, !frameDropPredicted);
                return;
            }

            // STEP 5: Get available actions (filter blacklist)
            List<String> allActions = learningEngine.getAllActions();
            List<String> availableActions = allActions.stream()
                    .filter(action -> !actionBlacklist.contains(action))
                    .collect(Collectors.toList());

            if (availableActions.isEmpty()) {
                LOGGER.warn("No available actions after blacklist filter");
                return;
            }

            // STEP 6: Use Q-learning to select best action
            String selectedAction = learningEngine.selectAction(
                    scenario.scenario(),
                    currentFps < (targetFps * 0.7) ? "low_fps" : "medium_fps",
                    "medium_hw",
                    availableActions
            );

            if (selectedAction == null) {
                LOGGER.warn("Learning engine returned null action");
                return;
            }

            // STEP 7: Calculate utility score for selected action
            double utilityScore = utilityScorer.calculateUtility(selectedAction, scenario, telemetry);

            // STEP 8: Check if utility meets minimum threshold
            if (utilityScore < MIN_UTILITY_THRESHOLD) {
                LOGGER.debug("Action '{}' utility too low: {:.2f} < {:.2f}",
                        selectedAction, utilityScore, MIN_UTILITY_THRESHOLD);
                return;
            }

            // STEP 9: Generate decision reasoning
            double qValue = learningEngine.getQValue(
                    scenario.scenario(),
                    currentFps < (targetFps * 0.7) ? "low_fps" : "medium_fps",
                    "medium_hw",
                    selectedAction
            );

            DecisionReasoning reasoning = new DecisionReasoning(
                    selectedAction,
                    scenario,
                    currentFps,
                    targetFps,
                    utilityScore,
                    qValue,
                    frameDropPredicted,
                    telemetry.getSpikeCount()
            );

            LOGGER.info("DECISION: Executing '{}' | Scenario={}, FPS={:.1f}/{:.1f}, Utility={:.2f}, Q={:.2f}",
                    selectedAction, scenario.scenario(), currentFps, targetFps, utilityScore, qValue);

            // STEP 10: Execute action via ProviderExecutor (REAL EXECUTION)
            executeActionWithTracking(selectedAction, telemetry, reasoning);

        } catch (Exception e) {
            LOGGER.error("Error in makeDecision", e);
        }
    }

    /**
     * Execute action using ProviderExecutor with full tracking.
     * <p>
     * This now performs REAL provider execution, not simulation.
     *
     * @param actionId  Action to execute
     * @param telemetry Current telemetry snapshot
     * @param reasoning Decision reasoning
     */
    private void executeActionWithTracking(
            String actionId,
            TelemetrySnapshot telemetry,
            DecisionReasoning reasoning
    ) {
        long decisionTime = System.currentTimeMillis();
        double preActionFps = telemetry.getCurrentFps();

        // Execute via ProviderExecutor (REAL)
        providerExecutor.executeAction(actionId)
                .thenAccept(result -> {
                    LOGGER.info("Action execution result: {} | Success={}, Time={}ms, Message={}",
                            actionId, result.isSuccess(), result.getExecutionTimeMs(), result.getMessage());

                    // Record experience for learning (will be completed with post-action FPS later)
                    // Note: For full learning, you need to measure FPS after action takes effect
                    double reward = result.isSuccess() ? 1.0 : -0.5;

                    learningEngine.recordExperience(
                            reasoning.scenario().scenario(),
                            preActionFps < 45 ? "low_fps" : "medium_fps",
                            "medium_hw",
                            actionId,
                            reward,
                            preActionFps,
                            preActionFps // TODO: Measure actual post-action FPS
                    );
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to execute action: {}", actionId, ex);
                    // Record negative experience
                    learningEngine.recordExperience(
                            reasoning.scenario().scenario(),
                            preActionFps < 45 ? "low_fps" : "medium_fps",
                            "medium_hw",
                            actionId,
                            -1.0,
                            preActionFps,
                            preActionFps
                    );
                    return null;
                });
    }

    public void shutdown() {
        isRunning.set(false);
        LOGGER.info("IntegratedGovernor shutdown");
    }
}