package dev.nozh.core.governor;

import dev.nozh.core.NozhLogger;
import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.Command;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.context.ScenarioDetector;
import dev.nozh.core.intelligence.SessionLearning;
import dev.nozh.core.matrix.ActionCandidate;
import dev.nozh.core.matrix.ActionMatrix;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.matrix.ConfidenceCalculator;
import dev.nozh.core.compatibility.ModConflictDetector;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.ActionHistoryEntry;
import dev.nozh.core.bus.CapabilityValue;

import dev.nozh.core.monitoring.ChunkLoadMonitor;
import dev.nozh.core.monitoring.SystemMonitor;

import java.util.Optional;

/**
 * Governor runner - integration loop (Phase G).
 */
public final class GovernorRunner {

    private final SimulationGovernor governor;
    private final ActionBus actionBus;
    private final NozhLogger logger;
    private final StateStore stateStore;
    private final ProviderRegistry providerRegistry;
    private final SessionLearning sessionLearning;

    // Intelligent components
    private final dev.nozh.core.governor.PredictiveAnalyzer predictiveAnalyzer;
    private final dev.nozh.core.monitoring.SystemMonitor systemMonitor;
    private final dev.nozh.core.monitoring.ChunkLoadMonitor chunkLoadMonitor;
    private final dev.nozh.core.context.ScenarioDetector scenarioDetector;
    private final ModConflictDetector conflictDetector;

    private int tickCounter = 0;
    private static final int POLL_INTERVAL_TICKS = 100;

    public GovernorRunner(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ActionBus actionBus,
            StateStore stateStore,
            NozhLogger logger,
            SessionLearning sessionLearning,
            ScenarioDetector scenarioDetector) {
        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTracker,
                new ConfidenceCalculator(),
                sessionLearning);

        this.governor = new SimulationGovernor(matrix);
        this.actionBus = actionBus;
        this.stateStore = stateStore;
        this.logger = logger;
        this.providerRegistry = registry;
        this.sessionLearning = sessionLearning;
        this.scenarioDetector = scenarioDetector;
        this.conflictDetector = new ModConflictDetector();

        this.predictiveAnalyzer = new PredictiveAnalyzer();
        this.systemMonitor = new SystemMonitor();
        this.chunkLoadMonitor = new ChunkLoadMonitor();
    }

    public void onTick() {
        tickCounter++;
        if (tickCounter < POLL_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        runGovernorLoop();
    }

    private void runGovernorLoop() {
        long now = System.currentTimeMillis();
        NozhConfig config = ConfigManager.getConfig();

        // 1. Detect scenario and update state reference
        dev.nozh.core.context.ScenarioSnapshot scenarioSnapshot = scenarioDetector.detect();
        try {
            stateStore.update(s -> s.withScenario(
                    scenarioSnapshot.scenario(),
                    scenarioSnapshot.confidence()));
        } catch (Exception e) {
            // Ignore update failure
        }

        RuntimeState state = stateStore.snapshotSafe();

        // === Pending evaluation ===
        if (state.pendingAction().isPresent()) {
            PendingAction pending = state.pendingAction().get();
            long elapsed = now - pending.timestampMillis();

            if (elapsed < config.rollbackWindowMillis) {
                logger.debug(String.format(
                        "Pending evaluation in progress (%dms/%dms)",
                        elapsed, config.rollbackWindowMillis));
                return;
            }

            evaluatePendingAction(state, pending, config);
            return;
        }

        // Feed predictor
        if (state.avgFrametimeMs() > 0) {
            predictiveAnalyzer.addSample(state.avgFrametimeMs());
        }

        if (chunkLoadMonitor.isHeavyChunkLoad()) {
            logger.debug("Skipping governor decision - heavy chunk load");
            return;
        }

        if (systemMonitor.isMemoryCritical()) {
            logger.warn("Skipping governor decision - memory critical");
            return;
        }

        GovernorMode mode = determineMode(state);
        if (state.safeMode()) {
            logger.debug("Safe mode active: skipping governor decision");
            return;
        }
        if (mode == GovernorMode.MANUAL_ASSIST && state.suggestedAction().isPresent()) {
            logger.debug("Manual assist active: suggestion pending, awaiting user confirmation");
            return;
        }
        String bound = detectBound(state);

        // 3. Detect performance bound from telemetry
        String currentBound = detectBound(state);
        // REMOVED: withGovernorSnapshot() tracking no longer available

        // 4. Check cooldown (NO CASCADE) with adaptive window
        long lastActionTimestamp = state.governorLastActionTimestamp();
        if (!governor.canAct(state, lastActionTimestamp, now)) {
            long windowMs = governor.getObservationWindow(state);
            logger.debug(String.format("Governor in cooldown, skipping decision (window: %dms)", windowMs));
            return;
        }

        Optional<ActionCandidate> decisionOpt = governor.decide(state, mode, bound, now);

        if (decisionOpt.isEmpty()) {
            return;
        }

        ActionCandidate decision = decisionOpt.get();
        String steward = conflictDetector.getSteward(decision.capabilityId());
        // REMOVED: withDecision() tracking no longer available
        String actionSummary = formatActionSummary(decision);

        // Log decision
        logger.info("Governor decision: " + decision.reason());

        try {
            stateStore.update(currentState -> currentState.withDecision(decision.reason(), now));
        } catch (Exception e) {
            logger.warn("Failed to update state decision: " + e.getMessage());
        }

        // Manual Assist: stage suggestion only
        if (mode == GovernorMode.MANUAL_ASSIST && decision.targetValue() != null) {
            Optional<dev.nozh.core.bus.CapabilityValue> previousValue = providerRegistry.get(decision.capabilityId())
                    .flatMap(provider -> provider.getCurrentValueSafe());
            Command cmd = new Command.ApplyCapability(
                    decision.capabilityId(),
                    decision.targetValue());
            PendingAction pending = new PendingAction(
                    now,
                    decision.capabilityId(),
                    cmd,
                    previousValue,
                    decision.targetValue(),
                    state.avgFrametimeMs(),
                    state.p95FrametimeMs());
            try {
                stateStore.update(currentState -> currentState.withSuggestedAction(pending));
            } catch (Exception e) {
                logger.warn("Failed to store suggested action: " + e.getMessage());
            }
            logger.info("Governor suggestion queued for manual assist: " + actionSummary);
            return;
        }

        // Dispatch via ActionBus
        if (decision.targetValue() != null) {
            Optional<dev.nozh.core.bus.CapabilityValue> previousValue = providerRegistry.get(decision.capabilityId())
                    .flatMap(provider -> provider.getCurrentValueSafe());
            Command cmd = new Command.ApplyCapability(
                    decision.capabilityId(),
                    decision.targetValue());
            PendingAction pending = new PendingAction(
                    now,
                    decision.capabilityId(),
                    cmd,
                    previousValue,
                    decision.targetValue(),
                    state.avgFrametimeMs(),
                    state.p95FrametimeMs());

            actionBus.dispatch(cmd, report -> {
                if (report.succeeded()) {
                    logger.info("Governor action succeeded");
                    predictiveAnalyzer.reset();
                } else {
                    logger.warn("Governor action failed: " +
                            report.error().orElse("unknown"));

                    // CRITICAL: Clear pending action so we don't evaluate a failed action later
                    try {
                        stateStore.update(RuntimeState::withPendingActionCleared);
                    } catch (Exception e) {
                        logger.error("Failed to clear pending action after execution failure");
                    }
                }

                // REMOVED: withRecentAction() tracking no longer available
            });

            // Update state after action dispatch
            try {
                stateStore.update(currentState -> currentState.withGovernorAction(now, pending));
            } catch (Exception e) {
                logger.warn("Failed to update state after governor action: " + e.getMessage());
            }
        } else {
            logger.info("Governor PASSIVE: yield decision, no action dispatched");
        }
    }

    private String detectBound(RuntimeState state) {
        double avgMs = state.avgFrametimeMs();
        double p95Ms = state.p95FrametimeMs();
        double tickAvgMs = state.tickTimeAvg();
        double tickP95Ms = state.tickTimeP95();

        boolean frameDataAvailable = avgMs >= 0 && p95Ms >= 0;
        boolean tickDataAvailable = tickAvgMs >= 0 && tickP95Ms >= 0;

        if (!frameDataAvailable && !tickDataAvailable) {
            return "BALANCED";
        }

        if (!tickDataAvailable) {
            // Fallback heuristic without tick data
            if (avgMs > 16.67) {
                return "CPU";
            }

            if (p95Ms > avgMs * 1.5) {
                return "GPU";
            }

            return "BALANCED";
        }

        double tickThresholdMs = 50.0;
        boolean tickHigh = tickAvgMs > tickThresholdMs || tickP95Ms > tickThresholdMs;

        if (tickHigh) {
            return "CPU";
        }

        double frameThresholdMs = 16.67;
        boolean frameHigh = frameDataAvailable && (avgMs > frameThresholdMs || p95Ms > frameThresholdMs);
        if (frameHigh) {
            return "GPU";
        }
        return "BALANCED";
    }

    private GovernorMode determineMode(RuntimeState state) {
        if (!state.enabled() || state.governorDisabled()) {
            return GovernorMode.OFF;
        }
        if (state.safeMode() || !state.autoTuning()) {
            return GovernorMode.MANUAL_ASSIST;
        }
        if (state.currentScenario() == dev.nozh.core.context.Scenario.COMBAT) {
            return GovernorMode.AUTO_AGGRESSIVE;
        }
        return GovernorMode.AUTO_CONSERVATIVE;
    }

    private void evaluatePendingAction(RuntimeState state, PendingAction pending, NozhConfig config) {
        double avg = state.avgFrametimeMs();
        double p95 = state.p95FrametimeMs();

        // Strict Evaluation:
        // 1. Worsened: Avg increased significantly ( > baseline + epsilon)
        // 2. Ineffective: Avg didn't decrease enough ( > baseline - epsilon)
        // Goal: avg < baseline - epsilon

        boolean avgWorsened = avg > pending.baselineAvgMs() + config.improvementEpsilonAvgMs;
        boolean avgIneffective = avg > pending.baselineAvgMs() - config.improvementEpsilonAvgMs;
        boolean p95Worsened = p95 > pending.baselineP95Ms() + config.improvementEpsilonP95Ms;
        boolean p95Ineffective = p95 > pending.baselineP95Ms() - config.improvementEpsilonP95Ms;

        boolean worsened = avgWorsened || p95Worsened;
        boolean ineffective = avgIneffective || p95Ineffective;

        if (worsened || ineffective) {
            if (config.rollbackEnabled) {
                Command rollback = pending.previousValue()
                        .<Command>map(v -> new Command.ApplyCapability(pending.capability(), v))
                        .orElseGet(() -> new Command.ResetCapability(pending.capability()));

                actionBus.dispatch(rollback, r -> {
                    if (r.succeeded()) {
                        logger.info("Rollback succeeded (Action was " + (worsened ? "harmful" : "ineffective") + ")");
                    } else {
                        logger.warn("Rollback failed");
                    }
                });
            } else {
                logger.info("Rollback disabled in config, keeping ineffective action.");
            }

            sessionLearning.recordFailure(pending.capability());
        } else {
            // Success: avg <= baseline - epsilon
            double gainAvg = Math.max(0, pending.baselineAvgMs() - avg);
            double gainP95 = Math.max(0, pending.baselineP95Ms() - p95);
            sessionLearning.recordSuccess(pending.capability(), Math.max(gainAvg, gainP95));
        }
        // Clear pending action
        try {
            stateStore.update(RuntimeState::withPendingActionCleared);
        } catch (Exception e) {
            logger.warn("Failed to clear pending action");
        }
    }

    private String formatActionSummary(ActionCandidate decision) {
        String value = formatCapabilityValue(decision.targetValue());
        if (value.isEmpty()) {
            return decision.capabilityId().toString();
        }
        return decision.capabilityId().toString() + "=" + value;
    }

    private String formatCapabilityValue(CapabilityValue value) {
        if (value == null) {
            return "";
        }
        if (value instanceof CapabilityValue.IntValue intValue) {
            return Integer.toString(intValue.value());
        }
        if (value instanceof CapabilityValue.EnumValue enumValue) {
            return enumValue.name();
        }
        if (value instanceof CapabilityValue.BoolValue boolValue) {
            return Boolean.toString(boolValue.value());
        }
        if (value instanceof CapabilityValue.FloatValue floatValue) {
            return Float.toString(floatValue.value());
        }
        return value.toString();
    }
}
