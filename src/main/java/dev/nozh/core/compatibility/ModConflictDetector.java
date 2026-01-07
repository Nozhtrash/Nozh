package dev.nozh.core.compatibility;

import dev.nozh.api.compat.StewardshipMode;
import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.compatibility.StewardshipDecision;
import dev.nozh.core.compatibility.StewardshipHandshakeRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashSet;
import java.util.Set;

/**
 * Mod conflict detector to prevent NOZH from interfering with other
 * optimization mods.
 * 
 * Intelligence: Detects installed mods and skips conflicting actions
 * automatically.
 * This ensures NOZH plays nicely with Sodium, Iris, etc.
 */
public final class ModConflictDetector {

    private final Set<String> installedMods = new HashSet<>();

    public ModConflictDetector() {
        // Core Renderers
        checkForMod("sodium");
        checkForMod("canvas");
        checkForMod("vulkanmod");
        checkForMod("nvidium");
        checkForMod("optifabric");
        checkForMod("iris");
        checkForMod("indium");
        checkForMod("distant-horizons");
        checkForMod("bobby");
        checkForMod("farsight");
        checkForMod("cullclouds");
        checkForMod("betterclouds");
        checkForMod("simpleclouds");
        checkForMod("fabricskyboxes");

        // Optimization Suites
        checkForMod("sodium-extra");
        checkForMod("reeses-sodium-options");
        checkForMod("lithium");
        checkForMod("phosphor");
        checkForMod("starlight");
        checkForMod("ferritecore");
        checkForMod("modernfix");
        checkForMod("krypton");
        checkForMod("c2me");
        checkForMod("noisium");
        checkForMod("immediatelyfast");
        checkForMod("exordium");
        checkForMod("entityculling");
        checkForMod("moreculling");
        checkForMod("cullleaves");
        checkForMod("enhancedblockentities");
        checkForMod("lambdynlights");
        checkForMod("dynamic-fps");
        checkForMod("dynamic_fps");
        checkForMod("fpsreducer");
        checkForMod("dashloader");
        checkForMod("memoryleakfix");
        checkForMod("lazydfu");
        checkForMod("debugify");
        checkForMod("smoothboot");
        checkForMod("fastload");
        checkForMod("fastquit");
        checkForMod("threadtweak");
        checkForMod("vmp");
        checkForMod("servercore");
        checkForMod("lazy-language-loader");
        checkForMod("continuity");
        checkForMod("lambdabettergrass");
        checkForMod("animatica");
        checkForMod("entity_texture_features");
        checkForMod("entity_model_features");
        checkForMod("particleculling");
        checkForMod("visuality");
        checkForMod("presencefootsteps");
        checkForMod("blur");
        checkForMod("yosbr"); // YetAnotherConfigLib often comes with this
    }

    private void checkForMod(String modId) {
        if (FabricLoader.getInstance().isModLoaded(modId)) {
            installedMods.add(modId);
        }
    }

    /**
     * Check if a capability conflicts with installed mods.
     * 
     * @param capability The capability to check
     * @return true if there's a conflict (should skip this action)
     */
    public boolean hasConflict(CapabilityId capability) {
        StewardshipDecision handshakeDecision = StewardshipHandshakeRegistry.resolveDecision(capability);
        if (handshakeDecision != null) {
            return handshakeDecision.mode() == StewardshipMode.EXCLUSIVE;
        }
        return switch (capability) {
            // Sodium/Vulkan/Canvas manage chunk rendering
            case RENDER_DISTANCE -> installedMods.contains("sodium") ||
                    installedMods.contains("bobby") ||
                    installedMods.contains("farsight") ||
                    installedMods.contains("vulkanmod") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("distant-horizons") ||
                    installedMods.contains("nvidium");

            // Sodium Extra / Canvas manage particles
            case PARTICLES -> installedMods.contains("sodium-extra") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("particleculling") ||
                    installedMods.contains("visuality") ||
                    installedMods.contains("presencefootsteps");

            // Dynamic FPS / Exordium manage FPS capping
            case FPS_CAP -> installedMods.contains("dynamic-fps") ||
                    installedMods.contains("dynamic_fps") ||
                    installedMods.contains("fpsreducer") ||
                    installedMods.contains("exordium");

            // Iris / Sodium / Canvas manage clouds
            case CLOUDS -> installedMods.contains("iris") ||
                    installedMods.contains("sodium") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("betterclouds") ||
                    installedMods.contains("cullclouds") ||
                    installedMods.contains("simpleclouds") ||
                    installedMods.contains("fabricskyboxes");

            // Sodium / EntityCulling / MoreCulling manage entity distance/culling
            case ENTITY_DISTANCE -> installedMods.contains("sodium") ||
                    installedMods.contains("entityculling") ||
                    installedMods.contains("moreculling") ||
                    installedMods.contains("vulkanmod") ||
                    installedMods.contains("nvidium");

            // Biome blend
            case BIOME_BLEND -> installedMods.contains("sodium") ||
                    installedMods.contains("vulkanmod");

            // Smooth lighting
            case SMOOTH_LIGHTING -> installedMods.contains("sodium") ||
                    installedMods.contains("iris") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("vulkanmod") ||
                    installedMods.contains("optifabric") ||
                    installedMods.contains("continuity");

            // VSync
            case VSYNC -> installedMods.contains("sodium") ||
                    installedMods.contains("vulkanmod") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("nvidium");

            // Fog
            case FOG -> installedMods.contains("sodium") ||
                    installedMods.contains("sodium-extra") ||
                    installedMods.contains("iris") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("optifabric");
            case DYNAMIC_LIGHTING -> installedMods.contains("lambdynlights");

            case GRAPHICS_MODE -> installedMods.contains("sodium") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("vulkanmod") ||
                    installedMods.contains("optifabric") ||
                    installedMods.contains("blur");

            case MIPMAP_LEVEL -> installedMods.contains("sodium") ||
                    installedMods.contains("sodium-extra") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("optifabric") ||
                    installedMods.contains("reeses-sodium-options");

            case CHUNK_LOADING -> installedMods.contains("c2me") ||
                    installedMods.contains("starlight") ||
                    installedMods.contains("phosphor") ||
                    installedMods.contains("dashloader") ||
                    installedMods.contains("noisium");

            case SIMULATION_DISTANCE -> installedMods.contains("c2me") ||
                    installedMods.contains("lithium") ||
                    installedMods.contains("servercore") ||
                    installedMods.contains("vmp");

            case RESOLUTION_SCALE, DISTORTION_EFFECT_SCALE -> installedMods.contains("iris") ||
                    installedMods.contains("optifabric") ||
                    installedMods.contains("canvas");

            case ARMOR_STANDS, ITEM_FRAMES -> installedMods.contains("moreculling") ||
                    installedMods.contains("entityculling");

            case BLOCK_ENTITIES -> installedMods.contains("enhancedblockentities") ||
                    installedMods.contains("moreculling") ||
                    installedMods.contains("indium");

            case ANIMATIONS -> installedMods.contains("immediatelyfast") ||
                    installedMods.contains("animatica") ||
                    installedMods.contains("entity_texture_features") ||
                    installedMods.contains("entity_model_features") ||
                    installedMods.contains("moreculling");

            default -> false; // No known conflicts
        };
    }

    /**
     * Get human-readable reason for conflict.
     */
    public String getConflictReason(CapabilityId capability) {
        StewardshipDecision decision = getStewardshipDecision(capability);
        if (decision == null || decision.mode() == StewardshipMode.NONE) {
            return "No conflict";
        }
        if (decision.mode() == StewardshipMode.SHARED) {
            return "Shared with " + decision.steward();
        }
        return "Managed by " + decision.steward();
    }

    /**
     * GOD MODE: Decide who should steward a feature.
     * 
     * @return Name of the mod we should yield to, or null if NOZH should take
     *         charge.
     */
    public String getSteward(CapabilityId capability) {
        StewardshipDecision decision = getStewardshipDecision(capability);
        if (decision != null && decision.mode() != StewardshipMode.NONE) {
            return decision.steward();
        }
        if (!hasConflict(capability)) {
            return "NOZH";
        }

        return switch (capability) {
            case RENDER_DISTANCE -> {
                if (installedMods.contains("bobby"))
                    yield "Bobby (View Distance)";
                if (installedMods.contains("distant-horizons"))
                    yield "Distant Horizons";
                if (installedMods.contains("farsight"))
                    yield "Farsight";
                if (installedMods.contains("nvidium"))
                    yield "Nvidium";
                if (installedMods.contains("vulkanmod"))
                    yield "VulkanMod";
                if (installedMods.contains("canvas"))
                    yield "Canvas";
                yield "Sodium";
            }
            case PARTICLES -> {
                if (installedMods.contains("sodium-extra"))
                    yield "Sodium Extra";
                if (installedMods.contains("particleculling"))
                    yield "Particle Culling";
                yield "Visuality";
            }
            case FPS_CAP -> installedMods.contains("dynamic-fps") || installedMods.contains("dynamic_fps")
                    ? "Dynamic FPS"
                    : "FPS Reducer";
            case CLOUDS -> {
                if (installedMods.contains("simpleclouds"))
                    yield "Simple Clouds";
                if (installedMods.contains("iris"))
                    yield "Iris";
                yield "Sodium";
            }
            case ENTITY_DISTANCE -> installedMods.contains("entityculling") ? "Entity Culling" : "Sodium";
            case FOG -> installedMods.contains("sodium-extra") ? "Sodium Extra" : "Sodium/Iris";
            case DYNAMIC_LIGHTING -> "LambDynamicLights";
            case GRAPHICS_MODE -> installedMods.contains("canvas") ? "Canvas" : "Sodium";
            case CHUNK_LOADING -> {
                if (installedMods.contains("c2me"))
                    yield "C2ME";
                if (installedMods.contains("noisium"))
                    yield "Noisium";
                yield "Starlight";
            }
            case RESOLUTION_SCALE, DISTORTION_EFFECT_SCALE -> installedMods.contains("iris") ? "Iris" : "Canvas";
            case ARMOR_STANDS, ITEM_FRAMES, BLOCK_ENTITIES -> installedMods.contains("moreculling")
                    ? "More Culling"
                    : "Entity Culling";
            case ANIMATIONS -> installedMods.contains("animatica") ? "Animatica" : "ImmediatelyFast";
            default -> "External Mod";
        };
    }

    /**
     * Returns true if NOZH explicitly handles this capability because no other mod
     * does,
     * OR if NOZH is the specialized expert for it (e.g. Armor
     * Stands/Orchestration).
     * 
     * Note: Even if EnhancedBlockEntities is installing, NOZH's hiding
     * is a stronger optimization (100% culling vs optimized rendering),
     * so NOZH retains control for specialized heavy-load scenarios.
     */
    public boolean isNozhSpecialty(CapabilityId capability) {
        return switch (capability) {
            case ARMOR_STANDS, ITEM_FRAMES, BLOCK_ENTITIES, ANIMATIONS -> true;
            default -> false;
        };
    }

    public StewardshipDecision getStewardshipDecision(CapabilityId capability) {
        StewardshipDecision handshakeDecision = StewardshipHandshakeRegistry.resolveDecision(capability);
        if (handshakeDecision != null) {
            return handshakeDecision;
        }
        if (!hasConflict(capability)) {
            return new StewardshipDecision(capability, "NOZH", StewardshipMode.NONE, "No external conflicts");
        }
        String steward = getSteward(capability);
        return new StewardshipDecision(capability, steward, StewardshipMode.EXCLUSIVE, "Detected mod conflict");
    }

    /**
     * Get list of detected optimization mods.
     */
    public Set<String> getInstalledOptimizationMods() {
        return new HashSet<>(installedMods);
    }

    /**
     * Check if any major optimization mods are installed.
     */
    public boolean hasMajorOptimizationMods() {
        return installedMods.contains("sodium") ||
                installedMods.contains("lithium") ||
                installedMods.contains("iris") ||
                installedMods.contains("vulkanmod") ||
                installedMods.contains("canvas");
    }
}
