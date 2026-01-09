package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class VsyncProvider implements CapabilityProvider {
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing vsync state");
        }
        
        try {
            boolean targetState = parseBoolean(params[0]);
            
            GameOptions options = client.options;
            boolean oldState = options.getEnableVsync().getValue();
            
            if (oldState == targetState) {
                return ActionResult.noChange("VSync already " + (targetState ? "enabled" : "disabled"));
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("vsync", oldState);
            
            options.getEnableVsync().setValue(targetState);
            options.write();
            
            NozhConstants.LOGGER.info("Changed VSync: {} -> {}", oldState, targetState);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set VSync", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("vsync")) {
            throw new IllegalArgumentException("Invalid snapshot");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        boolean oldState = (boolean) snapshot.get("vsync");
        
        client.options.getEnableVsync().setValue(oldState);
        client.options.write();
        
        NozhConstants.LOGGER.info("Rolled back VSync to: {}", oldState);
    }
    
    private boolean parseBoolean(Object param) {
        if (param instanceof Boolean) return (Boolean) param;
        String str = param.toString().toLowerCase();
        return str.equals("true") || str.equals("on") || str.equals("1") || str.equals("yes");
    }
}
