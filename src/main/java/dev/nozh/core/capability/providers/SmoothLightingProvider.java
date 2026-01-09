package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;

/**
 * Provider for smooth lighting toggle.
 * Affects lighting quality.
 */
public class SmoothLightingProvider implements CapabilityProvider {
    
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
        
        if (!(targetValue instanceof Boolean)) {
            NozhConstants.LOGGER.error("Invalid smooth lighting value: {}", targetValue);
            return null;
        }
        
        try {
            boolean newValue = (Boolean) targetValue;
            boolean oldValue = client.options.getSmoothLighting().getValue();
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("smooth_lighting", oldValue);
            
            client.options.getSmoothLighting().setValue(newValue);
            client.options.write();
            
            NozhConstants.LOGGER.info("Smooth lighting: {} -> {}", oldValue, newValue);
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change smooth lighting", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("smooth_lighting")) {
            return false;
        }
        
        try {
            boolean oldValue = (Boolean) snapshot.get("smooth_lighting");
            client.options.getSmoothLighting().setValue(oldValue);
            client.options.write();
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Smooth lighting rollback failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "smooth_lighting";
    }
    
    @Override
    public String getDescription() {
        return "Toggles smooth lighting";
    }
}