package dev.nozh.core.compatibility;

import net.fabricmc.loader.api.FabricLoader;
import dev.nozh.core.bus.CapabilityId;

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

        // Optimization Suites
        checkForMod("sodium-extra");
        checkForMod("reeses-sodium-options");
        checkForMod("iris");
        checkForMod("lithium");
        checkForMod("phosphor");
        checkForMod("starlight");
        checkForMod("ferritecore");
        checkForMod("modernfix");
        checkForMod("krypton");
        checkForMod("immediatelyfast");
        checkForMod("exordium");
        checkForMod("entityculling");
        checkForMod("moreculling");
        checkForMod("enhancedblockentities");
        checkForMod("lambdynlights");
        checkForMod("dynamic-fps");
        checkForMod("bobby");
        checkForMod("c2me");
        checkForMod("dashloader");
        checkForMod("memoryleakfix");
        checkForMod("lazydfu");
        checkForMod("debugify");
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
        return switch (capability) {
            // Sodium/Vulkan/Canvas manage chunk rendering
            case RENDER_DISTANCE -> installedMods.contains("sodium") ||
                    installedMods.contains("bobby") ||
                    installedMods.contains("vulkanmod") ||
                    installedMods.contains("canvas");

            // Sodium Extra / Canvas manage particles
            case PARTICLES -> installedMods.contains("sodium-extra") ||
                    installedMods.contains("canvas");

            // Dynamic FPS / Exordium manage FPS capping
            case FPS_CAP -> installedMods.contains("dynamic-fps") ||
                    installedMods.contains("exordium");

            // Iris / Sodium / Canvas manage clouds
            case CLOUDS -> installedMods.contains("iris") ||
                    installedMods.contains("sodium") ||
                    installedMods.contains("canvas");

            // Sodium / EntityCulling / MoreCulling manage entity distance/culling
            case ENTITY_DISTANCE -> installedMods.contains("sodium") ||
                    installedMods.contains("entityculling") ||
                    installedMods.contains("vulkanmod");

            // Biome blend
            case BIOME_BLEND -> installedMods.contains("sodium") ||
                    installedMods.contains("vulkanmod");

            // Smooth lighting
            case SMOOTH_LIGHTING -> installedMods.contains("sodium") ||
                    installedMods.contains("iris") ||
                    installedMods.contains("canvas") ||
                    installedMods.contains("vulkanmod");

            // VSync
            case VSYNC -> installedMods.contains("sodium") ||
                    installedMods.contains("vulkanmod") ||
                    installedMods.contains("canvas");

            // Fog
            case FOG -> installedMods.contains("sodium") ||
                    installedMods.contains("sodium-extra") ||
                    installedMods.contains("iris") ||
                    installedMods.contains("canvas");
            case DYNAMIC_LIGHTING -> installedMods.contains("lambdynlights");

            default -> false; // No known conflicts
        };
    }

    /**
     * Get human-readable reason for conflict.
     */
    public String getConflictReason(CapabilityId capability) {
        String steward = getSteward(capability);
        return "NOZH".equals(steward) ? "No conflict" : "Managed by " + steward;
    }

    /**
     * GOD MODE: Decide who should steward a feature.
     * 
     * @return Name of the mod we should yield to, or null if NOZH should take
     *         charge.
     */
    public String getSteward(CapabilityId capability) {
        if (!hasConflict(capability))
            return "NOZH";

        return switch (capability) {
            case RENDER_DISTANCE -> {
                if (installedMods.contains("bobby"))
                    yield "Bobby (View Distance)";
                if (installedMods.contains("vulkanmod"))
                    yield "VulkanMod";
                if (installedMods.contains("canvas"))
                    yield "Canvas";
                yield "Sodium";
            }
            case PARTICLES -> installedMods.contains("sodium-extra") ? "Sodium Extra" : "External Mod";
            case FPS_CAP -> installedMods.contains("dynamic-fps") ? "Dynamic FPS" : "Exordium";
            case CLOUDS -> installedMods.contains("iris") ? "Iris" : "Sodium";
            case ENTITY_DISTANCE -> installedMods.contains("entityculling") ? "Entity Culling" : "Sodium";
            case FOG -> installedMods.contains("sodium-extra") ? "Sodium Extra" : "Sodium/Iris";
            case DYNAMIC_LIGHTING -> "LambDynamicLights";
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
