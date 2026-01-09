package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class SimulationDistanceProvider implements CapabilityProvider {
    
    private static final int MIN_DISTANCE = 3;
    private static final int MAX_DISTANCE = 32;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing target simulation distance");
        }
        
        try {
            int targetDistance = parseDistance(params[0]);
            
            if (targetDistance < MIN_DISTANCE || targetDistance > MAX_DISTANCE) {
                return ActionResult.invalid(
                    String.format("Simulation distance must be %d-%d, got %d", 
                                  MIN_DISTANCE, MAX_DISTANCE, targetDistance)
                );
            }
            
            GameOptions options = client.options;
            int oldValue = options.getSimulationDistance().getValue();
            
            if (oldValue == targetDistance) {
                return ActionResult.noChange("Already at target");
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("simulation_distance", oldValue);
            
            options.getSimulationDistance().setValue(targetDistance);
            options.write();
            
            NozhConstants.LOGGER.info("Changed simulation distance: {} -> {}", oldValue, targetDistance);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set simulation distance", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("simulation_distance")) {
            throw new IllegalArgumentException("Invalid snapshot");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        int oldValue = (int) snapshot.get("simulation_distance");
        
        client.options.getSimulationDistance().setValue(oldValue);
        client.options.write();
        
        NozhConstants.LOGGER.info("Rolled back simulation distance to: {}", oldValue);
    }
    
    private int parseDistance(Object param) {
        if (param instanceof Integer) return (Integer) param;
        if (param instanceof Number) return ((Number) param).intValue();
        return Integer.parseInt(param.toString());
    }
}
