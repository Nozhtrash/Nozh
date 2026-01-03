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
                        CapabilityId.ENTITY_DISTANCE, CapabilityId.RENDER_DISTANCE, CapabilityId.MIPMAP_LEVEL),
                0.4,
                "Sodium active: rendering options managed externally"));
        register(new CompatModule("sodium-extra", "Sodium Extra",
                EnumSet.of(CapabilityId.PARTICLES, CapabilityId.FOG),
                0.5,
                "Sodium Extra active: particle/fog controls managed externally"));
        register(new CompatModule("iris", "Iris",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.FOG, CapabilityId.SMOOTH_LIGHTING),
                0.6,
                "Iris active: shader pipeline may override visuals"));
        register(new CompatModule("lithium", "Lithium",
                EnumSet.of(CapabilityId.SIMULATION_DISTANCE),
                0.7,
                "Lithium active: simulation tuning already optimized"));
        register(new CompatModule("entityculling", "Entity Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE),
                0.6,
                "Entity Culling active: entity distance may be capped"));
        register(new CompatModule("moreculling", "More Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE),
                0.6,
                "More Culling active: entity distance may be capped"));
        register(new CompatModule("bobby", "Bobby",
                EnumSet.of(CapabilityId.RENDER_DISTANCE),
                0.3,
                "Bobby active: render distance controlled externally"));
        register(new CompatModule("vulkanmod", "VulkanMod",
                EnumSet.of(CapabilityId.RENDER_DISTANCE, CapabilityId.VSYNC, CapabilityId.CLOUDS),
                0.4,
                "VulkanMod active: rendering overrides detected"));
        register(new CompatModule("canvas", "Canvas",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.VSYNC, CapabilityId.PARTICLES),
                0.4,
                "Canvas active: renderer overrides detected"));
        register(new CompatModule("lambdynlights", "LambDynamicLights",
                EnumSet.of(CapabilityId.DYNAMIC_LIGHTING),
                0.5,
                "LambDynamicLights active: dynamic lighting managed externally"));
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

    public double getPriorityMultiplier(CapabilityId capability) {
        double multiplier = 1.0;
        for (CompatModule module : modules.values()) {
            if (module.manages(capability) && isLoaded(module.modId())) {
                multiplier = Math.min(multiplier, module.priorityMultiplier());
            }
        }
        return multiplier;
    }

    public java.util.List<String> getActiveWarnings() {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        for (CompatModule module : modules.values()) {
            if (isLoaded(module.modId()) && module.warningMessage() != null && !module.warningMessage().isBlank()) {
                warnings.add(module.warningMessage());
            }
        }
        return warnings;
    }

    private boolean isLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }
}
