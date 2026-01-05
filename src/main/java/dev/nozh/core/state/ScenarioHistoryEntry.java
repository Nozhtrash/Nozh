package dev.nozh.core.state;

import dev.nozh.core.context.Scenario;

/**
 * Scenario history entry for smoothing context changes.
 */
public record ScenarioHistoryEntry(
        long timestampMillis,
        Scenario scenario,
        double confidence) {
    public ScenarioHistoryEntry {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario cannot be null");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Scenario confidence must be between 0.0 and 1.0");
        }
    }
}
