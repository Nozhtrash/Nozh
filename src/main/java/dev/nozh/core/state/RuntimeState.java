/**
 * NOZH - Adaptive Performance Optimization
 * Copyright (c) 2025 NOZH Project
 * 
 * Licensed under the MIT License.
 * 
 * Architecture: Contract-based state management with immutability guarantees.
 * This file implements Contract 1: StateStore Purity.
 */
package dev.nozh.core.state;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.nozh.core.state.ActionHistoryEntry;
import dev.nozh.core.governor.ActionOutcome;

/**
 * Runtime state (Contract 1).
 *
 * IMMUTABLE record. All mutations create new instances.
 * 
 * Why immutable: Prevents accidental state corruption from concurrent access
 * (governor thread, HUD render thread, MC main thread). Snapshots are defensive
 * copies that can be safely passed to pure transformations without side
 * effects.
 */
public record RuntimeState(
        boolean enabled,
        boolean safeMode,
        boolean autoTuning,
        boolean debugLogs,
        boolean governorDisabled,
        boolean governorCooldownActive,
        long governorLastActionTimestamp,
        boolean benchmarkRunning,
        String benchmarkValidity,
        long benchmarkStartTimestamp,
        Optional<PendingAction> pendingAction,
        List<PendingAction> suggestedActions,
        int pendingActionsCount,
        int executionHistorySize,
        int lastSnapshotHistorySize,
        List<ActionHistoryEntry> actionHistory,
        int sessionChangesCount,
        double avgFrametimeMs,
        double p95FrametimeMs,
        double p99FrametimeMs,
        double frametimeStddevMs,
        double tickTimeAvg,
        double tickTimeP95,
        int spikeCount,
        StabilityStats stabilityStats,
        String lastDecisionReason,
        long lastDecisionTimestamp,
        double lastImpactMs,
        ActionOutcome lastOutcome,
        boolean lastDecisionAccepted,
        long sessionStartTime,
        int stateVersion,
        dev.nozh.core.context.Scenario currentScenario,
        double scenarioConfidence,
        long lastScenarioChangeTimestamp,
        int scenarioChangeCount,
        int rapidScenarioChangeCount,
        int combatAfkFlipCount,
        List<ScenarioHistoryEntry> scenarioHistory,
        Map<CapabilityId, CapabilityValue> baselineSettings,
        Map<CapabilityId, CapabilityValue> currentSettings) {
    private static final int CURRENT_VERSION = 10; // Bump version
    private static final long SCENARIO_HISTORY_WINDOW_MS = 20_000L;
    private static final double SCENARIO_DOMINANCE_THRESHOLD = 0.55;
    private static final long RAPID_SCENARIO_CHANGE_WINDOW_MS = 5_000L;

    /**
     * Create default initial state.
     */
    public static RuntimeState defaults() {
        return new RuntimeState(
                true, // enabled
                false, // safeMode
                false, // autoTuning
                false, // debugLogs
                false, // governorDisabled
                false, // governorCooldownActive
                0L, // governorLastActionTimestamp
                false, // benchmarkRunning
                "NONE", // benchmarkValidity
                0L, // benchmarkStartTimestamp
                Optional.empty(), // pendingAction
                List.of(), // suggestedActions
                0, // pendingActionsCount
                0, // executionHistorySize
                0, // lastSnapshotHistorySize
                List.of(), // actionHistory
                0, // sessionChangesCount
                -1.0, // avgFrametimeMs
                -1.0, // p95FrametimeMs
                -1.0, // p99FrametimeMs
                -1.0, // frametimeStddevMs
                -1.0, // tickTimeAvg
                -1.0, // tickTimeP95
                0, // spikeCount
                StabilityStats.defaults(),
                "", 0L, // lastDecisionReason, lastDecisionTimestamp
                0.0, // lastImpactMs
                ActionOutcome.NEUTRAL, // lastOutcome
                true, // lastDecisionAccepted
                System.currentTimeMillis(), CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD, 0.5,
                0L,
                0,
                0,
                0,
                List.of(),
                Map.of(),
                Map.of());
    }

    /**
     * Update after governor action (immutable).
     */
    public RuntimeState withGovernorAction(long timestamp, PendingAction pending, ActionHistoryEntry actionEntry,
            int maxHistoryEntries) {
        List<ActionHistoryEntry> updatedHistory = mergeHistory(actionHistory, actionEntry, maxHistoryEntries);
        int nextHistorySize = executionHistorySize + 1;
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                true, // governorCooldownActive = true after action
                timestamp, // governorLastActionTimestamp
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                Optional.of(pending),
                suggestedActions,
                pendingAction.isPresent() ? pendingActionsCount + 1 : 1,
                nextHistorySize,
                lastSnapshotHistorySize,
                updatedHistory,
                sessionChangesCount + 1, // increment changes
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    public RuntimeState withAppliedSuggestion(long timestamp, PendingAction pending, ActionHistoryEntry actionEntry,
            int maxHistoryEntries) {
        List<ActionHistoryEntry> updatedHistory = mergeHistory(actionHistory, actionEntry, maxHistoryEntries);
        List<PendingAction> updatedSuggestions = new ArrayList<>(suggestedActions != null ? suggestedActions : List.of());
        if (pending != null && updatedSuggestions.contains(pending)) {
            updatedSuggestions.remove(pending);
        } else if (!updatedSuggestions.isEmpty()) {
            updatedSuggestions.remove(0);
        }
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                true,
                timestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                Optional.of(pending),
                updatedSuggestions,
                pendingAction.isPresent() ? pendingActionsCount + 1 : 1,
                executionHistorySize + 1,
                lastSnapshotHistorySize,
                updatedHistory,
                sessionChangesCount + 1,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Clear any pending action (immutable).
     */
    public RuntimeState withPendingActionCleared() {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                governorCooldownActive,
                governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                Optional.empty(),
                suggestedActions,
                0,
                executionHistorySize,
                lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Update suggested action (manual assist).
     */
    public RuntimeState withSuggestedAction(PendingAction pending) {
        List<PendingAction> updatedSuggestions = new ArrayList<>(suggestedActions != null ? suggestedActions : List.of());
        if (pending != null) {
            updatedSuggestions.add(pending);
        }
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                governorCooldownActive,
                governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                updatedSuggestions,
                pendingActionsCount,
                executionHistorySize,
                lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Clear any suggested action (manual assist).
     */
    public RuntimeState withSuggestedActionCleared() {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                governorCooldownActive,
                governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                List.of(),
                pendingActionsCount,
                executionHistorySize,
                lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Update benchmark status (immutable).
     */
    public RuntimeState withBenchmarkStatus(boolean running, long startTime) {
        return withBenchmarkStatus(running, startTime, benchmarkValidity);
    }

    /**
     * Update benchmark status (immutable) with validity.
     */
    public RuntimeState withBenchmarkStatus(boolean running, long startTime, String validity) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                running ? true : governorDisabled, // disable governor during benchmark
                governorCooldownActive, governorLastActionTimestamp,
                running, validity, startTime,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Update telemetry metrics (immutable).
     */
    public RuntimeState withTelemetry(double avg, double p95, double p99, double stddev, int spikes, double tickAvg,
            double tickP95) {
        StabilityStats updatedStability = stabilityStats != null ? stabilityStats : StabilityStats.defaults();
        updatedStability = updatedStability.update(
                avg,
                p95,
                stddev,
                spikes,
                scenarioConfidenceInfo().stability(),
                System.currentTimeMillis());
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avg, p95, p99, stddev, tickAvg, tickP95, spikes,
                updatedStability,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Clear governor cooldown (immutable).
     */
    public RuntimeState withCooldownCleared() {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                false, // governorCooldownActive = false
                governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Update current scenario (immutable).
     */
    public RuntimeState withScenario(dev.nozh.core.context.Scenario scenario, double confidence) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                scenario, confidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    public RuntimeState withScenarioUpdate(dev.nozh.core.context.Scenario scenario, double confidence, long nowMillis) {
        List<ScenarioHistoryEntry> updatedHistory = updateScenarioHistory(scenarioHistory, scenario, confidence,
                nowMillis);
        ScenarioAggregate aggregate = resolveScenarioAggregate(updatedHistory, currentScenario);
        dev.nozh.core.context.Scenario resolvedScenario = aggregate.scenario();
        double resolvedConfidence = aggregate.confidence();
        boolean changed = resolvedScenario != currentScenario;
        boolean rapidChange = changed
                && lastScenarioChangeTimestamp > 0
                && nowMillis - lastScenarioChangeTimestamp <= RAPID_SCENARIO_CHANGE_WINDOW_MS;
        boolean combatAfkFlip = changed && isCombatAfkFlip(currentScenario, resolvedScenario);
        long changeTimestamp = changed ? nowMillis : lastScenarioChangeTimestamp;
        int changeCount = scenarioChangeCount + (changed ? 1 : 0);
        int rapidCount = rapidScenarioChangeCount + (rapidChange ? 1 : 0);
        int flipCount = combatAfkFlipCount + (combatAfkFlip ? 1 : 0);
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                resolvedScenario, resolvedConfidence,
                changeTimestamp, changeCount, rapidCount, flipCount,
                updatedHistory,
                baselineSettings, currentSettings);
    }

    // REMOVED: withGovernorSnapshot() - uses deleted GovernorMode and ParanoiaLevel
    // parameters

    // REMOVED: withDecision() - uses deleted governorMode, paranoiaLevel, etc.
    // parameters

    // REMOVED: withRecentAction() - uses deleted ActionHistoryEntry and
    // recentActions parameters

    public RuntimeState withConfig(dev.nozh.core.config.NozhConfig config) {
        return new RuntimeState(
                config.enabled,
                safeMode,
                config.allowAutoTuning,
                config.debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                suggestedActions,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Update last decision (immutable).
     */
    public RuntimeState withDecision(String reason, long timestamp) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                suggestedActions,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                reason != null ? reason : "",
                timestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    public RuntimeState withPendingSuggestion(PendingAction suggestion) {
        List<PendingAction> updatedSuggestions = new ArrayList<>(suggestedActions != null ? suggestedActions : List.of());
        if (suggestion != null) {
            updatedSuggestions.add(suggestion);
        }
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                updatedSuggestions,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    public RuntimeState withPendingSuggestionCleared() {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                List.of(),
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    /**
     * Create state from config (initialization).
     */
    public static RuntimeState fromConfig(dev.nozh.core.config.NozhConfig config) {
        return new RuntimeState(
                config.enabled,
                config.safeModeForce, // safeMode from config
                config.allowAutoTuning, // autoTuning mapped from allowAutoTuning
                config.debugLogs,
                false, // governorDisabled (runtime only)
                false, // governorCooldownActive (runtime only)
                0L, // governorLastActionTimestamp
                false, // benchmarkRunning (runtime only)
                "NONE", // benchmarkValidity
                0L, // benchmarkStartTimestamp
                Optional.empty(), // pendingAction
                List.of(), // suggestedActions
                0, // pendingActionsCount
                0, // executionHistorySize
                0, // lastSnapshotHistorySize
                List.of(), // actionHistory
                0, // sessionChangesCount
                -1.0, // avgFrametimeMs
                -1.0, // p95FrametimeMs
                -1.0, // p99FrametimeMs
                -1.0, // frametimeStddevMs
                -1.0, // tickTimeAvg
                -1.0, // tickTimeP95
                0, // spikeCount
                StabilityStats.defaults(),
                "", 0L, // lastDecisionReason, lastDecisionTimestamp
                0.0, // lastImpactMs
                ActionOutcome.NEUTRAL, // lastOutcome
                true, // lastDecisionAccepted
                System.currentTimeMillis(),
                CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD,
                0.5,
                0L,
                0,
                0,
                0,
                List.of(),
                Map.of(),
                Map.of());
    }

    public RuntimeState withBaselineSettings(Map<CapabilityId, CapabilityValue> baseline) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baseline,
                currentSettings);
    }

    public BaselineSnapshot baselineSnapshot() {
        if (baselineSettings == null || baselineSettings.isEmpty()) {
            return BaselineSnapshot.empty();
        }
        return new BaselineSnapshot(baselineSettings);
    }

    public RuntimeState withCurrentSettings(Map<CapabilityId, CapabilityValue> settings) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                lastImpactMs, lastOutcome, lastDecisionAccepted,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings,
                settings);
    }

    public RuntimeState withActionOutcome(long timestampMillis, ActionHistoryEntry updatedEntry) {
        if (updatedEntry == null) {
            return this;
        }
        List<ActionHistoryEntry> updatedHistory = new ArrayList<>(actionHistory);
        for (int i = 0; i < updatedHistory.size(); i++) {
            ActionHistoryEntry entry = updatedHistory.get(i);
            if (entry.timestampMillis() == timestampMillis) {
                updatedHistory.set(i, updatedEntry);
                break;
            }
        }
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                updatedHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                stabilityStats,
                lastDecisionReason, lastDecisionTimestamp,
                updatedEntry.p95DeltaMs(),
                updatedEntry.outcome(),
                !updatedEntry.rollbackApplied(),
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                scenarioHistory,
                baselineSettings, currentSettings);
    }

    private List<ActionHistoryEntry> mergeHistory(
            List<ActionHistoryEntry> existing,
            ActionHistoryEntry entry,
            int maxEntries) {
        if (entry == null) {
            return existing;
        }
        List<ActionHistoryEntry> updated = new ArrayList<>(existing != null ? existing : List.of());
        updated.add(entry);
        while (updated.size() > maxEntries && maxEntries > 0) {
            updated.remove(0);
        }
        return updated;
    }

    public dev.nozh.core.context.ScenarioConfidence scenarioConfidenceInfo() {
        double stability = calculateScenarioStability(scenarioHistory);
        return dev.nozh.core.context.ScenarioConfidence.from(scenarioConfidence, stability);
    }

    private List<ScenarioHistoryEntry> updateScenarioHistory(
            List<ScenarioHistoryEntry> existing,
            dev.nozh.core.context.Scenario scenario,
            double confidence,
            long nowMillis) {
        List<ScenarioHistoryEntry> updated = new ArrayList<>(existing != null ? existing : List.of());
        updated.add(new ScenarioHistoryEntry(nowMillis, scenario, confidence));
        long cutoff = nowMillis - SCENARIO_HISTORY_WINDOW_MS;
        updated.removeIf(entry -> entry.timestampMillis() < cutoff);
        return updated;
    }

    private ScenarioAggregate resolveScenarioAggregate(List<ScenarioHistoryEntry> history,
            dev.nozh.core.context.Scenario fallbackScenario) {
        if (history == null || history.isEmpty()) {
            return new ScenarioAggregate(fallbackScenario, scenarioConfidence, 1.0);
        }
        java.util.EnumMap<dev.nozh.core.context.Scenario, Double> scores = new java.util.EnumMap<>(
                dev.nozh.core.context.Scenario.class);
        java.util.EnumMap<dev.nozh.core.context.Scenario, Integer> counts = new java.util.EnumMap<>(
                dev.nozh.core.context.Scenario.class);
        double totalScore = 0.0;
        for (ScenarioHistoryEntry entry : history) {
            double score = clamp(entry.confidence());
            scores.merge(entry.scenario(), score, Double::sum);
            counts.merge(entry.scenario(), 1, Integer::sum);
            totalScore += score;
        }
        dev.nozh.core.context.Scenario bestScenario = fallbackScenario;
        double bestScore = -1.0;
        for (var entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestScenario = entry.getKey();
            }
        }
        double stability = totalScore > 0 ? Math.min(1.0, bestScore / totalScore) : 0.0;
        double avgConfidence = 0.0;
        Integer count = counts.get(bestScenario);
        if (count != null && count > 0) {
            avgConfidence = bestScore / count;
        }
        if (stability < SCENARIO_DOMINANCE_THRESHOLD && fallbackScenario != null) {
            bestScenario = fallbackScenario;
        }
        return new ScenarioAggregate(bestScenario, clamp(avgConfidence), stability);
    }

    private double calculateScenarioStability(List<ScenarioHistoryEntry> history) {
        if (history == null || history.isEmpty()) {
            return 1.0;
        }
        ScenarioAggregate aggregate = resolveScenarioAggregate(history, currentScenario);
        return aggregate.stability();
    }

    private boolean isCombatAfkFlip(dev.nozh.core.context.Scenario previous,
            dev.nozh.core.context.Scenario current) {
        return (previous == dev.nozh.core.context.Scenario.COMBAT
                && current == dev.nozh.core.context.Scenario.AFK)
                || (previous == dev.nozh.core.context.Scenario.AFK
                        && current == dev.nozh.core.context.Scenario.COMBAT);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record ScenarioAggregate(dev.nozh.core.context.Scenario scenario, double confidence, double stability) {
    }
}
