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
        Optional<PendingAction> suggestedAction,
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
        Map<CapabilityId, CapabilityValue> baselineSettings,
        Map<CapabilityId, CapabilityValue> currentSettings,
        Map<CapabilityId, List<CapabilityChangeEntry>> capabilityHistory,
        long performanceStableSince,
        boolean performanceStable) {
    private static final int CURRENT_VERSION = 6; // Bump version

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
                Optional.empty(), // suggestedAction
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
                Map.of(),
                Map.of(),
                Map.of(),
                0L,
                false);
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
                suggestedAction,
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
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    public RuntimeState withGovernorAction(long timestamp, PendingAction pending) {
        int nextHistorySize = executionHistorySize + 1;
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                true,
                timestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                Optional.of(pending),
                suggestedAction,
                pendingAction.isPresent() ? pendingActionsCount + 1 : 1,
                nextHistorySize,
                lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount + 1,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    public RuntimeState withAppliedSuggestion(long timestamp, PendingAction pending, ActionHistoryEntry actionEntry,
            int maxHistoryEntries) {
        List<ActionHistoryEntry> updatedHistory = mergeHistory(actionHistory, actionEntry, maxHistoryEntries);
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                true,
                timestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                Optional.of(pending),
                Optional.empty(),
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
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                suggestedAction,
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
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    /**
     * Update suggested action (manual assist).
     */
    public RuntimeState withSuggestedAction(PendingAction pending) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                governorCooldownActive,
                governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                Optional.of(pending),
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
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                Optional.empty(),
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
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avg, p95, p99, stddev, tickAvg, tickP95, spikes,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    /**
     * Update current scenario (immutable).
     */
    public RuntimeState withScenario(dev.nozh.core.context.Scenario scenario, double confidence) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                scenario, confidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                suggestedAction,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                suggestedAction,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                reason != null ? reason : "",
                timestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    public RuntimeState withPendingSuggestion(PendingAction suggestion) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                Optional.of(suggestion),
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    public RuntimeState withPendingSuggestionCleared() {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                Optional.empty(),
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
                Optional.empty(), // suggestedAction
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
                Map.of(),
                Map.of(),
                Map.of(),
                0L,
                false);
    }

    public RuntimeState withBaselineSettings(Map<CapabilityId, CapabilityValue> baseline) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baseline,
                currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    public RuntimeState withCurrentSettings(Map<CapabilityId, CapabilityValue> settings) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings,
                settings,
                capabilityHistory, performanceStableSince, performanceStable);
    }

    public RuntimeState withCapabilityHistoryEntry(
            CapabilityId capabilityId,
            CapabilityValue previousValue,
            CapabilityValue newValue,
            CapabilityChangeType type,
            int maxEntries,
            long timestampMillis) {
        if (capabilityId == null) {
            return this;
        }
        Map<CapabilityId, List<CapabilityChangeEntry>> updated = new java.util.EnumMap<>(CapabilityId.class);
        if (capabilityHistory != null) {
            updated.putAll(capabilityHistory);
        }
        List<CapabilityChangeEntry> history = new ArrayList<>(updated.getOrDefault(capabilityId, List.of()));
        history.add(new CapabilityChangeEntry(timestampMillis, capabilityId, previousValue, newValue, type));
        while (history.size() > maxEntries && maxEntries > 0) {
            history.remove(0);
        }
        updated.put(capabilityId, List.copyOf(history));
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings,
                currentSettings,
                updated, performanceStableSince, performanceStable);
    }

    public RuntimeState withPerformanceStability(boolean stable, long stableSinceMillis) {
        long nextStableSince = stable ? stableSinceMillis : 0L;
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                actionHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings,
                currentSettings,
                capabilityHistory,
                nextStableSince,
                stable);
    }

    public long getLastCapabilityChangeMillis(CapabilityId capabilityId) {
        if (capabilityId == null || capabilityHistory == null) {
            return 0L;
        }
        List<CapabilityChangeEntry> history = capabilityHistory.get(capabilityId);
        if (history == null || history.isEmpty()) {
            return 0L;
        }
        return history.get(history.size() - 1).timestampMillis();
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
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                updatedHistory,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, p99FrametimeMs, frametimeStddevMs, tickTimeAvg, tickTimeP95,
                spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence,
                baselineSettings, currentSettings,
                capabilityHistory, performanceStableSince, performanceStable);
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
