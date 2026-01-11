package dev.nozh.core.governor;

import dev.nozh.core.NozhLogger;
import dev.nozh.core.bus.ActionBus;
import dev.nozh.core.bus.Command;
import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.context.ScenarioDetector;
import dev.nozh.core.intelligence.SessionLearning;
import dev.nozh.core.matrix.ActionCandidate;
import dev.nozh.core.matrix.ActionMatrix;
import dev.nozh.core.matrix.ActionSuccessTracker;
import dev.nozh.core.matrix.ConfidenceCalculator;
import dev.nozh.core.matrix.ActionMatrixTuning;
import dev.nozh.core.compat.IrisCompat;
import dev.nozh.core.compatibility.CompatibilityMatrix;
import dev.nozh.core.profiler.PerfManager;
import dev.nozh.core.profiler.SpikeCausalityReport;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.ActionHistoryEntry;
import dev.nozh.core.state.BaselineSnapshot;
import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.governor.ActionOutcome;
import dev.nozh.core.preset.HardwareProfile;
import dev.nozh.core.preset.ModpackProfile;
import dev.nozh.core.preset.ModpackRegistry;
import dev.nozh.core.preset.PresetTuningResolver;
import dev.nozh.core.capability.RollbackGuarantee;

import dev.nozh.core.monitoring.ChunkLoadMonitor;
import dev.nozh.core.monitoring.SystemMonitor;

import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Governor runner - integration loop (Phase G).
 */
public final class GovernorRunner {

    private static final int MAX_SUGGESTED_QUEUE = 5;
    private static final int REVERSE_IMPROVEMENT_STREAK = 3;
    private static final long SPIKE_PREDICTION_MIN_WINDOW_MS = 5_000L;
    private static final int DECISION_LATENCY_TARGET_MS = 2;

    private final HybridGovernor governor;
    private final ActionMatrix actionMatrix;
    private final ActionBus actionBus;
    private final NozhLogger logger;
    private final StateStore stateStore;
    private final ProviderRegistry providerRegistry;
    private final SessionLearning sessionLearning;
    private final ActionSuccessTracker successTracker;
    private final PerfManager perfManager;
    private final Supplier<PerfSnapshot> perfSnapshotSupplier;
    private final Map<dev.nozh.core.capability.CapabilityId, Long> rollbackCooldowns = new HashMap<>();

    // Intelligent components
    private final dev.nozh.core.governor.PredictiveAnalyzer predictiveAnalyzer;
    private final dev.nozh.core.monitoring.SystemMonitor systemMonitor;
    private final dev.nozh.core.monitoring.ChunkLoadMonitor chunkLoadMonitor;
    private final dev.nozh.core.context.ScenarioDetector scenarioDetector;
    private final Optional<ModpackProfile> modpackProfile;
    private final AdaptiveVisualQualityController visualQualityController;
    private ActionMatrixTuning actionMatrixTuning = ActionMatrixTuning.defaults();
    private String lastHardwareProfile = "";
    private String lastModpackId = "";

    private int tickCounter = 0;
    private long totalTicks = 0;
    private double lastReverseP95Ms = -1.0;
    private int lastReverseSpikes = -1;
    private int reverseP95Streak = 0;
    private int reverseSpikeStreak = 0;
    private int lastObservationWindowSeconds = -1;
    private dev.nozh.core.profiler.SpikePrediction pendingSpikePrediction;
    private long lastSpikePredictionMillis = 0L;
    private int lastSpikePredictionCount = -1;
    private boolean pendingFrametimePrediction = false;
    private double pendingFrametimeConfidence = 0.0;
    private long lastFrametimePredictionMillis = 0L;
    private double lastFrametimePredictionAvgMs = Double.NaN;
    private double lastFrametimePredictionP95Ms = Double.NaN;
    private int lastFrametimePredictionSpikes = -1;

    public GovernorRunner(
            ProviderRegistry registry,
            ActionSuccessTracker successTracker,
            ActionBus actionBus,
            StateStore stateStore,
            NozhLogger logger,
            SessionLearning sessionLearning,
            PerfManager perfManager,
            ScenarioDetector scenarioDetector,
            Supplier<PerfSnapshot> perfSnapshotSupplier) {
        ActionMatrix matrix = new ActionMatrix(
                registry,
                successTracker,
                new ConfidenceCalculator(),
                sessionLearning,
                new CompatibilityMatrix());

        this.actionMatrix = matrix;
        this.governor = new HybridGovernor(matrix, logger);
        this.actionBus = actionBus;
        this.stateStore = stateStore;
        this.logger = logger;
        this.providerRegistry = registry;
        this.sessionLearning = sessionLearning;
        this.successTracker = successTracker;
        this.perfManager = perfManager;
        this.scenarioDetector = scenarioDetector;
        this.perfSnapshotSupplier = perfSnapshotSupplier != null ? perfSnapshotSupplier : PerfSnapshot::empty;

        this.predictiveAnalyzer = new PredictiveAnalyzer();
        this.systemMonitor = new SystemMonitor();
        this.chunkLoadMonitor = new ChunkLoadMonitor();
        this.modpackProfile = ModpackRegistry.detect();
        this.visualQualityController = new AdaptiveVisualQualityController();
    }

    public void onTick() {
        totalTicks++;
        dev.nozh.core.context.ScenarioSnapshot scenarioSnapshot = scenarioDetector.detect();
        long nowMillis = nowMillis();
        try {
            stateStore.update(state -> {
                return state.withScenarioUpdate(
                        scenarioSnapshot.scenario(),
                        scenarioSnapshot.confidence(),
                        nowMillis);
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

        refreshActionMatrixTuning(config);
        syncObservationWindow(config);
        refreshCurrentSettings();
        RuntimeState state = stateStore.snapshotSafe();
        applyPredictionLearningFeedback();
        evaluatePredictionAccuracy(state, config, now);
        syncBaselineIfNeeded(state);
        boolean reverseReady = updateReverseImprovement(state, config);

        // === Pending evaluation ===
        if (state.pendingAction().isPresent()) {
            PendingAction pending = state.pendingAction().get();
            long elapsed = now - pending.timestampMillis();
            long evaluationWindow = resolveEvaluationWindowMillis(config);

            if (elapsed < evaluationWindow) {
                logger.debug(String.format(
                        "Pending evaluation in progress (%dms/%dms)",
                        elapsed, evaluationWindow));
                return;
            }

            evaluatePendingAction(state, pending, config);
            return;
        }

        if (actionBus.hasPendingCommands()) {
            logger.debug("Skipping governor decision - action bus pending");
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

        updateSpikePrediction(state, config, now);
        updateFrametimePrediction(state, config, now);

        if (chunkLoadMonitor.isHeavyChunkLoad()) {
            logger.debug("Skipping governor decision - heavy chunk load");
            return;
        }

        if (systemMonitor.isMemoryCritical()) {
            logger.warn("Skipping governor decision - memory critical");
            return;
        }

        if (attemptPreventiveAction(state, config, now)) {
            return;
        }

        Optional<AdaptiveVisualQualityController.QualityChange> visualChange = visualQualityController.evaluate(
                state,
                config,
                providerRegistry,
                now);
        if (visualChange.isPresent()) {
            if (dispatchAdaptiveVisualChange(state, config, now, visualChange.get())) {
                return;
            }
        }

        GovernorMode mode = determineMode(state);
        mode = ModePolicy.enforceManualPreference(mode, state.autoTuning() && config.allowAutoTuning);
        ModePolicy policy = ModePolicy.forMode(mode);

        // 3. Detect performance bound from telemetry
        SpikeCausalityReport spikeCausality = resolveSpikeCausality();
        String bound = applyCausalityBound(detectBound(state), spikeCausality);
        // REMOVED: withGovernorSnapshot() tracking no longer available

        // 4. Check cooldown (NO CASCADE) with adaptive window
        long lastActionTimestamp = state.governorLastActionTimestamp();
        boolean benchmarkMode = config.benchmarkModeEnabled;
        if (!governor.canAct(state, lastActionTimestamp, now, benchmarkMode, config.benchmarkMicroIntervalMillis)) {
            long windowMs = benchmarkMode ? config.benchmarkMicroIntervalMillis : governor.getObservationWindow(state);
            logger.debug(String.format("Governor in cooldown, skipping decision (window: %dms)", windowMs));
            return;
        }

        int decisionBudgetMs = Math.min(config.governorDecisionBudgetMs, DECISION_LATENCY_TARGET_MS);
        DecisionBudget decisionBudget = new DecisionBudget(decisionBudgetMs);
        long decisionStartNanos = perfManager != null ? perfManager.startDecisionTimer() : System.nanoTime();
        
        // FIXED: Use config OptimizationProfile and convert to governor OptimizationProfile
        dev.nozh.core.config.OptimizationProfile configProfile = dev.nozh.core.config.OptimizationProfile.fromConfig(config.optimizationProfile);
        OptimizationProfile governorProfile = configProfile.isAggressive() ? OptimizationProfile.AGGRESSIVE : OptimizationProfile.BALANCED;
        
        Optional<ActionCandidate> decisionOpt = governor.decide(
                state,
                mode,
                bound,
                now,
                governorProfile,
                config.targetFps,
                config.reverseEpsilonMs,
                reverseReady,
                state.baselineSnapshot(),
                state.currentSettings(),
                config,
                actionMatrixTuning,
                spikeCausality);

        if (decisionBudget.isOverBudget()) {
            if (perfManager != null) {
                perfManager.recordDecisionLatency(decisionStartNanos);
            }
            logger.warn(String.format(
                    "Governor decision aborted - internal budget exceeded (%dms)",
                    decisionBudget.elapsedMs()));
            return;
        }

        if (perfManager != null && !perfManager.isDecisionWithinBudget(decisionStartNanos, decisionBudgetMs)) {
            logger.warn(String.format(
                    "Governor decision aborted - latency budget exceeded (%dms)",
                    perfManager.getLastDecisionLatencyMs()));
            return;
        }

        if (decisionOpt.isEmpty()) {
            return;
        }

        ActionCandidate decision = decisionOpt.get();
        if (decision.targetValue() == null) {
            logger.info("Governor idle: stewardship active (" + decision.reason() + ")");
            return;
        }
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
            PerfSnapshot baselineSnapshot = captureSnapshot();
            Optional<dev.nozh.core.capability.CapabilityValue> previousValue = providerRegistry.get(decision.capabilityId())
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
            PerfSnapshot baselineSnapshot = captureSnapshot();
            int observationWindowSeconds = resolveObservationWindowSeconds(config, baselineSnapshot);
            Optional<dev.nozh.core.capability.CapabilityValue> previousValue = providerRegistry.get(decision.capabilityId())
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

    private boolean dispatchAdaptiveVisualChange(RuntimeState state, NozhConfig config, long now,
            AdaptiveVisualQualityController.QualityChange change) {
        if (change == null || change.targetValue() == null) {
            return false;
        }
        PerfSnapshot baselineSnapshot = captureSnapshot();
        int observationWindowSeconds = resolveObservationWindowSeconds(config, baselineSnapshot);
        Optional<dev.nozh.core.capability.CapabilityValue> previousValue = providerRegistry.get(change.capabilityId())
                .flatMap(provider -> provider.getCurrentValueSafe());
        Command cmd = new Command.ApplyCapability(change.capabilityId(), change.targetValue());
        PendingAction pending = new PendingAction(
                now,
                totalTicks,
                change.capabilityId(),
                cmd,
                previousValue,
                change.targetValue(),
                state.avgFrametimeMs(),
                state.p95FrametimeMs(),
                state.currentScenario(),
                state.scenarioConfidence(),
                baselineSnapshot);

        String actionSummary = "adaptive_visual=" + change.capabilityId().name()
                + "=" + formatCapabilityValue(change.targetValue());
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

        try {
            stateStore.update(currentState -> currentState.withDecision(change.reason(), now));
        } catch (Exception e) {
            logger.warn("Failed to update state decision: " + e.getMessage());
        }

        actionBus.dispatch(cmd, report -> {
            if (report.succeeded()) {
                logger.info("Adaptive visual quality action succeeded");
                long appliedAt = report.finishedAtMillis() > 0 ? report.finishedAtMillis() : nowMillis();
                visualQualityController.onChangeApplied(change.nextStep(), appliedAt);
                predictiveAnalyzer.reset();
                refreshCurrentSettings();
            } else {
                logger.warn("Adaptive visual quality action failed: " +
                        report.error().orElse("unknown"));
                try {
                    stateStore.update(RuntimeState::withPendingActionCleared);
                } catch (Exception e) {
                    logger.error("Failed to clear pending action after execution failure");
                }
            }
        });

        try {
            stateStore.update(currentState -> currentState.withGovernorAction(
                    now,
                    pending,
                    actionEntry,
                    config.historyMaxEntries));
        } catch (Exception e) {
            logger.warn("Failed to update state after adaptive visual action: " + e.getMessage());
        }
        return true;
    }

    private boolean attemptPreventiveAction(RuntimeState state, NozhConfig config, long nowMillis) {
        if (state == null || config == null) {
            return false;
        }
        if (!config.rollbackEnabled) {
            return false;
        }

        PredictiveAnalyzer.Prediction prediction = predictiveAnalyzer.evaluate();
        if (!prediction.ready() || !prediction.isLikely() || prediction.confidence() < 0.5) {
            return false;
        }

        int predictionCount = sessionLearning.getPredictionCount();
        double predictionAccuracy = sessionLearning.getPredictionAccuracy();
        if (predictionCount >= 5 && predictionAccuracy < 0.4) {
            logger.debug("Skipping preventive action - prediction accuracy below threshold");
            return false;
        }

        GovernorMode mode = determineMode(state);
        mode = ModePolicy.enforceManualPreference(mode, state.autoTuning() && config.allowAutoTuning);
        ModePolicy policy = ModePolicy.forMode(mode);
        SpikeCausalityReport spikeCausality = resolveSpikeCausality();
        String bound = applyCausalityBound(detectBound(state), spikeCausality);
        long lastActionTimestamp = state.governorLastActionTimestamp();
        boolean benchmarkMode = config.benchmarkModeEnabled;
        if (!governor.canAct(state, lastActionTimestamp, nowMillis, benchmarkMode, config.benchmarkMicroIntervalMillis)) {
            return false;
        }

        // FIXED: Use config OptimizationProfile and convert
        dev.nozh.core.config.OptimizationProfile configProfile = dev.nozh.core.config.OptimizationProfile.fromConfig(config.optimizationProfile);
        OptimizationProfile profile = configProfile.isAggressive() ? OptimizationProfile.AGGRESSIVE : OptimizationProfile.BALANCED;
        
        ActionCandidate decision = null;
        List<ActionCandidate> candidates = actionMatrix.generateCandidates(
                policy,
                bound,
                state.currentScenario(),
                profile,
                state.p95FrametimeMs(),
                state.spikeCount(),
                actionMatrixTuning);
        applyCausalityPriority(candidates, spikeCausality);
        for (ActionCandidate candidate : candidates) {
            if (candidate.targetValue() == null) {
                continue;
            }
            if (candidate.rollbackGuarantee() != RollbackGuarantee.STRONG) {
                continue;
            }
            decision = candidate;
            break;
        }

        if (decision == null) {
            return false;
        }

        successTracker.recordDecision(decision);
        ActionCandidate preventiveDecision = decision;
        String actionSummary = "preventive(" + prediction.window().name().toLowerCase(Locale.ROOT) + ") "
                + formatActionSummary(preventiveDecision);

        logger.info("Preventive governor decision: " + preventiveDecision.reason());
        try {
            stateStore.update(currentState -> currentState.withDecision(
                    "preventive: " + preventiveDecision.reason(),
                    nowMillis));
        } catch (Exception e) {
            logger.warn("Failed to update state decision: " + e.getMessage());
        }

        if (policy.requiresUserConfirmation() && preventiveDecision.targetValue() != null) {
            if (state.suggestedActions() != null) {
                for (PendingAction existing : state.suggestedActions()) {
                    if (existing.capability() == preventiveDecision.capabilityId()
                            && existing.newValue().equals(preventiveDecision.targetValue())) {
                        logger.debug("Suggestion already queued, skipping duplicate preventive suggestion");
                        return true;
                    }
                }
            }
            PerfSnapshot baselineSnapshot = captureSnapshot();
            Optional<dev.nozh.core.capability.CapabilityValue> previousValue = providerRegistry.get(preventiveDecision.capabilityId())
                    .flatMap(provider -> provider.getCurrentValueSafe());
            Command cmd = new Command.ApplyCapability(
                    preventiveDecision.capabilityId(),
                    preventiveDecision.targetValue());
            PendingAction pending = new PendingAction(
                    nowMillis,
                    totalTicks,
                    preventiveDecision.capabilityId(),
                    cmd,
                    previousValue,
                    preventiveDecision.targetValue(),
                    state.avgFrametimeMs(),
                    state.p95FrametimeMs(),
                    state.currentScenario(),
                    state.scenarioConfidence(),
                    baselineSnapshot);
            try {
                stateStore.update(currentState -> currentState.withSuggestedAction(pending));
            } catch (Exception e) {
                logger.warn("Failed to store preventive suggested action: " + e.getMessage());
            }
            logger.info("Preventive suggestion queued for manual assist: " + actionSummary);
            return true;
        }

        PerfSnapshot baselineSnapshot = captureSnapshot();
        int observationWindowSeconds = resolveObservationWindowSeconds(config, baselineSnapshot);
        Optional<dev.nozh.core.capability.CapabilityValue> previousValue = providerRegistry.get(preventiveDecision.capabilityId())
                .flatMap(provider -> provider.getCurrentValueSafe());
        Command cmd = new Command.ApplyCapability(
                preventiveDecision.capabilityId(),
                preventiveDecision.targetValue());
        PendingAction pending = new PendingAction(
                nowMillis,
                totalTicks,
                preventiveDecision.capabilityId(),
                cmd,
                previousValue,
                preventiveDecision.targetValue(),
                state.avgFrametimeMs(),
                state.p95FrametimeMs(),
                state.currentScenario(),
                state.scenarioConfidence(),
                baselineSnapshot);

        ActionHistoryEntry actionEntry = new ActionHistoryEntry(
                nowMillis,
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
                logger.info("Preventive governor action succeeded");
                predictiveAnalyzer.reset();
                refreshCurrentSettings();
            } else {
                logger.warn("Preventive governor action failed: " +
                        report.error().orElse("unknown"));
                try {
                    stateStore.update(RuntimeState::withPendingActionCleared);
                } catch (Exception e) {
                    logger.error("Failed to clear pending action after preventive execution failure");
                }
            }
        });

        try {
            stateStore.update(currentState -> currentState.withGovernorAction(
                    nowMillis,
                    pending,
                    actionEntry,
                    config.historyMaxEntries));
        } catch (Exception e) {
            logger.warn("Failed to update state after preventive action: " + e.getMessage());
        }
        return true;
    }

    private void syncBaselineIfNeeded(RuntimeState state) {
        if (state.baselineSnapshot() != null && !state.baselineSnapshot().isEmpty()) {
            return;
        }
        refreshBaselineSettings();
        refreshCurrentSettings();
    }

    private void refreshBaselineSettings() {
        java.util.Map<dev.nozh.core.capability.CapabilityId, dev.nozh.core.capability.CapabilityValue> baseline = new java.util.EnumMap<>(
                dev.nozh.core.capability.CapabilityId.class);
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
        java.util.Map<dev.nozh.core.capability.CapabilityId, dev.nozh.core.capability.CapabilityValue> current = new java.util.EnumMap<>(
                dev.nozh.core.capability.CapabilityId.class);
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

    private SpikeCausalityReport resolveSpikeCausality() {
        if (perfManager == null) {
            return SpikeCausalityReport.unknown();
        }
        SpikeCausalityReport report = perfManager.getSpikeCausality();
        if (report == null) {
            return SpikeCausalityReport.unknown();
        }
        if (sessionLearning != null && report.cause() != null) {
            double learnedConfidence = sessionLearning.getCausalityConfidence(report.cause());
            if (learnedConfidence > 0.0 && report.confidence() > 0.0) {
                double blended = Math.min(0.95, report.confidence() * 0.7 + learnedConfidence * 0.3);
                report = new SpikeCausalityReport(report.cause(), blended, report.detail());
            }
            sessionLearning.recordCausality(report.cause(), report.confidence());
        }
        return report;
    }

    private String applyCausalityBound(String detectedBound, SpikeCausalityReport report) {
        return CausalityPriorityResolver.applyBound(detectedBound, report);
    }

    private void applyCausalityPriority(List<ActionCandidate> candidates, SpikeCausalityReport report) {
        CausalityPriorityResolver.prioritizeCandidates(candidates, report);
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

        double frameThresholdMs = 16.67;
        boolean frameHigh = frameDataAvailable && (avgMs > frameThresholdMs || p95Ms > frameThresholdMs);

        String heuristicBound = "BALANCED";
        if (tickHigh) {
            heuristicBound = "CPU";
        } else if (frameHigh) {
            heuristicBound = "GPU";
        }

        double renderTimeMs = resolveRenderTimeMs(avgMs, p95Ms);
        double tickTimeMs = resolveTickTimeMs(tickAvgMs, tickP95Ms);
        boolean systemDataAvailable = renderTimeMs >= 0 && tickTimeMs >= 0;
        if (!systemDataAvailable) {
            return heuristicBound;
        }

        double resolutionScale = resolveResolutionScale(state);
        boolean shadersActive = IrisCompat.areShadersLikelyActive();
        String systemBound = systemMonitor.detectBottleneck(
                tickTimeMs,
                renderTimeMs,
                -1,
                shadersActive,
                resolutionScale);

        if (!systemBound.equals(heuristicBound)) {
            logger.debug(String.format(
                    "Bound telemetry mismatch: heuristic=%s system=%s (tick=%.2fms render=%.2fms cpu=%.1f%% scale=%.2f shaders=%s)",
                    heuristicBound,
                    systemBound,
                    tickTimeMs,
                    renderTimeMs,
                    systemMonitor.getSystemCpuLoad() * 100,
                    resolutionScale,
                    shadersActive));
        }

        if (!"BALANCED".equals(systemBound) && !systemBound.equals(heuristicBound)) {
            return systemBound;
        }
        return "BALANCED".equals(heuristicBound) ? systemBound : heuristicBound;
    }

    private double resolveRenderTimeMs(double avgMs, double p95Ms) {
        if (avgMs >= 0) {
            return avgMs;
        }
        if (p95Ms >= 0) {
            return p95Ms;
        }
        return -1.0;
    }

    private double resolveTickTimeMs(double avgMs, double p95Ms) {
        if (avgMs >= 0) {
            return avgMs;
        }
        if (p95Ms >= 0) {
            return p95Ms;
        }
        return -1.0;
    }

    private double resolveResolutionScale(RuntimeState state) {
        if (state == null || state.currentSettings() == null) {
            return 1.0;
        }
        CapabilityValue value = state.currentSettings().get(CapabilityId.RESOLUTION_SCALE);
        if (value instanceof CapabilityValue.FloatValue floatValue) {
            return floatValue.value();
        }
        if (value instanceof CapabilityValue.IntValue intValue) {
            return intValue.value();
        }
        return 1.0;
    }

    private GovernorMode determineMode(RuntimeState state) {
        if (!state.enabled() || state.governorDisabled()) {
            return GovernorMode.OFF;
        }
        if (state.safeMode() || !state.autoTuning()) {
            return GovernorMode.MANUAL_ASSIST;
        }
        GovernorMode baseMode = state.currentScenario() == dev.nozh.core.context.Scenario.COMBAT
                ? GovernorMode.AUTO_AGGRESSIVE
                : GovernorMode.AUTO_CONSERVATIVE;
        dev.nozh.core.context.ScenarioConfidence confidence = state.scenarioConfidenceInfo();
        if (confidence.band() == dev.nozh.core.context.ScenarioConfidence.Band.LOW
                || confidence.stability() < 0.45) {
            return baseMode == GovernorMode.AUTO_AGGRESSIVE ? GovernorMode.AUTO_CONSERVATIVE : GovernorMode.MANUAL_ASSIST;
        }
        if (confidence.band() == dev.nozh.core.context.ScenarioConfidence.Band.MEDIUM
                && baseMode == GovernorMode.AUTO_AGGRESSIVE) {
            return GovernorMode.AUTO_CONSERVATIVE;
        }
        return baseMode;
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
            sessionLearning.recordOutcome(pending.capability(), pending.scenario(), ActionOutcome.NEGATIVE, 0.0, 0.0, 0);
            successTracker.recordFailure(pending.capability());
            try {
                stateStore.update(RuntimeState::withPendingActionCleared);
            } catch (Exception e) {
                logger.warn("Failed to clear pending action after fallback");
            }
            successTracker.clearDecisionSnapshot(pending.capability());
            return;
        }
        PerfSnapshot currentSnapshot = captureSnapshot();
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

        int observationWindowSeconds = resolveObservationWindowSeconds(config,
                currentSnapshot != null ? currentSnapshot : baselineSnapshot);
        double p95Delta = resolveDelta(currentSnapshot, baselineSnapshot, PerfSnapshot::p95FrametimeMs);
        int spikeDelta = resolveSpikeDelta(currentSnapshot, baselineSnapshot);
        if (spikeDelta > 0) {
            outcome = ActionOutcome.NEGATIVE;
        }

        boolean rollbackRequested = false;
        boolean rollbackApplied = false;
        if (outcome != ActionOutcome.POSITIVE && sufficientData) {
            if (config.rollbackEnabled) {
                Long lastRollback = rollbackCooldowns.get(pending.capability());
                if (lastRollback != null && nowMillis() - lastRollback < config.rollbackCooldownMillis) {
                    logger.info("Rollback skipped due to cooldown for " + pending.capability());
                } else {
                    rollbackRequested = true;
                }
            } else {
                logger.info("Rollback disabled in config, keeping ineffective action.");
            }

            sessionLearning.recordOutcome(pending.capability(), pending.scenario(), ActionOutcome.NEGATIVE, 0.0,
                    p95Delta, spikeDelta);
            successTracker.recordFailure(pending.capability());
        } else if (outcome == ActionOutcome.POSITIVE) {
            double gainAvg = resolveGain(pending.baselineAvgMs(), currentSnapshot != null ? currentSnapshot.avgFrametimeMs() : Double.NaN);
            double gainP95 = resolveGain(pending.baselineP95Ms(), currentSnapshot != null ? currentSnapshot.p95FrametimeMs() : Double.NaN);
            sessionLearning.recordOutcome(pending.capability(), pending.scenario(), ActionOutcome.POSITIVE,
                    Math.max(gainAvg, gainP95), p95Delta, spikeDelta);
            successTracker.recordSuccess(pending.capability());
        } else {
            sessionLearning.recordOutcome(pending.capability(), pending.scenario(), ActionOutcome.NEUTRAL, 0.0,
                    p95Delta, spikeDelta);
        }

        logger.debug(String.format(
                "Governor action evaluation action=%s impact=%s decision=%s p95Delta=%.2fms spikesDelta=%d window=%ds",
                pending.capability(),
                outcome,
                rollbackRequested ? "rollback" : "keep",
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
        if (rollbackRequested) {
            rollbackApplied = attemptMetricRollback(state, pending);
            if (rollbackApplied) {
                rollbackCooldowns.put(pending.capability(), nowMillis());
            }
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

    private void syncObservationWindow(NozhConfig config) {
        if (config == null || config.observationWindowSeconds <= 0) {
            return;
        }
        if (config.observationWindowSeconds == lastObservationWindowSeconds) {
            return;
        }
        if (perfManager != null) {
            perfManager.setObservationWindowSeconds(config.observationWindowSeconds);
        }
        lastObservationWindowSeconds = config.observationWindowSeconds;
    }

    private void refreshActionMatrixTuning(NozhConfig config) {
        String hardwareProfile = config != null ? config.hardwareProfile : "";
        String modpackId = modpackProfile.map(ModpackProfile::id).orElse("");
        if (Objects.equals(lastHardwareProfile, hardwareProfile)
                && Objects.equals(lastModpackId, modpackId)) {
            return;
        }
        HardwareProfile hardware = HardwareProfile.parse(hardwareProfile).orElse(HardwareProfile.unknown());
        actionMatrixTuning = PresetTuningResolver.resolve(hardware, modpackProfile.orElse(null));
        lastHardwareProfile = hardwareProfile;
        lastModpackId = modpackId;
    }

    private void applyPredictionLearningFeedback() {
        predictiveAnalyzer.applyLearning(
                sessionLearning.getPredictionAccuracy(),
                sessionLearning.getPredictionAvgConfidence());
    }

    private PerfSnapshot captureSnapshot() {
        PerfSnapshot snapshot = perfManager != null ? perfManager.getSnapshot() : perfSnapshotSupplier.get();
        return snapshot != null ? snapshot : PerfSnapshot.empty();
    }

    private long resolveEvaluationWindowMillis(NozhConfig config) {
        if (config == null) {
            return 0L;
        }
        if (config.benchmarkModeEnabled) {
            return Math.max(1000L, config.benchmarkMicroIntervalMillis);
        }
        if (config.observationWindowSeconds > 0) {
            return config.observationWindowSeconds * 1000L;
        }
        return config.rollbackWindowMillis;
    }

    private boolean attemptMetricRollback(RuntimeState state, PendingAction pending) {
        Optional<CapabilityValue> rollbackValue = resolveRollbackValue(state, pending);
        if (rollbackValue.isEmpty()) {
            logger.warn("No rollback value available for " + pending.capability());
            return false;
        }

        Optional<Command> rollbackCommand = pending.command().inverse(rollbackValue);
        if (rollbackCommand.isEmpty()) {
            logger.warn("Rollback command unavailable for " + pending.capability());
            return false;
        }

        actionBus.dispatch(rollbackCommand.get(), report -> {
            boolean rollbackSucceeded = report.succeeded();
            if (rollbackSucceeded) {
                logger.info("Rollback succeeded (Action was ineffective)");
            } else {
                logger.warn("Rollback failed for " + pending.capability() + ": " + report.error().orElse("unknown"));
            }
            updateRollbackOutcome(pending.timestampMillis(), rollbackSucceeded);
        });
        return true;
    }

    private Optional<CapabilityValue> resolveRollbackValue(RuntimeState state, PendingAction pending) {
        Optional<CapabilityValue> candidate = pending.previousValue();
        BaselineSnapshot baselineSnapshot = state != null ? state.baselineSnapshot() : BaselineSnapshot.empty();
        CapabilityValue baseline = baselineSnapshot.get(pending.capability()).orElse(null);

        if (candidate.isPresent()) {
            CapabilityValue value = candidate.get();
            if (baseline != null && baselineSnapshot.exceedsBaseline(pending.capability(), value)) {
                return Optional.of(baseline);
            }
            return Optional.of(value);
        }

        if (baseline != null) {
            return Optional.of(baseline);
        }

        return Optional.empty();
    }

    private void updateRollbackOutcome(long timestampMillis, boolean rollbackApplied) {
        try {
            stateStore.update(currentState -> {
                ActionHistoryEntry entry = null;
                for (ActionHistoryEntry historyEntry : currentState.actionHistory()) {
                    if (historyEntry.timestampMillis() == timestampMillis) {
                        entry = historyEntry;
                        break;
                    }
                }
                if (entry == null) {
                    return currentState;
                }
                ActionHistoryEntry updatedEntry = new ActionHistoryEntry(
                        entry.timestampMillis(),
                        entry.actionSummary(),
                        entry.scenario(),
                        entry.scenarioConfidence(),
                        entry.beforeSnapshot(),
                        entry.afterSnapshot(),
                        entry.p95DeltaMs(),
                        entry.spikeDelta(),
                        entry.observationWindowSeconds(),
                        entry.outcome(),
                        rollbackApplied);
                return currentState.withActionOutcome(timestampMillis, updatedEntry);
            });
        } catch (Exception e) {
            logger.warn("Failed to update rollback outcome state");
        }
    }


    private int resolveObservationWindowSeconds(NozhConfig config, PerfSnapshot snapshot) {
        if (config != null && config.observationWindowSeconds > 0) {
            return config.observationWindowSeconds;
        }
        if (snapshot == null || snapshot.windowSeconds() <= 0) {
            return 0;
        }
        return snapshot.windowSeconds();
    }

    private String formatActionSummary(ActionCandidate decision) {
        String value = formatCapabilityValue(decision.targetValue());
        if (isReverseAction(decision)) {
            return "restore " + decision.capabilityId().toString() + (value.isEmpty() ? "" : "=" + value);
        }
        if (value.isEmpty()) {
            return decision.capabilityId().toString();
        }
        return decision.capabilityId().toString() + "=" + value;
    }

    private boolean isReverseAction(ActionCandidate decision) {
        return decision != null
                && decision.reason() != null
                && decision.reason().contains("restore baseline");
    }

    private boolean updateReverseImprovement(RuntimeState state, NozhConfig config) {
        if (state == null || config == null) {
            reverseP95Streak = 0;
            reverseSpikeStreak = 0;
            return false;
        }
        double p95 = state.p95FrametimeMs();
        int spikes = state.spikeCount();
        boolean p95Valid = p95 > 0;
        boolean spikesValid = spikes >= 0;

        if (!p95Valid || !spikesValid) {
            reverseP95Streak = 0;
            reverseSpikeStreak = 0;
            if (p95Valid) {
                lastReverseP95Ms = p95;
            }
            if (spikesValid) {
                lastReverseSpikes = spikes;
            }
            return false;
        }

        boolean p95Improved = lastReverseP95Ms > 0
                && p95 <= (lastReverseP95Ms - config.improvementEpsilonP95Ms);
        boolean spikesImproved = lastReverseSpikes >= 0
                && (spikes < lastReverseSpikes || (spikes == 0 && lastReverseSpikes == 0));

        reverseP95Streak = p95Improved ? reverseP95Streak + 1 : 0;
        reverseSpikeStreak = spikesImproved ? reverseSpikeStreak + 1 : 0;

        lastReverseP95Ms = p95;
        lastReverseSpikes = spikes;

        return reverseP95Streak >= REVERSE_IMPROVEMENT_STREAK
                && reverseSpikeStreak >= REVERSE_IMPROVEMENT_STREAK;
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

    private void evaluatePredictionAccuracy(RuntimeState state, NozhConfig config, long nowMillis) {
        if (state == null) {
            return;
        }
        evaluateSpikePredictionAccuracy(state, config, nowMillis);
        evaluateFrametimePredictionAccuracy(state, config, nowMillis);
    }

    private void updateSpikePrediction(RuntimeState state, NozhConfig config, long nowMillis) {
        if (perfManager == null || state == null || pendingSpikePrediction != null) {
            return;
        }
        dev.nozh.core.profiler.SpikePrediction prediction = perfManager.getSpikePrediction();
        if (prediction == null) {
            return;
        }
        if ("INSUFFICIENT_DATA".equals(prediction.reason())) {
            return;
        }
        lastSpikePredictionMillis = nowMillis;
        lastSpikePredictionCount = state.spikeCount();
        pendingSpikePrediction = prediction;
        if (prediction.confidence() >= 0.5) {
            logger.debug(String.format(
                    "Spike prediction: likely=%s confidence=%.2f reason=%s",
                    prediction.spikeLikely(),
                    prediction.confidence(),
                    prediction.reason()));
        }
    }

    private void updateFrametimePrediction(RuntimeState state, NozhConfig config, long nowMillis) {
        if (state == null || pendingFrametimePrediction) {
            return;
        }
        PredictiveAnalyzer.Prediction prediction = predictiveAnalyzer.evaluate();
        if (!prediction.ready() || !prediction.isLikely()) {
            return;
        }
        if (prediction.confidence() < 0.5) {
            return;
        }
        pendingFrametimePrediction = true;
        pendingFrametimeConfidence = prediction.confidence();
        lastFrametimePredictionMillis = nowMillis;
        lastFrametimePredictionAvgMs = state.avgFrametimeMs();
        lastFrametimePredictionP95Ms = state.p95FrametimeMs();
        lastFrametimePredictionSpikes = state.spikeCount();
        logger.debug(String.format(
                "Frametime prediction: window=%s confidence=%.2f trend=%s",
                prediction.window(),
                prediction.confidence(),
                predictiveAnalyzer.getTrendDescription()));
    }

    private void evaluateSpikePredictionAccuracy(RuntimeState state, NozhConfig config, long nowMillis) {
        if (pendingSpikePrediction == null) {
            return;
        }
        long windowMs = resolvePredictionWindowMillis(config);
        if (nowMillis - lastSpikePredictionMillis < windowMs) {
            return;
        }
        if (lastSpikePredictionCount < 0) {
            pendingSpikePrediction = null;
            return;
        }
        boolean actualSpike = state.spikeCount() > lastSpikePredictionCount;
        sessionLearning.recordPredictionOutcome(pendingSpikePrediction.spikeLikely(), actualSpike,
                pendingSpikePrediction.confidence());
        pendingSpikePrediction = null;
    }

    private void evaluateFrametimePredictionAccuracy(RuntimeState state, NozhConfig config, long nowMillis) {
        if (!pendingFrametimePrediction) {
            return;
        }
        long windowMs = resolvePredictionWindowMillis(config);
        if (nowMillis - lastFrametimePredictionMillis < windowMs) {
            return;
        }
        boolean actualSpike = isPredictionSpike(state, config);
        sessionLearning.recordPredictionOutcome(true, actualSpike, pendingFrametimeConfidence);
        pendingFrametimePrediction = false;
    }

    private boolean isPredictionSpike(RuntimeState state, NozhConfig config) {
        if (state == null) {
            return false;
        }
        double avgEpsilon = config != null ? config.improvementEpsilonAvgMs : 0.5;
        double p95Epsilon = config != null ? config.improvementEpsilonP95Ms : 1.0;

        double avgMs = state.avgFrametimeMs();
        double p95Ms = state.p95FrametimeMs();
        boolean avgSpike = isValidPerfValue(avgMs)
                && isValidPerfValue(lastFrametimePredictionAvgMs)
                && avgMs > lastFrametimePredictionAvgMs + avgEpsilon;
        boolean p95Spike = isValidPerfValue(p95Ms)
                && isValidPerfValue(lastFrametimePredictionP95Ms)
                && p95Ms > lastFrametimePredictionP95Ms + p95Epsilon;
        boolean spikeRise = lastFrametimePredictionSpikes >= 0
                && state.spikeCount() > lastFrametimePredictionSpikes;

        return avgSpike || p95Spike || spikeRise;
    }

    private long resolvePredictionWindowMillis(NozhConfig config) {
        if (config != null && config.observationWindowSeconds > 0) {
            return Math.max(SPIKE_PREDICTION_MIN_WINDOW_MS, config.observationWindowSeconds * 1000L);
        }
        return SPIKE_PREDICTION_MIN_WINDOW_MS;
    }
}
