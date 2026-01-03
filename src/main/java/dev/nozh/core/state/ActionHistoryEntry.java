package dev.nozh.core.state;

import dev.nozh.core.bus.CommandLifecycle;

/**
 * Immutable action history entry for HUD and diagnostics.
 */
public record ActionHistoryEntry(
        long timestampMillis,
        String actionSummary,
        CommandLifecycle outcome
) {
}
