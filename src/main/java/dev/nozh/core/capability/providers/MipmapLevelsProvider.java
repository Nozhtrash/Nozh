package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class MipmapLevelsProvider implements CapabilityProvider {
    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 4;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        int oldValue = options.getMipmapLevels().getValue();
        
        int targetValue;
        if (params != null && params.length > 0 && params[0] instanceof Integer) {
            targetValue = (Integer) params[0];
        } else {
            targetValue = Math.max(MIN_LEVEL, oldValue - 1);
        }
        
        if (targetValue < MIN_LEVEL || targetValue > MAX_LEVEL) {
            return ActionResult.invalid("Mipmap level must be between " + MIN_LEVEL + " and " + MAX_LEVEL);
        }
        
        if (targetValue == oldValue) {
            return ActionResult.success(StateSnapshot.single("mipmap_levels", oldValue));
        }
        
        StateSnapshot snapshot = StateSnapshot.single("mipmap_levels", oldValue);
        
        try {
            options.getMipmapLevels().setValue(targetValue);
            options.write();
            NozhConstants.LOGGER.info("Mipmap levels changed: {} -> {}", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set mipmap levels", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("mipmap_levels")) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        
        try {
            Integer oldValue = snapshot.getInteger("mipmap_levels");
            if (oldValue != null) {
                client.options.getMipmapLevels().setValue(oldValue);
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back mipmap levels to: {}", oldValue);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for mipmap levels", e);
        }
    }
}
