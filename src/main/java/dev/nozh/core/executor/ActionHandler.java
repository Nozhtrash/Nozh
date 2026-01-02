package dev.nozh.core.executor;

import net.minecraft.client.MinecraftClient;

/**
 * Handles the actual execution of an action.
 * The ONLY place where net.minecraft imports are allowed for execution logic.
 */
public interface ActionHandler {
    /**
     * Execute the action.
     * 
     * @param client Minecraft instance
     * @return true if changed, false if stuck/already at limit
     */
    boolean execute(MinecraftClient client);

    /**
     * Get details of the change for history.
     * 
     * @return Formatted string "OLD -> NEW"
     */
    String getLastChangeDetails();

    String getOldValue();

    String getNewValue();

    /**
     * Phase 6.5: Apply a specific value (used for Rollback).
     * 
     * @param client Minecraft instance
     * @param value  The value to apply (from history)
     * @return true if applied successfully
     */
    boolean apply(MinecraftClient client, String value);
}
