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
        String lastDecisionReason,
        long lastDecisionTimestamp,
        long sessionStartTime,
        int stateVersion,
        dev.nozh.core.context.Scenario currentScenario,
        double scenarioConfidence,
        long lastScenarioChangeTimestamp,
        int scenarioChangeCount,
        int rapidScenarioChangeCount,
        int combatAfkFlipCount,
        Map<CapabilityId, CapabilityValue> baselineSettings,
        Map<CapabilityId, CapabilityValue> currentSettings) {
    private static final int CURRENT_VERSION = 7; // Bump version

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
                "", 0L, // lastDecisionReason, lastDecisionTimestamp
                System.currentTimeMillis(), CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD, 0.5,
                0L,
                0,
                0,
                0,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                baselineSettings, currentSettings);
    }

    /**
     * Update telemetry metrics (immutable).
     */
    public RuntimeState withTelemetry(double avg, double p95, double p99, double stddev, int spikes, double tickAvg,
            double tickP95) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedActions, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avg, p95, p99, stddev, tickAvg, tickP95, spikes,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                scenario, confidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                baselineSettings, currentSettings);
    }

    public RuntimeState withScenarioUpdate(dev.nozh.core.context.Scenario scenario, double confidence, long nowMillis,
            boolean changed, boolean rapidChange, boolean combatAfkFlip) {
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                scenario, confidence,
                changeTimestamp, changeCount, rapidCount, flipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                reason != null ? reason : "",
                timestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                "", 0L, // lastDecisionReason, lastDecisionTimestamp
                System.currentTimeMillis(),
                CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD,
                0.5,
                0L,
                0,
                0,
                0,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
                baseline,
                currentSettings);
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                lastScenarioChangeTimestamp, scenarioChangeCount, rapidScenarioChangeCount, combatAfkFlipCount,
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
}
