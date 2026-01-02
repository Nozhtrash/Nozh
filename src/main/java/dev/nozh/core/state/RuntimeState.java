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

import dev.nozh.core.governor.GovernorMode;
import dev.nozh.core.issues.ParanoiaLevel;

import java.util.ArrayList;
import java.util.List;

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
        int pendingActionsCount,
        int executionHistorySize,
        int lastSnapshotHistorySize,
        int sessionChangesCount,
        double avgFrametimeMs,
        double p95FrametimeMs,
        int spikeCount,
        long sessionStartTime,
        int stateVersion,
        dev.nozh.core.context.Scenario currentScenario,
        GovernorMode governorMode,
        ParanoiaLevel paranoiaLevel,
        String currentBound,
        String lastDecisionReason,
        long lastDecisionTimestamp,
        String lastDecisionSteward,
        List<ActionHistoryEntry> recentActions) {
    private static final int CURRENT_VERSION = 3; // Bump version

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
                0, // pendingActionsCount
                0, // executionHistorySize
                0, // lastSnapshotHistorySize
                0, // sessionChangesCount
                -1.0, // avgFrametimeMs (sentinel)
                -1.0, // p95FrametimeMs (sentinel)
                0, // spikeCount
                System.currentTimeMillis(), // sessionStartTime
                CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD,
                GovernorMode.MANUAL_ASSIST,
                ParanoiaLevel.NORMAL,
                "BALANCED",
                "",
                0L,
                "NOZH",
                List.of());
    }

    /**
     * Update after governor action (immutable).
     */
    public RuntimeState withGovernorAction(long timestamp) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                true, // governorCooldownActive = true after action
                timestamp, // governorLastActionTimestamp
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingActionsCount,
                executionHistorySize + 1, // increment history
                lastSnapshotHistorySize,
                sessionChangesCount + 1, // increment changes
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario,
                governorMode,
                paranoiaLevel,
                currentBound,
                lastDecisionReason,
                lastDecisionTimestamp,
                lastDecisionSteward,
                recentActions);
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
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario,
                governorMode,
                paranoiaLevel,
                currentBound,
                lastDecisionReason,
                lastDecisionTimestamp,
                lastDecisionSteward,
                recentActions);
    }

    /**
     * Update telemetry metrics (immutable).
     */
    public RuntimeState withTelemetry(double avg, double p95, int spikes) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avg, p95, spikes,
                sessionStartTime, stateVersion,
                currentScenario,
                governorMode,
                paranoiaLevel,
                currentBound,
                lastDecisionReason,
                lastDecisionTimestamp,
                lastDecisionSteward,
                recentActions);
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
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario,
                governorMode,
                paranoiaLevel,
                currentBound,
                lastDecisionReason,
                lastDecisionTimestamp,
                lastDecisionSteward,
                recentActions);
    }

    /**
     * Update current scenario (immutable).
     */
    public RuntimeState withScenario(dev.nozh.core.context.Scenario scenario) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                scenario,
                governorMode,
                paranoiaLevel,
                currentBound,
                lastDecisionReason,
                lastDecisionTimestamp,
                lastDecisionSteward,
                recentActions);
    }

    /**
     * Update governor snapshot info (immutable).
     */
    public RuntimeState withGovernorSnapshot(GovernorMode mode, String bound) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario,
                mode,
                paranoiaLevel,
                bound,
                lastDecisionReason,
                lastDecisionTimestamp,
                lastDecisionSteward,
                recentActions);
    }

    /**
     * Update last decision summary (immutable).
     */
    public RuntimeState withDecision(String reasonKey, long timestampMillis, String steward) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario,
                governorMode,
                paranoiaLevel,
                currentBound,
                reasonKey,
                timestampMillis,
                steward,
                recentActions);
    }

    /**
     * Add an action history entry (immutable).
     */
    public RuntimeState withRecentAction(ActionHistoryEntry entry, int maxEntries) {
        List<ActionHistoryEntry> updated = new ArrayList<>(recentActions);
        updated.add(entry);

        if (updated.size() > maxEntries) {
            updated = new ArrayList<>(updated.subList(updated.size() - maxEntries, updated.size()));
        }

        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario,
                governorMode,
                paranoiaLevel,
                currentBound,
                lastDecisionReason,
                lastDecisionTimestamp,
                lastDecisionSteward,
                List.copyOf(updated));
    }

    /**
     * Create state from config (initialization).
     */
    public static RuntimeState fromConfig(dev.nozh.core.config.NozhConfig config) {
        GovernorMode mode;
        if (!config.enabled) {
            mode = GovernorMode.OFF;
        } else if (!config.allowAutoTuning) {
            mode = GovernorMode.MANUAL_ASSIST;
        } else {
            mode = GovernorMode.AUTO_CONSERVATIVE;
        }

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
                0, // pendingActionsCount
                0, // executionHistorySize
                0, // lastSnapshotHistorySize
                0, // sessionChangesCount
                -1.0, // avgFrametimeMs
                -1.0, // p95FrametimeMs
                0, // spikeCount
                System.currentTimeMillis(),
                CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD,
                mode,
                ParanoiaLevel.NORMAL,
                "BALANCED",
                "",
                0L,
                "NOZH",
                List.of());
    }
}
