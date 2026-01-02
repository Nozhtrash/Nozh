package dev.nozh.core.state;

import dev.nozh.NozhConstants;

import java.util.HashMap;
import java.util.Map;

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
 */
public final class StateMigrationRegistry {

    private static final int CURRENT_VERSION = 3; // Current state version

    private final Map<Integer, StateMigrator> migrators = new HashMap<>();

    public StateMigrationRegistry() {
        // Register migrators here as versions evolve
        // Example (when v2 comes):
        // registerMigrator(1, oldState -> {
        // // Migrate v1 -> v2
        // return new RuntimeState(
        // oldState.enabled(),
        // ... existing fields ...
        // false, // NEW: benchmarkRunning
        // 2 // NEW: stateVersion
        // );
        // });

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
