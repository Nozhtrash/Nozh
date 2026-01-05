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
import dev.nozh.core.governor.OptimizationProfile;
import dev.nozh.core.compatibility.CompatibilityMatrix;
import dev.nozh.core.compatibility.ModConflictDetector;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.ActionHistoryEntry;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.capability.ApplyResult;
import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.governor.ActionOutcome;

import dev.nozh.core.monitoring.ChunkLoadMonitor;
import dev.nozh.core.monitoring.SystemMonitor;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.HashMap;
import java.util.Map;

/**
 * Governor runner - integration loop (Phase G).
 */
public final class GovernorRunner {

    private static final int MAX_SUGGESTED_QUEUE = 5;
    private static final long RAPID_SCENARIO_CHANGE_WINDOW_MS = 5_000L;

    private final SimulationGovernor governor;
    private final ActionBus actionBus;
    private final NozhLogger logger;
    private final StateStore stateStore;
    private final ProviderRegistry providerRegistry;
    private final SessionLearning sessionLearning;
    private final ActionSuccessTracker successTracker;
    private final Supplier<PerfSnapshot> perfSnapshotSupplier;
    private final Map<dev.nozh.core.bus.CapabilityId, Long> rollbackCooldowns = new HashMap<>();

    // Intelligent components
    private final dev.nozh.core.governor.PredictiveAnalyzer predictiveAnalyzer;
    private final dev.nozh.core.monitoring.SystemMonitor systemMonitor;
    private final dev.nozh.core.monitoring.ChunkLoadMonitor chunkLoadMonitor;
    private final dev.nozh.core.context.ScenarioDetector scenarioDetector;

    private int tickCounter = 0;
    private long totalTicks = 0;

    public GovernorRunner(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ActionBus actionBus,
            StateStore stateStore,
            NozhLogger logger,
            SessionLearning sessionLearning,
            ScenarioDetector scenarioDetector,
            Supplier<PerfSnapshot> perfSnapshotSupplier) {
        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTracker,
                new ConfidenceCalculator(),
                sessionLearning,
                new CompatibilityMatrix());

        this.governor = new SimulationGovernor(matrix);
        this.actionBus = actionBus;
        this.stateStore = stateStore;
        this.logger = logger;
        this.providerRegistry = registry;
        this.sessionLearning = sessionLearning;
        this.successTracker = successTracker;
        this.scenarioDetector = scenarioDetector;
        this.perfSnapshotSupplier = perfSnapshotSupplier != null ? perfSnapshotSupplier : PerfSnapshot::empty;

        this.predictiveAnalyzer = new PredictiveAnalyzer();
        this.systemMonitor = new SystemMonitor();
        this.chunkLoadMonitor = new ChunkLoadMonitor();
    }

    public void onTick() {
        totalTicks++;
        dev.nozh.core.context.ScenarioSnapshot scenarioSnapshot = scenarioDetector.detect();
        long nowMillis = nowMillis();
        try {
            stateStore.update(state -> {
                boolean changed = scenarioSnapshot.scenario() != state.currentScenario();
                boolean rapidChange = changed
                        && state.lastScenarioChangeTimestamp() > 0
                        && nowMillis - state.lastScenarioChangeTimestamp() <= RAPID_SCENARIO_CHANGE_WINDOW_MS;
                boolean combatAfkFlip = changed
                        && isCombatAfkFlip(state.currentScenario(), scenarioSnapshot.scenario());
                return state.withScenarioUpdate(
                        scenarioSnapshot.scenario(),
                        scenarioSnapshot.confidence(),
                        nowMillis,
                        changed,
                        rapidChange,
                        combatAfkFlip);
            });
        } catch (Exception e) {
            // Ignore update failure
        }
        tickCounter++;
        int pollInterval = Math.max(20, ConfigManager.getConfig().evalPeriodTicks);
        if (tickCounter < pollInterval) {
            return;
        }
        tickCounter = 0;
        runGovernorLoop();
    }

    private void runGovernorLoop() {
        long now = System.currentTimeMillis();
        NozhConfig config = ConfigManager.getConfig();

        refreshCurrentSettings();
        RuntimeState state = stateStore.snapshotSafe();
        syncBaselineIfNeeded(state);

        // === Pending evaluation ===
        if (state.pendingAction().isPresent()) {
            PendingAction pending = state.pendingAction().get();
            long elapsed = now - pending.timestampMillis();
            long evaluationWindow = config.benchmarkModeEnabled ? config.benchmarkMicroIntervalMillis
                    : config.rollbackWindowMillis;

            if (elapsed < evaluationWindow) {
                logger.debug(String.format(
                        "Pending evaluation in progress (%dms/%dms)",
                        elapsed, evaluationWindow));
                return;
            }

            evaluatePendingAction(state, pending, config);
            return;
        }

        int suggestionQueueSize = state.suggestedActions() != null ? state.suggestedActions().size() : 0;
        if (suggestionQueueSize >= MAX_SUGGESTED_QUEUE) {
            logger.debug("Suggestion queue full, awaiting confirmations");
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
        mode = ModePolicy.enforceManualPreference(mode, state.autoTuning() && config.allowAutoTuning);
        ModePolicy policy = ModePolicy.forMode(mode);

        // 3. Detect performance bound from telemetry
        String bound = detectBound(state);
        // REMOVED: withGovernorSnapshot() tracking no longer available

        // 4. Check cooldown (NO CASCADE) with adaptive window
        long lastActionTimestamp = state.governorLastActionTimestamp();
        boolean benchmarkMode = config.benchmarkModeEnabled;
        if (!governor.canAct(state, lastActionTimestamp, now, benchmarkMode, config.benchmarkMicroIntervalMillis)) {
            long windowMs = benchmarkMode ? config.benchmarkMicroIntervalMillis : governor.getObservationWindow(state);
            logger.debug(String.format("Governor in cooldown, skipping decision (window: %dms)", windowMs));
            return;
        }

        Optional<ActionCandidate> decisionOpt = governor.decide(
                state,
                mode,
                bound,
                now,
                OptimizationProfile.fromConfig(config.optimizationProfile),
                config.targetFps,
                config.reverseEpsilonMs,
                state.baselineSettings(),
                state.currentSettings());

        if (decisionOpt.isEmpty()) {
            return;
        }

        ActionCandidate decision = decisionOpt.get();
        successTracker.recordDecision(decision);
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
        if (policy.requiresUserConfirmation() && decision.targetValue() != null) {
            if (state.suggestedActions() != null) {
                for (PendingAction existing : state.suggestedActions()) {
                    if (existing.capability() == decision.capabilityId()
                            && existing.newValue().equals(decision.targetValue())) {
                        logger.debug("Suggestion already queued, skipping duplicate");
                        return;
                    }
                }
            }
            PerfSnapshot baselineSnapshot = perfSnapshotSupplier.get();
            Optional<dev.nozh.core.bus.CapabilityValue> previousValue = providerRegistry.get(decision.capabilityId())
                    .flatMap(provider -> provider.getCurrentValueSafe());
            Command cmd = new Command.ApplyCapability(
                    decision.capabilityId(),
                    decision.targetValue());
            PendingAction pending = new PendingAction(
                    now,
                    totalTicks,
                    decision.capabilityId(),
                    cmd,
                    previousValue,
                    decision.targetValue(),
                    state.avgFrametimeMs(),
                    state.p95FrametimeMs(),
                    state.currentScenario(),
                    state.scenarioConfidence(),
                    baselineSnapshot);
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
            PerfSnapshot baselineSnapshot = perfSnapshotSupplier.get();
            int observationWindowSeconds = resolveObservationWindowSeconds(baselineSnapshot);
            Optional<dev.nozh.core.bus.CapabilityValue> previousValue = providerRegistry.get(decision.capabilityId())
                    .flatMap(provider -> provider.getCurrentValueSafe());
            Command cmd = new Command.ApplyCapability(
                    decision.capabilityId(),
                    decision.targetValue());
            PendingAction pending = new PendingAction(
                    now,
                    totalTicks,
                    decision.capabilityId(),
                    cmd,
                    previousValue,
                    decision.targetValue(),
                    state.avgFrametimeMs(),
                    state.p95FrametimeMs(),
                    state.currentScenario(),
                    state.scenarioConfidence(),
                    baselineSnapshot);

            ActionHistoryEntry actionEntry = new ActionHistoryEntry(
                    now,
                    actionSummary,
                    state.currentScenario(),
                    state.scenarioConfidence(),
                    baselineSnapshot,
                    PerfSnapshot.empty(),
                    0.0,
                    0,
                    observationWindowSeconds,
                    ActionOutcome.NEUTRAL,
                    false);

            actionBus.dispatch(cmd, report -> {
                if (report.succeeded()) {
                    logger.info("Governor action succeeded");
                    predictiveAnalyzer.reset();
                    refreshCurrentSettings();
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
                stateStore.update(currentState -> currentState.withGovernorAction(
                        now,
                        pending,
                        actionEntry,
                        config.historyMaxEntries));
            } catch (Exception e) {
                logger.warn("Failed to update state after governor action: " + e.getMessage());
            }
        } else {
            logger.info("Governor PASSIVE: yield decision, no action dispatched");
        }
    }

    private void syncBaselineIfNeeded(RuntimeState state) {
        if (state.baselineSettings() != null && !state.baselineSettings().isEmpty()) {
            return;
        }
        refreshBaselineSettings();
        refreshCurrentSettings();
    }

    private void refreshBaselineSettings() {
        java.util.Map<dev.nozh.core.bus.CapabilityId, dev.nozh.core.bus.CapabilityValue> baseline = new java.util.EnumMap<>(
                dev.nozh.core.bus.CapabilityId.class);
        for (var provider : providerRegistry.getAllProviders()) {
            provider.getCurrentValueSafe().ifPresent(value -> baseline.put(provider.id(), value));
        }
        try {
            stateStore.update(state -> state.withBaselineSettings(baseline));
        } catch (Exception e) {
            logger.warn("Failed to store baseline settings: " + e.getMessage());
        }
    }

    private void refreshCurrentSettings() {
        java.util.Map<dev.nozh.core.bus.CapabilityId, dev.nozh.core.bus.CapabilityValue> current = new java.util.EnumMap<>(
                dev.nozh.core.bus.CapabilityId.class);
        for (var provider : providerRegistry.getAllProviders()) {
            provider.getCurrentValueSafe().ifPresent(value -> current.put(provider.id(), value));
        }
        try {
            stateStore.update(state -> state.withCurrentSettings(current));
        } catch (Exception e) {
            logger.warn("Failed to store current settings: " + e.getMessage());
        }
    }

    public void captureBaselineSettings() {
        refreshBaselineSettings();
        refreshCurrentSettings();
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

    private boolean isCombatAfkFlip(dev.nozh.core.context.Scenario previous,
            dev.nozh.core.context.Scenario current) {
        return (previous == dev.nozh.core.context.Scenario.COMBAT
                && current == dev.nozh.core.context.Scenario.AFK)
                || (previous == dev.nozh.core.context.Scenario.AFK
                        && current == dev.nozh.core.context.Scenario.COMBAT);
    }

    private void evaluatePendingAction(RuntimeState state, PendingAction pending, NozhConfig config) {
        Optional<CapabilityValue> currentValue = providerRegistry.get(pending.capability())
                .flatMap(provider -> provider.getCurrentValueSafe());
        if (currentValue.isPresent() && !currentValue.get().equals(pending.newValue())) {
            logger.warn("Detected inconsistency for " + pending.capability() + ", applying safe fallback");
            pending.command()
                    .inverse(pending.previousValue())
                    .ifPresentOrElse(rollback -> actionBus.dispatch(rollback, r -> {
                        if (r.succeeded()) {
                            logger.info("Safe fallback rollback succeeded");
                        } else {
                            logger.warn("Safe fallback rollback failed");
                        }
                    }), () -> logger.warn("Safe fallback rollback unavailable"));
            sessionLearning.recordFailure(pending.capability());
            successTracker.recordFailure(pending.capability());
            try {
                stateStore.update(RuntimeState::withPendingActionCleared);
            } catch (Exception e) {
                logger.warn("Failed to clear pending action after fallback");
            }
            successTracker.clearDecisionSnapshot(pending.capability());
            return;
        }
        PerfSnapshot currentSnapshot = perfSnapshotSupplier.get();
        if (currentSnapshot == null) {
            currentSnapshot = PerfSnapshot.empty();
        }
        PerfSnapshot baselineSnapshot = pending.baselineSnapshot();
        if (baselineSnapshot == null) {
            baselineSnapshot = PerfSnapshot.empty();
        }
        boolean sufficientData = currentSnapshot != null
                && baselineSnapshot != null
                && currentSnapshot.sufficientData()
                && baselineSnapshot.sufficientData();

        ActionOutcome outcome = sufficientData
                ? governor.evaluateOutcome(baselineSnapshot, currentSnapshot)
                : evaluateOutcomeFallback(baselineSnapshot, currentSnapshot);
        if (!sufficientData) {
            logger.debug("Outcome evaluation fallback used (insufficient perf snapshot data)");
        }

        int observationWindowSeconds = resolveObservationWindowSeconds(currentSnapshot != null ? currentSnapshot : baselineSnapshot);
        double p95Delta = resolveDelta(currentSnapshot, baselineSnapshot, PerfSnapshot::p95FrametimeMs);
        int spikeDelta = resolveSpikeDelta(currentSnapshot, baselineSnapshot);
        if (spikeDelta > 0) {
            outcome = ActionOutcome.NEGATIVE;
        }

        boolean rollbackApplied = false;
        if (outcome != ActionOutcome.POSITIVE && sufficientData) {
            if (config.rollbackEnabled) {
                Long lastRollback = rollbackCooldowns.get(pending.capability());
                if (lastRollback != null && nowMillis() - lastRollback < config.rollbackCooldownMillis) {
                    logger.info("Rollback skipped due to cooldown for " + pending.capability());
                } else {
                    rollbackApplied = attemptProviderRollback(pending);
                    rollbackCooldowns.put(pending.capability(), nowMillis());
                }
            } else {
                logger.info("Rollback disabled in config, keeping ineffective action.");
            }

            sessionLearning.recordFailure(pending.capability(), pending.scenario());
            successTracker.recordFailure(pending.capability());
        } else if (outcome == ActionOutcome.POSITIVE) {
            double gainAvg = resolveGain(pending.baselineAvgMs(), currentSnapshot != null ? currentSnapshot.avgFrametimeMs() : Double.NaN);
            double gainP95 = resolveGain(pending.baselineP95Ms(), currentSnapshot != null ? currentSnapshot.p95FrametimeMs() : Double.NaN);
            sessionLearning.recordSuccess(pending.capability(), pending.scenario(), Math.max(gainAvg, gainP95));
            successTracker.recordSuccess(pending.capability());
        }

        logger.debug(String.format(
                "Governor action evaluation action=%s impact=%s decision=%s p95Delta=%.2fms spikesDelta=%d window=%ds",
                pending.capability(),
                outcome,
                rollbackApplied ? "rollback" : "keep",
                p95Delta,
                spikeDelta,
                observationWindowSeconds));

        String actionSummary = resolveActionSummary(state, pending);
        ActionHistoryEntry updatedEntry = new ActionHistoryEntry(
                pending.timestampMillis(),
                actionSummary,
                pending.scenario(),
                pending.scenarioConfidence(),
                pending.baselineSnapshot(),
                currentSnapshot,
                p95Delta,
                spikeDelta,
                observationWindowSeconds,
                outcome,
                rollbackApplied);
        try {
            stateStore.update(currentState -> currentState.withActionOutcome(pending.timestampMillis(), updatedEntry));
        } catch (Exception e) {
            logger.warn("Failed to update action history outcome");
        }
        // Clear pending action
        try {
            stateStore.update(RuntimeState::withPendingActionCleared);
        } catch (Exception e) {
            logger.warn("Failed to clear pending action");
        }
        successTracker.clearDecisionSnapshot(pending.capability());
    }

    private double resolvePreP95(PendingAction pending) {
        return successTracker.getDecisionSnapshot(pending.capability())
                .map(snapshot -> snapshot.preSnapshot())
                .filter(snapshot -> snapshot != null)
                .map(snapshot -> snapshot.p95FrametimeMs())
                .filter(this::isValidPerfValue)
                .orElse(pending.baselineP95Ms());
    }

    private ActionOutcome evaluateOutcomeFallback(PerfSnapshot baselineSnapshot, PerfSnapshot currentSnapshot) {
        if (baselineSnapshot == null || currentSnapshot == null) {
            return ActionOutcome.NEUTRAL;
        }
        double avgDelta = resolveDelta(currentSnapshot, baselineSnapshot, PerfSnapshot::avgFrametimeMs);
        double p95Delta = resolveDelta(currentSnapshot, baselineSnapshot, PerfSnapshot::p95FrametimeMs);
        boolean avgValid = isValidPerfValue(currentSnapshot.avgFrametimeMs())
                && isValidPerfValue(baselineSnapshot.avgFrametimeMs());
        boolean p95Valid = isValidPerfValue(currentSnapshot.p95FrametimeMs())
                && isValidPerfValue(baselineSnapshot.p95FrametimeMs());
        if (!avgValid && !p95Valid) {
            return ActionOutcome.NEUTRAL;
        }
        if ((avgValid && avgDelta < 0) || (p95Valid && p95Delta < 0)) {
            return ActionOutcome.POSITIVE;
        }
        return ActionOutcome.NEGATIVE;
    }

    private String resolveActionSummary(RuntimeState state, PendingAction pending) {
        for (ActionHistoryEntry entry : state.actionHistory()) {
            if (entry.timestampMillis() == pending.timestampMillis()) {
                return entry.actionSummary();
            }
        }
        return pending.capability().toString();
    }

    private boolean isValidPerfValue(double value) {
        return Double.isFinite(value) && value > 0;
    }

    private double resolveDelta(
            PerfSnapshot currentSnapshot,
            PerfSnapshot baselineSnapshot,
            ToDoubleFunction<PerfSnapshot> extractor) {
        if (currentSnapshot == null || baselineSnapshot == null) {
            return 0.0;
        }
        double currentValue = extractor.applyAsDouble(currentSnapshot);
        double baselineValue = extractor.applyAsDouble(baselineSnapshot);
        if (!isValidPerfValue(currentValue) || !isValidPerfValue(baselineValue)) {
            return 0.0;
        }
        return currentValue - baselineValue;
    }

    private int resolveSpikeDelta(PerfSnapshot currentSnapshot, PerfSnapshot baselineSnapshot) {
        if (currentSnapshot == null || baselineSnapshot == null) {
            return 0;
        }
        return currentSnapshot.spikeCount() - baselineSnapshot.spikeCount();
    }

    private double resolveGain(double baselineMs, double currentMs) {
        if (!isValidPerfValue(baselineMs) || !isValidPerfValue(currentMs)) {
            return 0.0;
        }
        return Math.max(0.0, baselineMs - currentMs);
    }

    private boolean attemptProviderRollback(PendingAction pending) {
        if (pending.previousValue().isEmpty()) {
            logger.warn("No previous value available for rollback: " + pending.capability());
            return false;
        }
        return providerRegistry.get(pending.capability())
                .map(provider -> {
                    ApplyResult result = provider.apply(pending.previousValue().get());
                    if (result instanceof ApplyResult.Success) {
                        logger.info("Rollback succeeded (Action was ineffective)");
                        return true;
                    }
                    if (result instanceof ApplyResult.Rejected rejected) {
                        logger.warn("Rollback rejected for " + pending.capability() + ": " + rejected.reason());
                    } else if (result instanceof ApplyResult.Failed failed) {
                        logger.warn("Rollback failed for " + pending.capability() + ": " + failed.reason());
                    } else {
                        logger.warn("Rollback returned unknown result for " + pending.capability());
                    }
                    return false;
                })
                .orElseGet(() -> {
                    logger.warn("Rollback provider unavailable for " + pending.capability());
                    return false;
                });
    }

    private int resolveObservationWindowSeconds(PerfSnapshot snapshot) {
        if (snapshot == null || snapshot.windowSeconds() <= 0) {
            return 0;
        }
        return snapshot.windowSeconds();
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

    private long nowMillis() {
        return System.currentTimeMillis();
    }
}
