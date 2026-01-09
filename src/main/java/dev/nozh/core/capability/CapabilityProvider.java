package dev.nozh.core.capability;

import net.minecraft.client.MinecraftClient;

/**
 * Core interface for capability providers.
 * 
 * A CapabilityProvider is responsible for modifying a specific game setting
 * in a safe, reversible manner.
 * 
 * Contract:
 * - execute() returns ActionResult indicating success/failure
 * - If canRollback() returns true, rollback() must be implemented
 * - Providers must be isolated - one failing provider cannot crash others
 * 
 * @since v0.2.0-alpha
 */
public interface CapabilityProvider {
    
    /**
     * Execute the capability action.
     * 
     * @param client Minecraft client instance
     * @param params optional parameters for the action
     * @return ActionResult with status and optional snapshot
     */
    ActionResult execute(MinecraftClient client, Object... params);
    
    /**
     * Check if this provider supports rollback.
     * 
     * @return true if rollback is supported
     */
    boolean canRollback();
    
    /**
     * Rollback to a previous state.
     * Only called if canRollback() returns true.
     * 
     * @param snapshot the state to restore
     */
    void rollback(StateSnapshot snapshot);
    
    /**
     * Get the unique identifier for this provider.
     * Should match the action ID in ActionMatrix.
     * 
     * @return action identifier
     */
    String getActionId();
    
    /**
     * Get human-readable description of this provider.
     * 
     * @return description
     */
    String getDescription();
}
