package dev.nozh.fabric.compat;

import dev.nozh.core.bus.CapabilityId;
import net.fabricmc.loader.api.FabricLoader;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Fabric compatibility registry for external mod stewardship.
 */
public final class CompatRegistry {

    private final Map<String, CompatModule> modules = new HashMap<>();

    public CompatRegistry() {
        register(new CompatModule("sodium", "Sodium",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.VSYNC, CapabilityId.FOG, CapabilityId.BIOME_BLEND,
                        CapabilityId.ENTITY_DISTANCE, CapabilityId.RENDER_DISTANCE, CapabilityId.MIPMAP_LEVEL)));
        register(new CompatModule("sodium-extra", "Sodium Extra",
                EnumSet.of(CapabilityId.PARTICLES, CapabilityId.FOG)));
        register(new CompatModule("iris", "Iris",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.FOG, CapabilityId.SMOOTH_LIGHTING)));
        register(new CompatModule("lithium", "Lithium",
                EnumSet.of(CapabilityId.SIMULATION_DISTANCE)));
        register(new CompatModule("entityculling", "Entity Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE)));
        register(new CompatModule("moreculling", "More Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE)));
        register(new CompatModule("bobby", "Bobby",
                EnumSet.of(CapabilityId.RENDER_DISTANCE)));
        register(new CompatModule("vulkanmod", "VulkanMod",
                EnumSet.of(CapabilityId.RENDER_DISTANCE, CapabilityId.VSYNC, CapabilityId.CLOUDS)));
        register(new CompatModule("canvas", "Canvas",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.VSYNC, CapabilityId.PARTICLES)));
        register(new CompatModule("lambdynlights", "LambDynamicLights",
                EnumSet.of(CapabilityId.DYNAMIC_LIGHTING)));
    }

    private void register(CompatModule module) {
        modules.put(module.modId(), module);
    }

    public boolean isExternallyManaged(CapabilityId capability) {
        return modules.values().stream()
                .anyMatch(module -> module.manages(capability) && isLoaded(module.modId()));
    }

    public String getSteward(CapabilityId capability) {
        return modules.values().stream()
                .filter(module -> module.manages(capability) && isLoaded(module.modId()))
                .map(CompatModule::displayName)
                .findFirst()
                .orElse(null);
    }

    private boolean isLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }
}
