package dev.nozh.core.compatibility;

import dev.nozh.api.compat.StewardshipMode;
import dev.nozh.core.bus.CapabilityId;

public record StewardshipDecision(
        CapabilityId capability,
        String steward,
        StewardshipMode mode,
        String reason) {
}
