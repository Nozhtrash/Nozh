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

import java.util.Optional;

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
        Optional<PendingAction> pendingAction,
        Optional<PendingAction> suggestedAction,
        int pendingActionsCount,
        int executionHistorySize,
        int lastSnapshotHistorySize,
        int sessionChangesCount,
        double avgFrametimeMs,
        double p95FrametimeMs,
        double tickTimeAvg,
        double tickTimeP95,
        int spikeCount,
        String lastDecisionReason,
        long lastDecisionTimestamp,
        long sessionStartTime,
        int stateVersion,
        dev.nozh.core.context.Scenario currentScenario,
        double scenarioConfidence) {
    private static final int CURRENT_VERSION = 4; // Bump version

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
                0, // sessionChangesCount
                -1.0, // avgFrametimeMs
                -1.0, // p95FrametimeMs
                -1.0, // tickTimeAvg
                -1.0, // tickTimeP95
                0, // spikeCount
                "", 0L, // lastDecisionReason, lastDecisionTimestamp
                System.currentTimeMillis(), CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD, 0.5);
    }

    /**
     * Update after governor action (immutable).
     */
    public RuntimeState withGovernorAction(long timestamp, PendingAction pending) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled,
                true, // governorCooldownActive = true after action
                timestamp, // governorLastActionTimestamp
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                Optional.of(pending),
                suggestedAction,
                pendingAction.isPresent() ? pendingActionsCount + 1 : 1,
                executionHistorySize + 1, // increment history
                lastSnapshotHistorySize,
                sessionChangesCount + 1, // increment changes
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
    }

    public RuntimeState withAppliedSuggestion(long timestamp, PendingAction pending) {
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
                sessionChangesCount + 1,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
    }

    /**
     * Update telemetry metrics (immutable).
     */
    public RuntimeState withTelemetry(double avg, double p95, int spikes, double tickAvg, double tickP95) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction, suggestedAction, pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avg, p95, tickAvg, tickP95, spikes,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                scenario, confidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                reason != null ? reason : "",
                timestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
    }

    public RuntimeState withPendingSuggestion(PendingAction suggestion) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                Optional.of(suggestion),
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
    }

    public RuntimeState withPendingSuggestionCleared() {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkValidity, benchmarkStartTimestamp,
                pendingAction,
                Optional.empty(),
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, tickTimeAvg, tickTimeP95, spikeCount,
                lastDecisionReason, lastDecisionTimestamp,
                sessionStartTime, stateVersion,
                currentScenario, scenarioConfidence);
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
                0, // sessionChangesCount
                -1.0, // avgFrametimeMs
                -1.0, // p95FrametimeMs
                -1.0, // tickTimeAvg
                -1.0, // tickTimeP95
                0, // spikeCount
                "", 0L, // lastDecisionReason, lastDecisionTimestamp
                System.currentTimeMillis(),
                CURRENT_VERSION,
                dev.nozh.core.context.Scenario.STANDARD,
                0.5);
    }
}
