package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class RenderDistanceProvider implements CapabilityProvider {
    
    private static final int MIN_DISTANCE = 2;
    private static final int MAX_DISTANCE = 32;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing target distance parameter");
        }
        
        try {
            int targetDistance = parseDistance(params[0]);
            
            // Validate range
            if (targetDistance < MIN_DISTANCE || targetDistance > MAX_DISTANCE) {
                return ActionResult.invalid(
                    String.format("Distance must be between %d and %d, got %d", 
                                  MIN_DISTANCE, MAX_DISTANCE, targetDistance)
                );
            }
            
            GameOptions options = client.options;
            int oldValue = options.getViewDistance().getValue();
            
            // No-op if already at target
            if (oldValue == targetDistance) {
                return ActionResult.noChange("Already at target distance: " + targetDistance);
            }
            
            // Create snapshot for rollback
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("render_distance", oldValue);
            
            // Apply change
            options.getViewDistance().setValue(targetDistance);
            options.write();
            
            NozhConstants.LOGGER.info("Changed render distance: {} -> {}", oldValue, targetDistance);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set render distance", e);
            return ActionResult.error("Exception: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("render_distance")) {
            throw new IllegalArgumentException("Invalid snapshot for render distance rollback");
        }
        
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            int oldValue = (int) snapshot.get("render_distance");
            
            client.options.getViewDistance().setValue(oldValue);
            client.options.write();
            
            NozhConstants.LOGGER.info("Rolled back render distance to: {}", oldValue);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to rollback render distance", e);
            throw new RuntimeException("Rollback failed", e);
        }
    }
    
    private int parseDistance(Object param) {
        if (param instanceof Integer) {
            return (Integer) param;
        }
        if (param instanceof String) {
            return Integer.parseInt((String) param);
        }
        if (param instanceof Number) {
            return ((Number) param).intValue();
        }
        throw new IllegalArgumentException("Cannot parse distance from: " + param.getClass());
    }
}
