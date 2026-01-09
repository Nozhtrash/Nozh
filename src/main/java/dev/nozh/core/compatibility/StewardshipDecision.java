package dev.nozh.core.compatibility;

import dev.nozh.api.compat.StewardshipMode;
import dev.nozh.core.capability.CapabilityId;

public record StewardshipDecision(
        CapabilityId capability,
        String steward,
        StewardshipMode mode,
        String reason) {
}
