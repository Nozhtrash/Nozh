package dev.nozh.core.compat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry for mod permission coordination.
 * 
 * Tracks which mod is responsible for which capability:
 * - Sodium manages render distance
 * - Lithium manages entity AI
 * - NOZH manages culling
 * 
 * Prevents conflicts by yielding control when needed.
 * 
 * TASK 5: Real orchestration - permission system
 */
public final class ModPermissionRegistry {

    private final Map<String, ModOwnership> capabilities = new HashMap<>();
    private final Set<String> activeMods = new HashSet<>();

    /**
     * Register mod as active.
     */
    public void registerMod(String modId) {
        activeMods.add(modId);
    }

    /**
     * Declare capability ownership.
     */
    public void declareOwnership(String capabilityId, String modId, OwnershipLevel level) {
        ModOwnership existing = capabilities.get(capabilityId);

        if (existing == null || level.priority > existing.level.priority) {
            capabilities.put(capabilityId, new ModOwnership(modId, level));
        }
    }

    /**
     * Check if NOZH can control this capability.
     */
    public boolean canNozhControl(String capabilityId) {
        ModOwnership owner = capabilities.get(capabilityId);

        if (owner == null) {
            return true; // No owner = NOZH can control
        }

        if (owner.modId.equals("nozh")) {
            return true;
        }

        // Can override if ownership is SHARED
        return owner.level == OwnershipLevel.SHARED;
    }

    /**
     * Get capability owner.
     */
    public String getOwner(String capabilityId) {
        ModOwnership owner = capabilities.get(capabilityId);
        return owner != null ? owner.modId : "none";
    }

    /**
     * Get ownership level.
     */
    public OwnershipLevel getOwnershipLevel(String capabilityId) {
        ModOwnership owner = capabilities.get(capabilityId);
        return owner != null ? owner.level : OwnershipLevel.SHARED;
    }

    /**
     * Initialize default permissions.
     */
    public void initializeDefaults() {
        // Sodium owns rendering
        if (activeMods.contains("sodium")) {
            declareOwnership("render_distance", "sodium", OwnershipLevel.PRIMARY);
            declareOwnership("clouds", "sodium", OwnershipLevel.PRIMARY);
            declareOwnership("smooth_lighting", "sodium", OwnershipLevel.PRIMARY);
        }

        // Lithium owns logic optimization
        if (activeMods.contains("lithium")) {
            declareOwnership("entity_ai", "lithium", OwnershipLevel.PRIMARY);
            declareOwnership("chunk_ticking", "lithium", OwnershipLevel.PRIMARY);
        }

        // Iris owns shaders
        if (activeMods.contains("iris")) {
            declareOwnership("shaders", "iris", OwnershipLevel.EXCLUSIVE);
        }

        // NOZH owns culling and adaptive quality
        declareOwnership("entity_culling", "nozh", OwnershipLevel.PRIMARY);
        declareOwnership("adaptive_quality", "nozh", OwnershipLevel.PRIMARY);
    }

    private static class ModOwnership {
        final String modId;
        final OwnershipLevel level;

        ModOwnership(String modId, OwnershipLevel level) {
            this.modId = modId;
            this.level = level;
        }
    }

    public enum OwnershipLevel {
        EXCLUSIVE(3),  // Only this mod can control (e.g. Iris shaders)
        PRIMARY(2),    // This mod is primary, but others can assist
        SHARED(1),     // Multiple mods can control
        ADVISORY(0);   // Mod can observe but not control

        final int priority;

        OwnershipLevel(int priority) {
            this.priority = priority;
        }
    }
}
