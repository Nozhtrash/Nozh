package dev.nozh.core.compatibility;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.capability.ProviderMetadata;
import dev.nozh.fabric.compat.CompatRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Set;

/**
 * Compatibility matrix for external mod stewardship.
 */
public final class CompatibilityMatrix {

    private final ModConflictDetector conflictDetector;
    private final CompatRegistry compatRegistry;

    public CompatibilityMatrix() {
        this.conflictDetector = new ModConflictDetector();
        this.compatRegistry = new CompatRegistry();
    }

    public boolean isExternallyManaged(CapabilityId capability, ProviderMetadata metadata) {
        if (conflictDetector.isNozhSpecialty(capability)) {
            return false;
        }
        if (metadata != null && !metadata.conflictingMods().isEmpty()) {
            for (String mod : metadata.conflictingMods()) {
                if (isModLoaded(mod)) {
                    return true;
                }
            }
        }
        if (compatRegistry.isExternallyManaged(capability)) {
            return true;
        }
        return conflictDetector.hasConflict(capability);
    }

    public String getSteward(CapabilityId capability) {
        String steward = compatRegistry.getSteward(capability);
        if (steward != null) {
            return steward;
        }
        return conflictDetector.getSteward(capability);
    }

    public boolean isBlockedByDependencies(ProviderMetadata metadata) {
        if (metadata == null) {
            return false;
        }
        Set<String> required = metadata.requiredMods();
        if (required == null || required.isEmpty()) {
            return false;
        }
        for (String mod : required) {
            if (!isModLoaded(mod)) {
                return true;
            }
        }
        return false;
    }

    public String getDependencySteward(ProviderMetadata metadata) {
        if (metadata == null || metadata.requiredMods().isEmpty()) {
            return "External Mod";
        }
        return String.join(", ", metadata.requiredMods());
    }

    private boolean isModLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }
}
