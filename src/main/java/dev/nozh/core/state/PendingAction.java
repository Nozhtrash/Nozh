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
     * Compatibility constructor for older call sites that did not pass the command.
     */
    @Deprecated
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
     * Compatibility alias for older call sites.
     */
    @Deprecated
    public Command actionCommand() {
        return command;
    }
}
