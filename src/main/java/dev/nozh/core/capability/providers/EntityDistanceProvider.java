package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.state.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Controls entity distance scaling.
 * Valid range: 0.5 (50%) to 5.0 (500%).
 * 
 * <p>Lower values reduce entity rendering distance, improving performance
 * in entity-heavy scenarios.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 1)
 */
public class EntityDistanceProvider implements CapabilityProvider {
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 5.0;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        double oldValue = options.getEntityDistanceScaling().getValue();
        
        // Determine target value
        double targetValue;
        if (params != null && params.length > 0 && params[0] instanceof Number) {
            targetValue = ((Number) params[0]).doubleValue();
        } else {
            // Default: reduce by 25%
            targetValue = Math.max(MIN_SCALE, oldValue * 0.75);
        }
        
        // Validate range
        if (targetValue < MIN_SCALE || targetValue > MAX_SCALE) {
            return ActionResult.invalid("Entity distance must be between " + MIN_SCALE + " and " + MAX_SCALE);
        }
        
        if (Math.abs(targetValue - oldValue) < 0.01) { // Epsilon comparison
            return ActionResult.success(StateSnapshot.single("entity_distance", oldValue));
        }
        
        StateSnapshot snapshot = StateSnapshot.single("entity_distance", oldValue);
        
        try {
            options.getEntityDistanceScaling().setValue(targetValue);
            options.write();
            
            NozhConstants.LOGGER.info("Entity distance changed: {:.2f} -> {:.2f}", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set entity distance", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("entity_distance")) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        
        try {
            Object oldValue = snapshot.get("entity_distance");
            if (oldValue instanceof Number num) {
                client.options.getEntityDistanceScaling().setValue(num.doubleValue());
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back entity distance to: {}", oldValue);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for entity distance", e);
        }
    }
}
