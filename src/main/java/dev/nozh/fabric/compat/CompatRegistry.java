package dev.nozh.fabric.compat;

import dev.nozh.core.bus.CapabilityId;
import net.fabricmc.loader.api.FabricLoader;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fabric compatibility registry for external mod stewardship.
 */
public final class CompatRegistry {

    private final Map<String, CompatModule> modules = new HashMap<>();
    private final Map<String, CompatAdapter> adapters = new HashMap<>();

    public CompatRegistry() {
        register(new CompatModule("sodium", "Sodium",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.VSYNC, CapabilityId.FOG, CapabilityId.BIOME_BLEND,
                        CapabilityId.ENTITY_DISTANCE, CapabilityId.RENDER_DISTANCE, CapabilityId.MIPMAP_LEVEL),
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.SMOOTH_LIGHTING, CapabilityId.MIPMAP_LEVEL)));
        register(new CompatModule("sodium-extra", "Sodium Extra",
                EnumSet.of(CapabilityId.PARTICLES, CapabilityId.FOG),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("iris", "Iris",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.FOG, CapabilityId.SMOOTH_LIGHTING),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("lithium", "Lithium",
                EnumSet.of(CapabilityId.SIMULATION_DISTANCE),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("entityculling", "Entity Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("moreculling", "More Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("bobby", "Bobby",
                EnumSet.of(CapabilityId.RENDER_DISTANCE),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("vulkanmod", "VulkanMod",
                EnumSet.of(CapabilityId.RENDER_DISTANCE, CapabilityId.VSYNC, CapabilityId.CLOUDS),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("canvas", "Canvas",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.VSYNC, CapabilityId.PARTICLES),
                EnumSet.noneOf(CapabilityId.class)));
        register(new CompatModule("lambdynlights", "LambDynamicLights",
                EnumSet.of(CapabilityId.DYNAMIC_LIGHTING),
                EnumSet.noneOf(CapabilityId.class)));

        registerAdapter(new SodiumOptionsAdapter());
    }

    private void register(CompatModule module) {
        modules.put(module.modId(), module);
    }

    private void registerAdapter(CompatAdapter adapter) {
        adapters.put(adapter.modId(), adapter);
    }

    public boolean isExternallyManaged(CapabilityId capability) {
        return modules.values().stream()
                .anyMatch(module -> module.manages(capability) && isLoaded(module.modId())
                        && !isActionPermitted(module, capability));
    }

    public String getSteward(CapabilityId capability) {
        return modules.values().stream()
                .filter(module -> module.manages(capability) && isLoaded(module.modId())
                        && !isActionPermitted(module, capability))
                .map(CompatModule::displayName)
                .findFirst()
                .orElse(null);
    }

    public boolean isActionPermitted(CapabilityId capability) {
        return modules.values().stream()
                .filter(module -> module.permits(capability) && isLoaded(module.modId()))
                .anyMatch(module -> isActionPermitted(module, capability));
    }

    public Optional<CompatAdapter> getAdapter(CapabilityId capability) {
        return modules.values().stream()
                .filter(module -> module.permits(capability) && isLoaded(module.modId()))
                .map(module -> adapters.get(module.modId()))
                .filter(adapter -> adapter != null && adapter.isAvailable()
                        && adapter.supportedCapabilities().contains(capability))
                .findFirst();
    }

    public Set<String> getDetectedMods() {
        return modules.values().stream()
                .filter(module -> isLoaded(module.modId()))
                .map(CompatModule::modId)
                .collect(Collectors.toSet());
    }

    public Map<String, EnumSet<CapabilityId>> getDetectedPermittedActions() {
        Map<String, EnumSet<CapabilityId>> permitted = new HashMap<>();
        for (CompatModule module : modules.values()) {
            if (!isLoaded(module.modId())) {
                continue;
            }
            EnumSet<CapabilityId> actions = EnumSet.noneOf(CapabilityId.class);
            for (CapabilityId capability : module.permittedActions()) {
                if (isActionPermitted(module, capability)) {
                    actions.add(capability);
                }
            }
            if (!actions.isEmpty()) {
                permitted.put(module.modId(), actions);
            }
        }
        return permitted;
    }

    private boolean isLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isActionPermitted(CompatModule module, CapabilityId capability) {
        if (!module.permits(capability)) {
            return false;
        }
        CompatAdapter adapter = adapters.get(module.modId());
        return adapter != null && adapter.isAvailable() && adapter.supportedCapabilities().contains(capability);
    }
}
