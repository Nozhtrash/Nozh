package dev.nozh.core.state;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.context.Scenario;
import dev.nozh.core.governor.ActionOutcome;

/**
 * Immutable action history entry for HUD and diagnostics.
 */
public record ActionHistoryEntry(
        long timestampMillis,
        String actionSummary,
        Scenario scenario,
        double scenarioConfidence,
        PerfSnapshot beforeSnapshot,
        PerfSnapshot afterSnapshot,
        double p95DeltaMs,
        int spikeDelta,
        int observationWindowSeconds,
        ActionOutcome outcome,
        boolean rollbackApplied
) {
}
