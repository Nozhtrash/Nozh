package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Provider for render distance adjustments.
 * 
 * ROADMAP: Phase 1, Sprint 1 - Real provider implementation
 * 
 * This provider directly modifies Minecraft's render distance setting
 * with full validation, rollback support, and state management.
 */
public class RenderDistanceProvider implements CapabilityProvider {
    
    private static final int MIN_RENDER_DISTANCE = 2;
    private static final int MAX_RENDER_DISTANCE = 32;
    
    @Override
    public boolean canExecute(MinecraftClient client) {
        if (client == null || client.options == null) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public StateSnapshot execute(MinecraftClient client, int targetValue) {
        if (!canExecute(client)) {
            NozhConstants.LOGGER.error("Cannot execute RenderDistanceProvider: client not ready");
            return null;
        }
        
        GameOptions options = client.options;
        
        // Validate range
        if (targetValue < MIN_RENDER_DISTANCE || targetValue > MAX_RENDER_DISTANCE) {
            NozhConstants.LOGGER.warn("Invalid render distance: {}. Must be {}-{}", 
                targetValue, MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE);
            return null;
        }
        
        try {
            // Capture current state
            int oldValue = options.getViewDistance().getValue();
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("render_distance", oldValue);
            snapshot.put("timestamp", System.currentTimeMillis());
            
            // Apply change
            options.getViewDistance().setValue(targetValue);
            options.write();
            
            NozhConstants.LOGGER.info("Render distance changed: {} -> {}", oldValue, targetValue);
            
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change render distance", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("render_distance")) {
            NozhConstants.LOGGER.error("Cannot rollback: invalid snapshot");
            return false;
        }
        
        try {
            int oldValue = (Integer) snapshot.get("render_distance");
            client.options.getViewDistance().setValue(oldValue);
            client.options.write();
            
            NozhConstants.LOGGER.info("Render distance rolled back to: {}", oldValue);
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to rollback render distance", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "render_distance";
    }
    
    @Override
    public String getDescription() {
        return "Controls view distance (chunk rendering radius)";
    }
}