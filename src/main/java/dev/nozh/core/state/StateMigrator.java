package dev.nozh.core.state;

/**
 * Interface for state migrations from one version to another.
 * 
 * Contract 1: Migrations are explicit, never silent.
 */
@FunctionalInterface
public interface StateMigrator {

    /**
     * Migrate state from previous version to next version.
     * 
     * @param oldState State from previous version
     * @return Migrated state for current version
     * @throws StateMigrationException if migration fails
     */
    RuntimeState migrate(RuntimeState oldState);
}
