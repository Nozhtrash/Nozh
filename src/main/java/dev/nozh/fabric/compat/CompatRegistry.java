package dev.nozh.fabric.compat;

import dev.nozh.api.compat.StewardshipMode;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.compatibility.StewardshipDecision;
import dev.nozh.core.compatibility.StewardshipHandshakeRegistry;
import dev.nozh.api.compat.StewardshipDeclaration;
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
    private final CompatActionRegistry actionRegistry = new CompatActionRegistry();

    public CompatRegistry() {
        register(new CompatModule("sodium", "Sodium",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.VSYNC, CapabilityId.FOG, CapabilityId.BIOME_BLEND,
                        CapabilityId.ENTITY_DISTANCE, CapabilityId.RENDER_DISTANCE, CapabilityId.MIPMAP_LEVEL,
                        CapabilityId.SMOOTH_LIGHTING, CapabilityId.PARTICLES, CapabilityId.GRAPHICS_MODE)));
        register(new CompatModule("sodium-extra", "Sodium Extra",
                EnumSet.of(CapabilityId.PARTICLES, CapabilityId.FOG, CapabilityId.CLOUDS,
                        CapabilityId.SMOOTH_LIGHTING, CapabilityId.MIPMAP_LEVEL)));
        register(new CompatModule("reeses-sodium-options", "Reese's Sodium Options",
                EnumSet.of(CapabilityId.MIPMAP_LEVEL, CapabilityId.SMOOTH_LIGHTING, CapabilityId.FOG)));
        register(new CompatModule("iris", "Iris",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.FOG, CapabilityId.SMOOTH_LIGHTING,
                        CapabilityId.RESOLUTION_SCALE, CapabilityId.DISTORTION_EFFECT_SCALE)));
        register(new CompatModule("indium", "Indium",
                EnumSet.of(CapabilityId.BLOCK_ENTITIES)));
        register(new CompatModule("optifabric", "OptiFabric",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.FOG, CapabilityId.SMOOTH_LIGHTING,
                        CapabilityId.MIPMAP_LEVEL, CapabilityId.RESOLUTION_SCALE)));
        register(new CompatModule("canvas", "Canvas",
                EnumSet.of(CapabilityId.CLOUDS, CapabilityId.VSYNC, CapabilityId.PARTICLES, CapabilityId.FOG,
                        CapabilityId.SMOOTH_LIGHTING, CapabilityId.MIPMAP_LEVEL, CapabilityId.RESOLUTION_SCALE)));
        register(new CompatModule("vulkanmod", "VulkanMod",
                EnumSet.of(CapabilityId.RENDER_DISTANCE, CapabilityId.VSYNC, CapabilityId.CLOUDS,
                        CapabilityId.FOG, CapabilityId.SMOOTH_LIGHTING)));
        register(new CompatModule("nvidium", "Nvidium",
                EnumSet.of(CapabilityId.RENDER_DISTANCE, CapabilityId.VSYNC, CapabilityId.ENTITY_DISTANCE)));
        register(new CompatModule("bobby", "Bobby",
                EnumSet.of(CapabilityId.RENDER_DISTANCE)));
        register(new CompatModule("farsight", "Farsight",
                EnumSet.of(CapabilityId.RENDER_DISTANCE)));
        register(new CompatModule("distant-horizons", "Distant Horizons",
                EnumSet.of(CapabilityId.RENDER_DISTANCE, CapabilityId.FOG)));
        register(new CompatModule("c2me", "C2ME",
                EnumSet.of(CapabilityId.CHUNK_LOADING, CapabilityId.SIMULATION_DISTANCE)));
        register(new CompatModule("lithium", "Lithium",
                EnumSet.of(CapabilityId.SIMULATION_DISTANCE)));
        register(new CompatModule("phosphor", "Phosphor",
                EnumSet.of(CapabilityId.CHUNK_LOADING, CapabilityId.SMOOTH_LIGHTING)));
        register(new CompatModule("starlight", "Starlight",
                EnumSet.of(CapabilityId.CHUNK_LOADING, CapabilityId.SMOOTH_LIGHTING)));
        register(new CompatModule("ferritecore", "FerriteCore",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("modernfix", "ModernFix",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("krypton", "Krypton",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("noisium", "Noisium",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("immediatelyfast", "ImmediatelyFast",
                EnumSet.of(CapabilityId.ANIMATIONS, CapabilityId.ENTITY_SHADOWS)));
        register(new CompatModule("exordium", "Exordium",
                EnumSet.of(CapabilityId.FPS_CAP)));
        register(new CompatModule("entityculling", "Entity Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE, CapabilityId.BLOCK_ENTITIES,
                        CapabilityId.ITEM_FRAMES, CapabilityId.ARMOR_STANDS)));
        register(new CompatModule("moreculling", "More Culling",
                EnumSet.of(CapabilityId.ENTITY_DISTANCE, CapabilityId.BLOCK_ENTITIES,
                        CapabilityId.ITEM_FRAMES, CapabilityId.ARMOR_STANDS, CapabilityId.ANIMATIONS)));
        register(new CompatModule("cullleaves", "Cull Leaves",
                EnumSet.of(CapabilityId.ANIMATIONS)));
        register(new CompatModule("enhancedblockentities", "Enhanced Block Entities",
                EnumSet.of(CapabilityId.BLOCK_ENTITIES)));
        register(new CompatModule("lambdynlights", "LambDynamicLights",
                EnumSet.of(CapabilityId.DYNAMIC_LIGHTING)));
        register(new CompatModule("dynamic-fps", "Dynamic FPS",
                EnumSet.of(CapabilityId.FPS_CAP)));
        register(new CompatModule("dynamic_fps", "Dynamic FPS",
                EnumSet.of(CapabilityId.FPS_CAP)));
        register(new CompatModule("fpsreducer", "FPS Reducer",
                EnumSet.of(CapabilityId.FPS_CAP)));
        register(new CompatModule("dashloader", "DashLoader",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("memoryleakfix", "Memory Leak Fix",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("lazydfu", "LazyDFU",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("lazy-language-loader", "Lazy Language Loader",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("debugify", "Debugify",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("smoothboot", "Smooth Boot",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("fastload", "Fastload",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("fastquit", "FastQuit",
                EnumSet.of(CapabilityId.CHUNK_LOADING)));
        register(new CompatModule("threadtweak", "ThreadTweak",
                EnumSet.of(CapabilityId.SIMULATION_DISTANCE)));
        register(new CompatModule("vmp", "VMP",
                EnumSet.of(CapabilityId.SIMULATION_DISTANCE)));
        register(new CompatModule("servercore", "ServerCore",
                EnumSet.of(CapabilityId.SIMULATION_DISTANCE)));
        register(new CompatModule("cullclouds", "Cull Clouds",
                EnumSet.of(CapabilityId.CLOUDS)));
        register(new CompatModule("simpleclouds", "Simple Clouds",
                EnumSet.of(CapabilityId.CLOUDS)));
        register(new CompatModule("betterclouds", "Better Clouds",
                EnumSet.of(CapabilityId.CLOUDS)));
        register(new CompatModule("continuity", "Continuity",
                EnumSet.of(CapabilityId.MIPMAP_LEVEL, CapabilityId.SMOOTH_LIGHTING)));
        register(new CompatModule("lambdabettergrass", "LambdaBetterGrass",
                EnumSet.of(CapabilityId.SMOOTH_LIGHTING)));
        register(new CompatModule("animatica", "Animatica",
                EnumSet.of(CapabilityId.ANIMATIONS)));
        register(new CompatModule("entity_texture_features", "Entity Texture Features",
                EnumSet.of(CapabilityId.ANIMATIONS)));
        register(new CompatModule("entity_model_features", "Entity Model Features",
                EnumSet.of(CapabilityId.ANIMATIONS)));
        register(new CompatModule("particleculling", "Particle Culling",
                EnumSet.of(CapabilityId.PARTICLES)));
        register(new CompatModule("visuality", "Visuality",
                EnumSet.of(CapabilityId.PARTICLES)));
        register(new CompatModule("presencefootsteps", "Presence Footsteps",
                EnumSet.of(CapabilityId.PARTICLES)));
        register(new CompatModule("fabricskyboxes", "FabricSkyBoxes",
                EnumSet.of(CapabilityId.CLOUDS)));
        register(new CompatModule("blur", "Blur",
                EnumSet.of(CapabilityId.GRAPHICS_MODE)));

        actionRegistry.register("sodium", EnumSet.of(
                CapabilityId.CLOUDS,
                CapabilityId.SMOOTH_LIGHTING,
                CapabilityId.MIPMAP_LEVEL));
        actionRegistry.register("sodium-extra", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("reeses-sodium-options", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("iris", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("indium", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("optifabric", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("canvas", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("vulkanmod", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("nvidium", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("bobby", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("farsight", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("distant-horizons", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("c2me", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("lithium", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("phosphor", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("starlight", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("ferritecore", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("modernfix", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("krypton", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("noisium", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("immediatelyfast", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("exordium", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("entityculling", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("moreculling", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("cullleaves", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("enhancedblockentities", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("lambdynlights", EnumSet.of(CapabilityId.DYNAMIC_LIGHTING));
        actionRegistry.register("dynamic-fps", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("dynamic_fps", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("fpsreducer", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("dashloader", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("memoryleakfix", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("lazydfu", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("lazy-language-loader", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("debugify", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("smoothboot", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("fastload", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("fastquit", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("threadtweak", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("vmp", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("servercore", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("cullclouds", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("simpleclouds", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("betterclouds", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("continuity", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("lambdabettergrass", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("animatica", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("entity_texture_features", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("entity_model_features", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("particleculling", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("visuality", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("presencefootsteps", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("fabricskyboxes", EnumSet.noneOf(CapabilityId.class));
        actionRegistry.register("blur", EnumSet.noneOf(CapabilityId.class));

        registerAdapter(new SodiumOptionsAdapter());
        registerAdapter(new LambDynamicLightsAdapter());

        syncStewardshipDeclarations();
    }

    private void register(CompatModule module) {
        modules.put(module.modId(), module);
    }

    private void registerAdapter(CompatAdapter adapter) {
        adapters.put(adapter.modId(), adapter);
    }

    public boolean isExternallyManaged(CapabilityId capability) {
        syncStewardshipDeclarations();
        StewardshipDecision handshakeDecision = StewardshipHandshakeRegistry.resolveDecision(capability);
        if (handshakeDecision != null) {
            return handshakeDecision.mode() == StewardshipMode.EXCLUSIVE;
        }
        return modules.values().stream()
                .anyMatch(module -> module.manages(capability) && isLoaded(module.modId())
                        && !isActionPermitted(module, capability));
    }

    public String getSteward(CapabilityId capability) {
        StewardshipDecision decision = getStewardshipDecision(capability);
        if (decision == null) {
            return null;
        }
        return decision.steward();
    }

    public boolean isActionPermitted(CapabilityId capability) {
        return modules.values().stream()
                .filter(module -> actionRegistry.isPermitted(module.modId(), capability) && isLoaded(module.modId()))
                .anyMatch(module -> isActionPermitted(module, capability));
    }

    public Optional<CompatAdapter> getAdapter(CapabilityId capability) {
        return modules.values().stream()
                .filter(module -> actionRegistry.isPermitted(module.modId(), capability) && isLoaded(module.modId()))
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
            for (CapabilityId capability : actionRegistry.permittedActions(module.modId())) {
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

    public StewardshipDecision getStewardshipDecision(CapabilityId capability) {
        syncStewardshipDeclarations();
        StewardshipDecision handshakeDecision = StewardshipHandshakeRegistry.resolveDecision(capability);
        if (handshakeDecision != null) {
            return handshakeDecision;
        }
        for (CompatModule module : modules.values()) {
            if (!module.manages(capability) || !isLoaded(module.modId())) {
                continue;
            }
            StewardshipMode mode = isActionPermitted(module, capability)
                    ? StewardshipMode.SHARED
                    : StewardshipMode.EXCLUSIVE;
            String reason = mode == StewardshipMode.SHARED
                    ? "Adapter handshake approved"
                    : "External mod manages capability";
            return new StewardshipDecision(capability, module.displayName(), mode, reason);
        }
        return null;
    }

    private boolean isActionPermitted(CompatModule module, CapabilityId capability) {
        if (!actionRegistry.isPermitted(module.modId(), capability)) {
            return false;
        }
        CompatAdapter adapter = adapters.get(module.modId());
        return adapter != null && adapter.isAvailable() && adapter.supportedCapabilities().contains(capability);
    }

    private void syncStewardshipDeclarations() {
        for (CompatModule module : modules.values()) {
            EnumSet<CapabilityId> shared = EnumSet.noneOf(CapabilityId.class);
            EnumSet<CapabilityId> exclusive = EnumSet.noneOf(CapabilityId.class);
            for (CapabilityId capability : module.managedCapabilities()) {
                if (isActionPermitted(module, capability)) {
                    shared.add(capability);
                } else {
                    exclusive.add(capability);
                }
            }
            StewardshipDeclaration.Builder builder = StewardshipDeclaration.builder(module.modId(), module.displayName())
                    .reason("Compat registry stewardship handshake");
            if (!shared.isEmpty()) {
                builder.shared(shared.toArray(new CapabilityId[0]));
            }
            if (!exclusive.isEmpty()) {
                builder.exclusive(exclusive.toArray(new CapabilityId[0]));
            }
            StewardshipHandshakeRegistry.register(builder.build());
        }
    }
}
