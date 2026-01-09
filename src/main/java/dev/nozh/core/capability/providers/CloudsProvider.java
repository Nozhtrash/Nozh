package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;

/**
 * Controls cloud rendering setting.
 * Options: FANCY, FAST, OFF.
 * 
 * <p>Disabling clouds improves performance with minimal visual impact.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 1)
 */
public class CloudsProvider implements CapabilityProvider {
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        CloudRenderMode oldValue = options.getCloudRenderMode().getValue();
        
        // Determine target value
        CloudRenderMode targetValue;
        if (params != null && params.length > 0 && params[0] instanceof CloudRenderMode) {
            targetValue = (CloudRenderMode) params[0];
        } else {
            // Default: disable clouds
            targetValue = CloudRenderMode.OFF;
        }
        
        if (targetValue == oldValue) {
            return ActionResult.success(StateSnapshot.single("clouds", oldValue));
        }
        
        StateSnapshot snapshot = StateSnapshot.single("clouds", oldValue);
        
        try {
            options.getCloudRenderMode().setValue(targetValue);
            options.write();
            
            NozhConstants.LOGGER.info("Clouds changed: {} -> {}", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set clouds", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("clouds")) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        
        try {
            Object oldValue = snapshot.get("clouds");
            if (oldValue instanceof CloudRenderMode mode) {
                client.options.getCloudRenderMode().setValue(mode);
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back clouds to: {}", mode);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for clouds", e);
        }
    }
}
