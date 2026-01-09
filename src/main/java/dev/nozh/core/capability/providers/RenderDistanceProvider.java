package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Controls render distance (view distance) setting.
 * Valid range: 2-32 chunks.
 * 
 * <p>This provider modifies the client's render distance setting
 * and supports rollback to previous values.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 1)
 */
public class RenderDistanceProvider implements CapabilityProvider {
    private static final int MIN_DISTANCE = 2;
    private static final int MAX_DISTANCE = 32;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        int oldValue = options.getViewDistance().getValue();
        
        // Determine target value
        int targetValue;
        if (params != null && params.length > 0 && params[0] instanceof Integer) {
            targetValue = (Integer) params[0];
        } else {
            // Default: reduce by 25%
            targetValue = Math.max(MIN_DISTANCE, (int) (oldValue * 0.75));
        }
        
        // Validate range
        if (targetValue < MIN_DISTANCE || targetValue > MAX_DISTANCE) {
            return ActionResult.invalid("Render distance must be between " + MIN_DISTANCE + " and " + MAX_DISTANCE);
        }
        
        // No change needed
        if (targetValue == oldValue) {
            return ActionResult.success(StateSnapshot.single("render_distance", oldValue));
        }
        
        // Capture state for rollback
        StateSnapshot snapshot = StateSnapshot.single("render_distance", oldValue);
        
        // Apply change
        try {
            options.getViewDistance().setValue(targetValue);
            options.write();
            
            NozhConstants.LOGGER.info("Render distance changed: {} -> {} chunks", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set render distance", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("render_distance")) {
            NozhConstants.LOGGER.warn("Cannot rollback render distance: invalid snapshot");
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            NozhConstants.LOGGER.warn("Cannot rollback: client not available");
            return;
        }
        
        try {
            Integer oldValue = snapshot.getInteger("render_distance");
            if (oldValue != null) {
                client.options.getViewDistance().setValue(oldValue);
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back render distance to: {}", oldValue);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for render distance", e);
        }
    }
}
