package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class SmoothLightingProvider implements CapabilityProvider {
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing smooth lighting state");
        }
        
        try {
            boolean targetState = parseBoolean(params[0]);
            
            GameOptions options = client.options;
            boolean oldState = options.getSmoothLighting().getValue();
            
            if (oldState == targetState) {
                return ActionResult.noChange("Already " + (targetState ? "enabled" : "disabled"));
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("smooth_lighting", oldState);
            
            options.getSmoothLighting().setValue(targetState);
            options.write();
            
            // Reload chunks to apply lighting change
            if (client.worldRenderer != null) {
                client.worldRenderer.reload();
            }
            
            NozhConstants.LOGGER.info("Changed smooth lighting: {} -> {}", oldState, targetState);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set smooth lighting", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("smooth_lighting")) {
            throw new IllegalArgumentException("Invalid snapshot");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        boolean oldState = (boolean) snapshot.get("smooth_lighting");
        
        client.options.getSmoothLighting().setValue(oldState);
        client.options.write();
        
        if (client.worldRenderer != null) {
            client.worldRenderer.reload();
        }
        
        NozhConstants.LOGGER.info("Rolled back smooth lighting to: {}", oldState);
    }
    
    private boolean parseBoolean(Object param) {
        if (param instanceof Boolean) return (Boolean) param;
        String str = param.toString().toLowerCase();
        return str.equals("true") || str.equals("on") || str.equals("1") || str.equals("yes");
    }
}
