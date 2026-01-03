/**
 * NOZH - Adaptive Performance Optimization
 * Copyright (c) 2025 NOZH Project
 * 
 * Licensed under the MIT License.
 * 
 * This file defines a CORE ARCHITECTURAL CONTRACT.
 * Changes here affect system-wide invariants.
 * 
 * Read docs/v0.2-alpha.md before modifying.
 */
package dev.nozh.core.state;

import dev.nozh.NozhConstants;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central state store for NOZH (Contract 1: StateStore Purity).
 * 
 * WHY THIS EXISTS:
 * StateStore is the single source of truth for runtime state. It enforces
 * transactional updates (read → validate → commit) to prevent corrupt state
 * from reaching the system. This is critical because invalid state can cause:
 * - Governor making decisions on stale data
 * - HUD displaying inconsistent metrics
 * - Provider rollbacks failing silently
 * 
 * RULES (NON-NEGOTIABLE):
 * 1. ZERO Minecraft dependencies (pure model)
 * 2. Thread-safe snapshot reads (read lock for concurrent access)
 * 3. Transactional updates (validated before applying)
 * 4. Dirty flag optimization (only persist on actual change)
 * 
 * WHY READ LOCKS:
 * State is accessed from multiple threads: MC main thread (updates),
 * governor tick (decisions), HUD render thread (display). Without locks,
 * snapshots could read torn/partial state during updates. Read locks allow
 * concurrent reads (cheap) while blocking during writes (rare).
 */
public final class StateStore {

    private static final StateStore INSTANCE = new StateStore();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final StateMigrationRegistry migrationRegistry = new StateMigrationRegistry();

    private RuntimeState currentState;
    private int lastPersistedHash; // For dirty flag optimization

    private StateStore() {
        // Initialize from config to respect user preferences
        this.currentState = RuntimeState.fromConfig(dev.nozh.core.config.ConfigManager.getConfig());
        this.lastPersistedHash = currentState.hashCode();
        NozhConstants.LOGGER.info("StateStore initialized from config (version {})",
                currentState.stateVersion());
        dev.nozh.core.config.ConfigManager.addListener(config -> update(state -> state.withConfig(config)));
    }

    /**
     * Get singleton instance.
     */
    public static StateStore getInstance() {
        return INSTANCE;
    }

    /**
     * Get thread-safe snapshot of current state.
     * 
     * NO locks on read (volatile read + immutable record = safe).
     * 
     * @return Current runtime state (immutable)
     */
    public RuntimeState snapshot() {
        lock.readLock().lock();
        try {
            return currentState;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get snapshot with fallback (never throws, for HUD Contract 7).
     * 
     * @return Current state, or defaults if corrupted
     */
    public RuntimeState snapshotSafe() {
        try {
            return snapshot();
        } catch (Exception e) {
            NozhConstants.LOGGER.error("StateStore snapshot failed, returning defaults", e);
            return RuntimeState.defaults();
        }
    }

    /**
     * Apply transactional update to state (Contract 1).
     * 
     * Process:
     * 1. Acquire write lock
     * 2. Apply update function
     * 3. Validate new state (invariants)
     * 4. If valid, commit; if invalid, reject
     * 5. Release lock
     * 
     * @param update Update function
     * @throws StateInvariantViolationException if new state violates invariants
     */
    public void update(StateUpdate update) {
        Objects.requireNonNull(update, "StateUpdate cannot be null");

        lock.writeLock().lock();
        try {
            // Apply update
            RuntimeState newState = update.apply(currentState);

            // Validate invariants (Contract 1)
            ValidationResult validation = StateInvariantValidator.validate(newState);

            if (validation.isInvalid()) {
                ValidationResult.Invalid invalid = (ValidationResult.Invalid) validation;
                throw new StateInvariantViolationException(
                        "State update rejected: " + invalid.formatViolations());
            }

            // Commit
            RuntimeState oldState = currentState;
            currentState = newState;

            if (NozhConstants.LOGGER.isDebugEnabled()) {
                NozhConstants.LOGGER.debug("State updated successfully");
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Replace entire state (used for loading from disk).
     * 
     * @param state New state (will be validated + migrated if needed)
     */
    public void replaceState(RuntimeState state) {
        Objects.requireNonNull(state, "RuntimeState cannot be null");

        lock.writeLock().lock();
        try {
            // Migrate if needed
            RuntimeState migrated = migrationRegistry.migrate(state);

            // Validate
            StateInvariantValidator.validateOrThrow(migrated);

            // Replace
            currentState = migrated;
            lastPersistedHash = migrated.hashCode();

            NozhConstants.LOGGER.info("State replaced (version {})", migrated.stateVersion());

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if state has changed since last persist (dirty flag).
     * 
     * @return true if state changed, false if unchanged
     */
    public boolean isDirty() {
        lock.readLock().lock();
        try {
            return currentState.hashCode() != lastPersistedHash;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Mark state as persisted (update cached hash).
     */
    public void markPersisted() {
        lock.writeLock().lock();
        try {
            lastPersistedHash = currentState.hashCode();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reset to default state (used for testing or emergency reset).
     */
    public void reset() {
        lock.writeLock().lock();
        try {
            currentState = RuntimeState.defaults();
            lastPersistedHash = currentState.hashCode();
            NozhConstants.LOGGER.warn("StateStore reset to defaults");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
