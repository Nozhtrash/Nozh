package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;

/**
 * Provider for cloud rendering mode.
 * Controls whether and how clouds are rendered.
 */
public class CloudsProvider implements CapabilityProvider {
    
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
        
        if (!(targetValue instanceof CloudRenderMode)) {
            NozhConstants.LOGGER.error("Invalid cloud mode: {}", targetValue);
            return null;
        }
        
        try {
            CloudRenderMode newMode = (CloudRenderMode) targetValue;
            CloudRenderMode oldMode = client.options.getCloudRenderMode().getValue();
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("clouds", oldMode);
            
            client.options.getCloudRenderMode().setValue(newMode);
            client.options.write();
            
            NozhConstants.LOGGER.info("Cloud mode: {} -> {}", oldMode, newMode);
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change cloud mode", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("clouds")) {
            return false;
        }
        
        try {
            CloudRenderMode oldMode = (CloudRenderMode) snapshot.get("clouds");
            client.options.getCloudRenderMode().setValue(oldMode);
            client.options.write();
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Cloud mode rollback failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "clouds";
    }
    
    @Override
    public String getDescription() {
        return "Controls cloud rendering (OFF/FAST/FANCY)";
    }
}