package dev.nozh.core.state;

import dev.nozh.NozhConstants;

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
 */
public final class StateMigrationRegistry {

    private static final int CURRENT_VERSION = 4; // Current state version

    private final Map<Integer, StateMigrator> migrators = new HashMap<>();

    public StateMigrationRegistry() {
        registerMigrator(2, oldState -> {
            String validity = oldState.benchmarkValidity() != null ? oldState.benchmarkValidity() : "NONE";
            // The original migrator for version 2 was adding benchmarkRunning=false and
            // handling validity.
            // This new version 2 migrator needs to provide defaults for all fields
            // introduced up to version 3
            // that are not present in version 1 or 2, and ensure the constructor matches
            // the latest RuntimeState.
            // The instruction implies a migration from an older state (likely v1 or v2) to
            // a v3-compatible state.
            // The original code was migrating from v1 to v2, adding benchmarkRunning and
            // handling validity.
            // The instruction provides a full list of parameters for a RuntimeState
            // constructor that has 26 fields.
            // This means the migrator for version 2 should now produce a state compatible
            // with the *current* RuntimeState
            // constructor, filling in defaults for fields that didn't exist in the oldState
            // (which is effectively v2 here).

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
                    Optional.empty(), // suggestedAction default
                    0, // pendingActionsCount default
                    oldState.executionHistorySize(),
                    oldState.lastSnapshotHistorySize(),
                    oldState.sessionChangesCount(),
                    oldState.avgFrametimeMs(),
                    oldState.p95FrametimeMs(),
                    oldState.tickTimeAvg(),
                    oldState.tickTimeP95(),
                    oldState.spikeCount(),
                    "", 0L, // lastDecisionReason, lastDecisionTimestamp default
                    oldState.sessionStartTime(),
                    4, // Target version is 4
                    dev.nozh.core.context.Scenario.STANDARD,
                    0.5);
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
                Optional.empty(),
                oldState.pendingActionsCount(),
                oldState.executionHistorySize(),
                oldState.lastSnapshotHistorySize(),
                oldState.sessionChangesCount(),
                oldState.avgFrametimeMs(),
                oldState.p95FrametimeMs(),
                oldState.tickTimeAvg(),
                oldState.tickTimeP95(),
                oldState.spikeCount(),
                oldState.lastDecisionReason(),
                oldState.lastDecisionTimestamp(),
                oldState.sessionStartTime(),
                4,
                oldState.currentScenario(),
                oldState.scenarioConfidence()));

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
}
