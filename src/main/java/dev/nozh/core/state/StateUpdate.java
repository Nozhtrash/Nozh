package dev.nozh.core.state;

/**
 * Functional interface for state updates.
 * 
 * Contract 1: Updates are transactional (atomic).
 * An update receives current state and returns new state.
 * The StateStore validates the new state before applying.
 */
@FunctionalInterface
public interface StateUpdate {

    /**
     * Apply update to current state.
     *
     * @param current Current runtime state
     * @return New runtime state (MUST be valid)
     */
    RuntimeState apply(RuntimeState current);
}
