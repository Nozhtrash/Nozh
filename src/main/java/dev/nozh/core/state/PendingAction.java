package dev.nozh.core.state;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.bus.Command;
import dev.nozh.core.context.Scenario;

import java.util.Optional;

/**
 * Pending action evaluation snapshot.
 *
 * Captures baseline telemetry and capability change metadata for rollback checks.
 */
public record PendingAction(
        long timestampMillis,
        long appliedTick,
        CapabilityId capability,
        dev.nozh.core.bus.Command command,
        Optional<CapabilityValue> previousValue,
        CapabilityValue newValue,
        double baselineAvgMs,
        double baselineP95Ms,
        Scenario scenario,
        double scenarioConfidence,
        PerfSnapshot baselineSnapshot) {
}
