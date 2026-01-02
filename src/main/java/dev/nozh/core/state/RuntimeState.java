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
        dev.nozh.core.context.Scenario currentScenario) {
    private static final int CURRENT_VERSION = 2; // Bump version

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
                dev.nozh.core.context.Scenario.STANDARD);
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
                benchmarkRunning, benchmarkStartTimestamp,
                pendingActionsCount,
                executionHistorySize + 1, // increment history
                lastSnapshotHistorySize,
                sessionChangesCount + 1, // increment changes
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario);
    }

    /**
     * Update benchmark status (immutable).
     */
    public RuntimeState withBenchmarkStatus(boolean running, long startTime) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                running ? true : governorDisabled, // disable governor during benchmark
                governorCooldownActive, governorLastActionTimestamp,
                running, startTime,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario);
    }

    /**
     * Update telemetry metrics (immutable).
     */
    public RuntimeState withTelemetry(double avg, double p95, int spikes) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avg, p95, spikes,
                sessionStartTime, stateVersion,
                currentScenario);
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
                benchmarkRunning, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                currentScenario);
    }

    /**
     * Update current scenario (immutable).
     */
    public RuntimeState withScenario(dev.nozh.core.context.Scenario scenario) {
        return new RuntimeState(
                enabled, safeMode, autoTuning, debugLogs,
                governorDisabled, governorCooldownActive, governorLastActionTimestamp,
                benchmarkRunning, benchmarkStartTimestamp,
                pendingActionsCount, executionHistorySize, lastSnapshotHistorySize,
                sessionChangesCount,
                avgFrametimeMs, p95FrametimeMs, spikeCount,
                sessionStartTime, stateVersion,
                scenario);
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
                dev.nozh.core.context.Scenario.STANDARD);
    }
}
