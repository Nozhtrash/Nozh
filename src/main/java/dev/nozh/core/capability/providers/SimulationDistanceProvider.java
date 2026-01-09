package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Provider for simulation distance adjustments.
 * 
 * ROADMAP: Phase 1, Sprint 1
 * 
 * Simulation distance controls how far away from the player 
 * the game world is actively simulated (mob AI, crops growing, etc.)
 */
public class SimulationDistanceProvider implements CapabilityProvider {
    
    private static final int MIN_SIMULATION_DISTANCE = 5;
    private static final int MAX_SIMULATION_DISTANCE = 32;
    
    @Override
    public boolean canExecute(MinecraftClient client) {
        return client != null && client.options != null;
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public StateSnapshot execute(MinecraftClient client, int targetValue) {
        if (!canExecute(client)) {
            NozhConstants.LOGGER.error("Cannot execute SimulationDistanceProvider");
            return null;
        }
        
        GameOptions options = client.options;
        
        // Validate range
        if (targetValue < MIN_SIMULATION_DISTANCE || targetValue > MAX_SIMULATION_DISTANCE) {
            NozhConstants.LOGGER.warn("Invalid simulation distance: {}", targetValue);
            return null;
        }
        
        try {
            int oldValue = options.getSimulationDistance().getValue();
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("simulation_distance", oldValue);
            
            options.getSimulationDistance().setValue(targetValue);
            options.write();
            
            NozhConstants.LOGGER.info("Simulation distance: {} -> {}", oldValue, targetValue);
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change simulation distance", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("simulation_distance")) {
            return false;
        }
        
        try {
            int oldValue = (Integer) snapshot.get("simulation_distance");
            client.options.getSimulationDistance().setValue(oldValue);
            client.options.write();
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "simulation_distance";
    }
    
    @Override
    public String getDescription() {
        return "Controls simulation distance (CPU-bound)";
    }
}