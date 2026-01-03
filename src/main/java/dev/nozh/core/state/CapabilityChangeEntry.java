package dev.nozh.core.state;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

/**
 * Immutable record of a capability change for restore tracking.
 */
public record CapabilityChangeEntry(
        long timestampMillis,
        CapabilityId capabilityId,
        CapabilityValue previousValue,
        CapabilityValue newValue,
        CapabilityChangeType type) {
}
