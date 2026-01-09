package dev.nozh.core.preset;

import dev.nozh.core.capability.CapabilityId;

import java.util.Set;

/**
 * Preset constraints record (Contract 10).
 * 
 * Defines LIMITS for a hardware tier, NOT actions.
 * Governor operates within these bounds.
 */
public record PresetConstraints(
        int maxRenderDistance,
        boolean shadersAllowed,
        int maxFpsCap,
        Set<CapabilityId> allowedCapabilities) {
    /**
     * Check if a capability is allowed by this preset.
     */
    public boolean allows(CapabilityId capability) {
        return allowedCapabilities.contains(capability);
    }
}
