package dev.nozh.fabric.compat;

import dev.nozh.core.bus.CapabilityId;

import java.util.EnumSet;

public record CompatModule(String modId, String displayName, EnumSet<CapabilityId> managedCapabilities,
        EnumSet<CapabilityId> permittedActions) {
    public boolean manages(CapabilityId capability) {
        return managedCapabilities.contains(capability);
    }

    public boolean permits(CapabilityId capability) {
        return permittedActions.contains(capability);
    }
}
