package dev.nozh.core.state;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.Optional;

/**
 * Pending action evaluation snapshot.
 *
 * Captures baseline telemetry and capability change metadata for rollback checks.
 */
public record PendingAction(
        long timestampMillis,
        CapabilityId capability,
        dev.nozh.core.bus.Command command,
        Optional<CapabilityValue> previousValue,
        CapabilityValue newValue,
        double baselineAvgMs,
        double baselineP95Ms) {
}
