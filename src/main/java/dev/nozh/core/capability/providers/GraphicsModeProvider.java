package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;

/**
 * Provider for graphics mode (fancy/fast/fabulous).
 * Controls overall rendering quality.
 */
public class GraphicsModeProvider implements CapabilityProvider {
    
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
        
        if (!(targetValue instanceof GraphicsMode)) {
            NozhConstants.LOGGER.error("Invalid graphics mode: {}", targetValue);
            return null;
        }
        
        try {
            GraphicsMode newMode = (GraphicsMode) targetValue;
            GraphicsMode oldMode = client.options.getGraphicsMode().getValue();
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("graphics_mode", oldMode);
            
            client.options.getGraphicsMode().setValue(newMode);
            client.options.write();
            
            NozhConstants.LOGGER.info("Graphics mode: {} -> {}", oldMode, newMode);
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change graphics mode", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("graphics_mode")) {
            return false;
        }
        
        try {
            GraphicsMode oldMode = (GraphicsMode) snapshot.get("graphics_mode");
            client.options.getGraphicsMode().setValue(oldMode);
            client.options.write();
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Graphics mode rollback failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "graphics_mode";
    }
    
    @Override
    public String getDescription() {
        return "Controls graphics quality (FAST/FANCY/FABULOUS)";
    }
}