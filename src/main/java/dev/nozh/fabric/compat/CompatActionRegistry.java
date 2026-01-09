package dev.nozh.fabric.compat;

import dev.nozh.core.capability.CapabilityId;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public final class CompatActionRegistry {

    private final Map<String, EnumSet<CapabilityId>> permitted = new HashMap<>();

    public void register(String modId, EnumSet<CapabilityId> actions) {
        permitted.put(modId, EnumSet.copyOf(actions));
    }

    public EnumSet<CapabilityId> permittedActions(String modId) {
        return permitted.getOrDefault(modId, EnumSet.noneOf(CapabilityId.class));
    }

    public boolean isPermitted(String modId, CapabilityId capability) {
        return permittedActions(modId).contains(capability);
    }
}
