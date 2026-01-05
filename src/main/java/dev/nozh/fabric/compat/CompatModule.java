package dev.nozh.fabric.compat;

import dev.nozh.core.bus.CapabilityId;

import java.util.EnumSet;

public record CompatModule(String modId, String displayName, EnumSet<CapabilityId> managedCapabilities) {
    public boolean manages(CapabilityId capability) {
        return managedCapabilities.contains(capability);
    }
}
