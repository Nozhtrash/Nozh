package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;

/**
 * Provider for mipmap levels.
 * Controls texture quality at distance.
 */
public class MipmapLevelsProvider implements CapabilityProvider {
    
    private static final int MIN_MIPMAP = 0;
    private static final int MAX_MIPMAP = 4;
    
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
        
        int newLevel;
        if (targetValue instanceof Number) {
            newLevel = ((Number) targetValue).intValue();
        } else {
            NozhConstants.LOGGER.error("Invalid mipmap level: {}", targetValue);
            return null;
        }
        
        if (newLevel < MIN_MIPMAP || newLevel > MAX_MIPMAP) {
            NozhConstants.LOGGER.warn("Mipmap level out of range: {}", newLevel);
            return null;
        }
        
        try {
            int oldLevel = client.options.getMipmapLevels().getValue();
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("mipmap_levels", oldLevel);
            
            client.options.getMipmapLevels().setValue(newLevel);
            client.options.write();
            
            NozhConstants.LOGGER.info("Mipmap levels: {} -> {}", oldLevel, newLevel);
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change mipmap levels", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("mipmap_levels")) {
            return false;
        }
        
        try {
            int oldLevel = (Integer) snapshot.get("mipmap_levels");
            client.options.getMipmapLevels().setValue(oldLevel);
            client.options.write();
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Mipmap rollback failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "mipmap_levels";
    }
    
    @Override
    public String getDescription() {
        return "Controls texture mipmapping (0-4)";
    }
}