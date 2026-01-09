package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class SmoothLightingProvider implements CapabilityProvider {
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        boolean oldValue = options.getSmoothLighting().getValue();
        
        boolean targetValue;
        if (params != null && params.length > 0 && params[0] instanceof Boolean) {
            targetValue = (Boolean) params[0];
        } else {
            targetValue = false;
        }
        
        if (targetValue == oldValue) {
            return ActionResult.success(StateSnapshot.single("smooth_lighting", oldValue));
        }
        
        StateSnapshot snapshot = StateSnapshot.single("smooth_lighting", oldValue);
        
        try {
            options.getSmoothLighting().setValue(targetValue);
            options.write();
            NozhConstants.LOGGER.info("Smooth lighting changed: {} -> {}", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set smooth lighting", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("smooth_lighting")) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        
        try {
            Object oldValue = snapshot.get("smooth_lighting");
            if (oldValue instanceof Boolean value) {
                client.options.getSmoothLighting().setValue(value);
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back smooth lighting to: {}", value);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for smooth lighting", e);
        }
    }
}
