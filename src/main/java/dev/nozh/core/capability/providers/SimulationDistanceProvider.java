package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.state.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Controls simulation distance setting.
 * Valid range: 2-32 chunks.
 * 
 * <p>Simulation distance controls how far game logic updates (mobs, redstone, etc.)
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 1)
 */
public class SimulationDistanceProvider implements CapabilityProvider {
    private static final int MIN_DISTANCE = 2;
    private static final int MAX_DISTANCE = 32;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        int oldValue = options.getSimulationDistance().getValue();
        
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
            return ActionResult.invalid("Simulation distance must be between " + MIN_DISTANCE + " and " + MAX_DISTANCE);
        }
        
        if (targetValue == oldValue) {
            return ActionResult.success(StateSnapshot.single("simulation_distance", oldValue));
        }
        
        StateSnapshot snapshot = StateSnapshot.single("simulation_distance", oldValue);
        
        try {
            options.getSimulationDistance().setValue(targetValue);
            options.write();
            
            NozhConstants.LOGGER.info("Simulation distance changed: {} -> {} chunks", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set simulation distance", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("simulation_distance")) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        
        try {
            Integer oldValue = snapshot.getInteger("simulation_distance");
            if (oldValue != null) {
                client.options.getSimulationDistance().setValue(oldValue);
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back simulation distance to: {}", oldValue);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for simulation distance", e);
        }
    }
}
