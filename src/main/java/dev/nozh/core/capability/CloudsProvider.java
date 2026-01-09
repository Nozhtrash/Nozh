package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;

public class CloudsProvider implements CapabilityProvider {
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing cloud render mode");
        }
        
        try {
            CloudRenderMode targetMode = parseMode(params[0]);
            
            GameOptions options = client.options;
            CloudRenderMode oldMode = options.getCloudRenderMode().getValue();
            
            if (oldMode == targetMode) {
                return ActionResult.noChange("Already at " + targetMode);
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("clouds", oldMode);
            
            options.getCloudRenderMode().setValue(targetMode);
            options.write();
            
            NozhConstants.LOGGER.info("Changed clouds: {} -> {}", oldMode, targetMode);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set clouds", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("clouds")) {
            throw new IllegalArgumentException("Invalid snapshot");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        CloudRenderMode oldMode = (CloudRenderMode) snapshot.get("clouds");
        
        client.options.getCloudRenderMode().setValue(oldMode);
        client.options.write();
        
        NozhConstants.LOGGER.info("Rolled back clouds to: {}", oldMode);
    }
    
    private CloudRenderMode parseMode(Object param) {
        if (param instanceof CloudRenderMode) return (CloudRenderMode) param;
        
        String str = param.toString().toUpperCase();
        
        try {
            return CloudRenderMode.valueOf(str);
        } catch (IllegalArgumentException e) {
            if (str.contains("OFF") || str.equals("FALSE")) return CloudRenderMode.OFF;
            if (str.contains("FAST")) return CloudRenderMode.FAST;
            if (str.contains("FANCY")) return CloudRenderMode.FANCY;
            
            throw new IllegalArgumentException("Unknown cloud mode: " + param);
        }
    }
}
