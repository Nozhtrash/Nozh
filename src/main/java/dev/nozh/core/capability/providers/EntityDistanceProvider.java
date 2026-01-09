package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;

/**
 * Provider for entity render distance scaling.
 * Controls how far entities are rendered.
 */
public class EntityDistanceProvider implements CapabilityProvider {
    
    private static final double MIN_ENTITY_DISTANCE = 0.5;
    private static final double MAX_ENTITY_DISTANCE = 5.0;
    
    @Override
    public boolean canExecute(MinecraftClient client) {
        return client != null && client.options != null;
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public StateSnapshot execute(MinecraftClient client, Object targetValue) {
        if (!canExecute(client)) {
            return null;
        }
        
        double newScale;
        if (targetValue instanceof Number) {
            newScale = ((Number) targetValue).doubleValue();
        } else {
            NozhConstants.LOGGER.error("Invalid entity distance: {}", targetValue);
            return null;
        }
        
        if (newScale < MIN_ENTITY_DISTANCE || newScale > MAX_ENTITY_DISTANCE) {
            NozhConstants.LOGGER.warn("Entity distance out of range: {}", newScale);
            return null;
        }
        
        try {
            double oldScale = client.options.getEntityDistanceScaling().getValue();
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("entity_distance", oldScale);
            
            client.options.getEntityDistanceScaling().setValue(newScale);
            client.options.write();
            
            NozhConstants.LOGGER.info("Entity distance: {:.1f} -> {:.1f}", oldScale, newScale);
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change entity distance", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("entity_distance")) {
            return false;
        }
        
        try {
            double oldScale = (Double) snapshot.get("entity_distance");
            client.options.getEntityDistanceScaling().setValue(oldScale);
            client.options.write();
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Entity distance rollback failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "entity_distance";
    }
    
    @Override
    public String getDescription() {
        return "Controls entity render distance scaling (0.5-5.0)";
    }
}