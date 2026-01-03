package dev.nozh.core.governor;

import dev.nozh.core.NozhLogger;
import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.Command;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.matrix.ActionCandidate;
import dev.nozh.core.matrix.ActionMatrix;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.matrix.ConfidenceCalculator;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;

import java.util.Optional;

/**
 * Governor runner - integration loop (Phase G).
 * 
 * RULES:
 * - Does NOT decide (SimulationGovernor decides)
 * - Does NOT filter (ActionMatrix filters)
 * - Does NOT interpret (just executes loop)
 * - Only calls: snapshot → decide → dispatch
 * 
 * This is MC integration layer (allowed to reference MC for polling).
 */
public final class GovernorRunner {

    private final SimulationGovernor governor;
    private final ActionBus actionBus;
    private final NozhLogger logger;
    private final StateStore stateStore;

    // Intelligent components
    private final dev.nozh.core.governor.PredictiveAnalyzer predictiveAnalyzer;
    private final dev.nozh.core.monitoring.SystemMonitor systemMonitor;
    private final dev.nozh.core.monitoring.ChunkLoadMonitor chunkLoadMonitor;
    private final dev.nozh.core.context.ScenarioDetector scenarioDetector;

    private int tickCounter = 0;
    private static final int POLL_INTERVAL_TICKS = 100; // ~5 seconds at 20tps

    public GovernorRunner(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ActionBus actionBus,
            StateStore stateStore,
            NozhLogger logger,
            dev.nozh.core.intelligence.SessionLearning sessionLearning,
            dev.nozh.core.context.ScenarioDetector scenarioDetector) {
        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTracker,
                new ConfidenceCalculator(),
                sessionLearning);

        this.governor = new SimulationGovernor(matrix);
        this.actionBus = actionBus;
        this.stateStore = stateStore;
        this.logger = logger;
        this.scenarioDetector = scenarioDetector;

        // Initialize intelligent components
        this.predictiveAnalyzer = new dev.nozh.core.governor.PredictiveAnalyzer();
        this.systemMonitor = new dev.nozh.core.monitoring.SystemMonitor();
        this.chunkLoadMonitor = new dev.nozh.core.monitoring.ChunkLoadMonitor();
    }

    /**
     * Called every client tick.
     */
    public void onTick() {
        tickCounter++;
        if (tickCounter < POLL_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        runGovernorLoop();
    }

    private void runGovernorLoop() {
        // 1. Detect scenario and update state reference
        dev.nozh.core.context.Scenario currentScenario = scenarioDetector.detect();
        try {
            stateStore.update(s -> s.withScenario(currentScenario));
        } catch (Exception e) {
            // Ignore update failure
        }

        // Get state snapshot (now contains latest scenario)
        RuntimeState state = stateStore.snapshotSafe();

        // Feed current frametime to predictive analyzer
        if (state.avgFrametimeMs() > 0) {
            predictiveAnalyzer.addSample(state.avgFrametimeMs());
        }

        // INTELLIGENT SKIP: Chunk loading in progress
        if (chunkLoadMonitor.isHeavyChunkLoad()) {
            logger.debug(String.format("Skipping governor decision - heavy chunk load detected (%d chunks/s)",
                    chunkLoadMonitor.getChunkLoadRate()));
            return;
        }

        // INTELLIGENT SKIP: Memory critical - let GC work
        if (systemMonitor.isMemoryCritical()) {
            logger.warn(String.format("Skipping governor decision - memory critical (%s)",
                    systemMonitor.getMemoryUsageString()));
            return;
        }

        // 2. Determine mode from state and scenario (Smart Mode Selection)
        GovernorMode mode = determineMode(state);

        // 3. Detect performance bound from telemetry
        String currentBound = detectBound(state);

        long now = System.currentTimeMillis();

        // 4. Check cooldown (NO CASCADE) with adaptive window
        long lastActionTimestamp = state.governorLastActionTimestamp();
        if (!governor.canAct(state, lastActionTimestamp, now)) {
            long windowMs = governor.getObservationWindow(state);
            logger.debug(String.format("Governor in cooldown, skipping decision (window: %dms)", windowMs));
            return;
        }

        // PREDICTIVE ANALYSIS: Act proactively if drop predicted
        if (predictiveAnalyzer.predictFPSDrop()) {
            double confidence = predictiveAnalyzer.getConfidence();
            logger.info(String.format("Predictive analysis: FPS drop predicted (confidence: %.2f, trend: %s)",
                    confidence, predictiveAnalyzer.getTrendDescription()));
            // Continue to decision making (will act proactively)
        }

        // Log memory pressure if present (affects ActionMatrix priority)
        if (systemMonitor.isMemoryPressure()) {
            logger.debug(String.format("Memory pressure detected (%s)", systemMonitor.getMemoryUsageString()));
        }

        // Decide
        Optional<ActionCandidate> decisionOpt = governor.decide(state, mode, currentBound, now);

        if (decisionOpt.isEmpty()) {
            logger.debug("Governor: no valid action");
            return;
        }

        ActionCandidate decision = decisionOpt.get();

        // Log decision
        logger.info("Governor decision: " + decision.reason());

        // Dispatch via ActionBus
        Command cmd;
        if (decision.targetValue() != null) {
            cmd = new Command.ApplyCapability(
                    decision.capabilityId(),
                    decision.targetValue());
        } else {
            cmd = new Command.ResetCapability(decision.capabilityId());
        }

        actionBus.dispatch(cmd, report -> {
            // Log result only, no logic
            if (report.succeeded()) {
                logger.info("Governor action succeeded");
                // Reset predictor after successful action
                predictiveAnalyzer.reset();
            } else {
                logger.warn("Governor action failed: " + report.error().orElse("unknown"));
            }
        });

        // Update state after action dispatch
        try {
            stateStore.update(currentState -> currentState.withGovernorAction(now));
        } catch (Exception e) {
            logger.warn("Failed to update state after governor action: " + e.getMessage());
        }
    }

    /**
     * Detect performance bound from telemetry.
     * 
     * Simple heuristics for v0.2-beta:
     * - If avg frametime > 16.67ms (60 FPS) → CPU bound
     * - If p95 stable but avg high → GPU bound
     * - Otherwise → BALANCED
     */
    private String detectBound(RuntimeState state) {
        double avgMs = state.avgFrametimeMs();
        double p95Ms = state.p95FrametimeMs();

        // No telemetry yet
        if (avgMs < 0 || p95Ms < 0) {
            return "BALANCED";
        }

        // High average frametime → CPU bound
        if (avgMs > 16.67) {
            return "CPU";
        }

        // P95 much higher than avg → GPU spikes
        if (p95Ms > avgMs * 1.5) {
            return "GPU";
        }

        return "BALANCED";
    }

    /**
     * Determine effective mode based on state and scenario.
     */
    private dev.nozh.core.governor.GovernorMode determineMode(RuntimeState state) {
        if (!state.enabled() || state.governorDisabled()) {
            return dev.nozh.core.governor.GovernorMode.OFF;
        }

        if (state.safeMode()) {
            return dev.nozh.core.governor.GovernorMode.MANUAL_ASSIST;
        }

        if (!state.autoTuning()) {
            return dev.nozh.core.governor.GovernorMode.MANUAL_ASSIST;
        }

        // Scenario-based overrides
        if (state.currentScenario() == dev.nozh.core.context.Scenario.COMBAT) {
            // Combat requires max FPS -> Aggressive
            return dev.nozh.core.governor.GovernorMode.AUTO_AGGRESSIVE;
        }

        if (state.currentScenario() == dev.nozh.core.context.Scenario.MINING) {
            // Mining usually stable
            return dev.nozh.core.governor.GovernorMode.AUTO_CONSERVATIVE;
        }

        // Default auto mode
        return dev.nozh.core.governor.GovernorMode.AUTO_CONSERVATIVE;
    }
}
