package dev.nozh.core.state;

import dev.nozh.NozhConstants;
import dev.nozh.core.governor.ActionOutcome;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of state version migrators.
 * 
 * Contract 1: Explicit migrations, never silent.
 * Migrators chain: v1 → v2 → v3 → ... → current
 * 
 * Example:
 * - Version 1 (initial): RuntimeState with basic fields
 * - Version 2 (adds benchmarkRunning): Migrator adds benchmarkRunning=false
 * - Version 3 (adds confidence tracking): Migrator adds confidence fields
 * - Version 4 (adds pending suggestion): Migrator adds suggestion field
 * - Version 5 (adds baseline/current settings): Migrator adds settings maps
 * - Version 6 (adds suggested action queue): Migrator adds suggestion list
 * - Version 7 (adds scenario change metrics): Migrator adds scenario counters
 * - Version 8 (adds outcome traceability): Migrator adds outcome tracking
 * fields
 * - Version 9 (adds scenario history): Migrator adds scenario history list
 * - Version 10 (adds stability stats): Migrator adds stability tracking fields
 */
public final class StateMigrationRegistry {

    private static final int CURRENT_VERSION = 11; // Current state version

    private final Map<Integer, StateMigrator> migrators = new HashMap<>();

    public StateMigrationRegistry() {
        registerMigrator(2, oldState -> {
            return new RuntimeState(
                    oldState.enabled(),
                    oldState.safeMode(),
                    oldState.autoTuning(),
                    oldState.debugLogs(),
                    false, // governorDisabled default
                    false, // governorCooldownActive default
                    0L, // governorLastActionTimestamp default
                    false, // benchmarkRunning default
                    "NONE", // benchmarkValidity default
                    0L, // benchmarkStartTimestamp default
                    Optional.empty(), // pendingAction default
                    java.util.List.of(), // suggestedActions default
                    0, // pendingActionsCount default
                    oldState.executionHistorySize(),
                    oldState.lastSnapshotHistorySize(),
                    java.util.List.of(),
                    oldState.sessionChangesCount(),
                    oldState.avgFrametimeMs(),
                    oldState.p95FrametimeMs(),
                    -1.0,
                    -1.0,
                    oldState.tickTimeAvg(),
                    oldState.tickTimeP95(),
                    oldState.spikeCount(),
                    resolveStabilityStats(oldState),
                    "", 0L, // lastDecisionReason, lastDecisionTimestamp default
                    0.0,
                    ActionOutcome.NEUTRAL,
                    true,
                    oldState.sessionStartTime(),
                    11, // Target version is 11
                    dev.nozh.core.context.Scenario.STANDARD,
                    0.5,
                    0, // visibleEntityCount
                    0L,
                    0,
                    0,
                    0,
                    java.util.List.of(),
                    java.util.Map.of(),
                    java.util.Map.of());
        });

        registerMigrator(3, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                java.util.List.of(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                java.util.List.of(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                -1.0,
                -1.0,
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                0.0,
                ActionOutcome.NEUTRAL,
                true,
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                0L,
                0,
                0,
                0,
                java.util.List.of(),
                java.util.Map.of(),
                java.util.Map.of()));

        registerMigrator(4, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                oldState.suggestedActions(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.actionHistory(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.p99FrametimeMs(),
                oldState.frametimeStddevMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                0.0,
                ActionOutcome.NEUTRAL,
                true,
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                0L,
                0,
                0,
                0,
                java.util.List.of(),
                java.util.Map.of(),
                java.util.Map.of()));

        registerMigrator(5, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                oldState.suggestedActions(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.actionHistory(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.p99FrametimeMs(),
                oldState.frametimeStddevMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                0.0,
                ActionOutcome.NEUTRAL,
                true,
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                0L,
                0,
                0,
                0,
                java.util.List.of(),
                oldState.baselineSettings(),
                oldState.currentSettings()));

        registerMigrator(6, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                oldState.suggestedActions(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.actionHistory(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.p99FrametimeMs(),
                oldState.frametimeStddevMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                0.0,
                ActionOutcome.NEUTRAL,
                true,
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                0L,
                0,
                0,
                0,
                java.util.List.of(),
                oldState.baselineSettings(),
                oldState.currentSettings()));

        registerMigrator(7, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                oldState.suggestedActions(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.actionHistory(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.p99FrametimeMs(),
                oldState.frametimeStddevMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                0.0,
                ActionOutcome.NEUTRAL,
                true,
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                oldState.lastScenarioChangeTimestamp(),
                oldState.scenarioChangeCount(),
                oldState.rapidScenarioChangeCount(),
                oldState.combatAfkFlipCount(),
                oldState.scenarioHistory(),
                oldState.baselineSettings(),
                oldState.currentSettings()));

        registerMigrator(8, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                oldState.suggestedActions(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.actionHistory(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.p99FrametimeMs(),
                oldState.frametimeStddevMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                oldState.lastImpactMs(),
                oldState.lastOutcome(),
                oldState.lastDecisionAccepted(),
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                oldState.lastScenarioChangeTimestamp(),
                oldState.scenarioChangeCount(),
                oldState.rapidScenarioChangeCount(),
                oldState.combatAfkFlipCount(),
                oldState.scenarioHistory(),
                oldState.baselineSettings(),
                oldState.currentSettings()));

        registerMigrator(9, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                oldState.suggestedActions(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.actionHistory(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.p99FrametimeMs(),
                oldState.frametimeStddevMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                oldState.lastImpactMs(),
                oldState.lastOutcome(),
                oldState.lastDecisionAccepted(),
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                oldState.lastScenarioChangeTimestamp(),
                oldState.scenarioChangeCount(),
                oldState.rapidScenarioChangeCount(),
                oldState.combatAfkFlipCount(),
                oldState.scenarioHistory(),
                oldState.baselineSettings(),
                oldState.currentSettings()));

        registerMigrator(10, oldState -> new RuntimeState(
                oldState.enabled(),
                oldState.safeMode(),
                oldState.autoTuning(),
                oldState.debugLogs(),
                oldState.governorDisabled(),
                oldState.governorCooldownActive(),
                oldState.governorLastActionTimestamp(),
                oldState.benchmarkRunning(),
                oldState.benchmarkValidity(),
                oldState.benchmarkStartTimestamp(),
                oldState.pendingAction(),
                oldState.suggestedActions(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.actionHistory(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.p99FrametimeMs(),
                oldState.frametimeStddevMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                resolveStabilityStats(oldState),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                oldState.lastImpactMs(),
                oldState.lastOutcome(),
                oldState.lastDecisionAccepted(),
                oldState.sessionStartTime(),
                11,
                oldState.currentScenario(),
                oldState.scenarioConfidence(),
                0, // visibleEntityCount
                oldState.lastScenarioChangeTimestamp(),
                oldState.scenarioChangeCount(),
                oldState.rapidScenarioChangeCount(),
                oldState.combatAfkFlipCount(),
                oldState.scenarioHistory(),
                oldState.baselineSettings(),
                oldState.currentSettings()));

        NozhConstants.LOGGER.debug("StateMigrationRegistry initialized (current version: {})", CURRENT_VERSION);
    }

    /**
     * Register a migrator for a specific version transition.
     * 
     * @param fromVersion Source version
     * @param migrator    Migration function
     */
    public void registerMigrator(int fromVersion, StateMigrator migrator) {
        if (migrators.containsKey(fromVersion)) {
            NozhConstants.LOGGER.warn("Overwriting migrator for version {}", fromVersion);
        }
        migrators.put(fromVersion, migrator);
    }

    /**
     * Migrate state from old version to current version.
     * 
     * @param state State to migrate (any version)
     * @return Migrated state at current version
     * @throws StateMigrationException if migration chain fails
     */
    public RuntimeState migrate(RuntimeState state) {
        int currentVersion = state.stateVersion();

        if (currentVersion == CURRENT_VERSION) {
            // Already at current version, no migration needed
            return state;
        }

        if (currentVersion > CURRENT_VERSION) {
            throw new StateMigrationException(
                    "State version " + currentVersion + " is newer than current " + CURRENT_VERSION +
                            " (downgrade not supported)");
        }

        // Chain migrations: v1 → v2 → v3 → ... → current
        RuntimeState migrated = state;
        for (int version = currentVersion; version < CURRENT_VERSION; version++) {
            StateMigrator migrator = migrators.get(version);
            if (migrator == null) {
                throw new StateMigrationException(
                        "No migrator registered for version " + version + " → " + (version + 1));
            }

            NozhConstants.LOGGER.info("Migrating state: v{} → v{}", version, version + 1);
            migrated = migrator.migrate(migrated);
        }

        return migrated;
    }

    /**
     * Get current state version.
     */
    public static int getCurrentVersion() {
        return CURRENT_VERSION;
    }

    private static StabilityStats resolveStabilityStats(RuntimeState state) {
        if (state == null || state.stabilityStats() == null) {
            return StabilityStats.defaults();
        }
        return state.stabilityStats();
    }
}
