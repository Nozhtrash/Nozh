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
        String bound = detectBound(state);

        // 3. Detect performance bound from telemetry
        String currentBound = detectBound(state);
        try {
            stateStore.update(currentState -> currentState.withGovernorSnapshot(mode, currentBound));
        } catch (Exception e) {
            // Ignore update failure
        }

        long now = System.currentTimeMillis();

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
        try {
            stateStore.update(currentState -> currentState.withDecision(decision.reason(), now, steward));
        } catch (Exception e) {
            // Ignore update failure
        }
        String actionSummary = formatActionSummary(decision);

        // Log decision
        logger.info("Governor decision: " + decision.reason());

        // Dispatch via ActionBus
        if (decision.targetValue() != null) {
            Optional<dev.nozh.core.bus.CapabilityValue> previousValue = providerRegistry.get(decision.capabilityId())
                    .flatMap(provider -> provider.getCurrentValueSafe());
            PendingAction pending = new PendingAction(
                    now,
                    decision.capabilityId(),
                    previousValue,
                    decision.targetValue(),
                    state.avgFrametimeMs(),
                    state.p95FrametimeMs());
            Command cmd = new Command.ApplyCapability(
                    decision.capabilityId(),
                    decision.targetValue());

            actionBus.dispatch(cmd, report -> {
                if (report.succeeded()) {
                    logger.info("Governor action succeeded");
                    predictiveAnalyzer.reset();
                } else {
                    logger.warn("Governor action failed: " +
                            report.error().orElse("unknown"));
                }

                ActionHistoryEntry entry = new ActionHistoryEntry(
                        System.currentTimeMillis(),
                        actionSummary,
                        report.finalState());
                try {
                    stateStore.update(currentState -> currentState.withRecentAction(entry, 5));
                } catch (Exception e) {
                    logger.warn("Failed to update action history: " + e.getMessage());
                }
            });

            // Update state after action dispatch
            try {
                stateStore.update(currentState -> currentState.withGovernorAction(now, pending));
            } catch (Exception e) {
                logger.warn("Failed to update state after governor action: " + e.getMessage());
            }
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

        boolean worsened = avg < 0 || p95 < 0
                || avg > pending.baselineAvgMs() + config.improvementEpsilonAvgMs
                || p95 > pending.baselineP95Ms() + config.improvementEpsilonP95Ms;

        if (worsened) {
            Command rollback = pending.previousValue()
                    .<Command>map(v -> new Command.ApplyCapability(pending.capability(), v))
                    .orElseGet(() -> new Command.ResetCapability(pending.capability()));

            actionBus.dispatch(rollback, r -> {
                if (r.succeeded()) {
                    logger.info("Rollback succeeded");
                } else {
                    logger.warn("Rollback failed");
                }
            });

            sessionLearning.recordFailure(pending.capability());
        } else {
            double gain = Math.max(0, pending.baselineAvgMs() - avg);
            sessionLearning.recordSuccess(pending.capability(), gain);
        }

        // Scenario-based overrides (with confidence gating)
        double scenarioConfidence = state.scenarioConfidence();
        if (scenarioConfidence >= 0.55) {
            dev.nozh.core.context.Scenario scenario = state.currentScenario();
            if (scenario == dev.nozh.core.context.Scenario.COMBAT) {
                // Combat requires max FPS -> Aggressive
                return dev.nozh.core.governor.GovernorMode.AUTO_AGGRESSIVE;
            }

            if (scenario == dev.nozh.core.context.Scenario.MINING) {
                // Mining usually stable
                return dev.nozh.core.governor.GovernorMode.AUTO_CONSERVATIVE;
            }

            if (scenario == dev.nozh.core.context.Scenario.BUILDING) {
                return dev.nozh.core.governor.GovernorMode.AUTO_CONSERVATIVE;
            }

            if (scenario == dev.nozh.core.context.Scenario.AFK) {
                return dev.nozh.core.governor.GovernorMode.AUTO_CONSERVATIVE;
            }
        }
        String value = formatCapabilityValue(decision.targetValue());
        if (value.isEmpty()) {
            return decision.capabilityId().name();
        }
        return decision.capabilityId().name() + "=" + value;
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
