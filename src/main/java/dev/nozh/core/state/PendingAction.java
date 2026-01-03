package dev.nozh.core.state;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.bus.Command;

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
    /**
     * Legacy compatibility constructor for older call sites that did not pass the command.
     * Scheduled for removal in a cleanup pass.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public PendingAction(
            long timestampMillis,
            CapabilityId capability,
            Optional<CapabilityValue> previousValue,
            CapabilityValue newValue,
            double baselineAvgMs,
            double baselineP95Ms) {
        this(
                timestampMillis,
                capability,
                new Command.ApplyCapability(capability, newValue),
                previousValue,
                newValue,
                baselineAvgMs,
                baselineP95Ms);
    }

    /**
     * Legacy compatibility alias for older call sites.
     * Scheduled for removal in a cleanup pass.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public Command actionCommand() {
        return command;
    }
}
