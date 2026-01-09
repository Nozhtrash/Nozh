package dev.nozh.core.capability;

import dev.nozh.core.state.StateSnapshot;
import net.minecraft.client.MinecraftClient;

/**
 * Provider interface for capability actions.
 * 
 * <p>Implementations modify game settings and support rollback.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 1)
 */
public interface CapabilityProvider {
    
    /**
     * Execute the capability action.
     * 
     * @param client Minecraft client instance
     * @param params optional parameters for the action
     * @return action result with success status and snapshot
     */
    ActionResult execute(MinecraftClient client, Object... params);
    
    /**
     * Check if this provider supports rollback.
     * 
     * @return true if rollback is supported
     */
    boolean canRollback();
    
    /**
     * Rollback to previous state.
     * 
     * @param snapshot state snapshot to restore
     */
    void rollback(StateSnapshot snapshot);
}
