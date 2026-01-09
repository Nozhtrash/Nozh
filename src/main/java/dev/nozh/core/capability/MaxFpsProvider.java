package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class MaxFpsProvider implements CapabilityProvider {
    
    private static final int MIN_FPS = 10;
    private static final int MAX_FPS = 260;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing max FPS value");
        }
        
        try {
            int targetFps = parseFps(params[0]);
            
            if (targetFps < MIN_FPS || targetFps > MAX_FPS) {
                return ActionResult.invalid(
                    String.format("Max FPS must be %d-%d, got %d", 
                                  MIN_FPS, MAX_FPS, targetFps)
                );
            }
            
            GameOptions options = client.options;
            int oldFps = options.getMaxFps().getValue();
            
            if (oldFps == targetFps) {
                return ActionResult.noChange("Already capped at " + targetFps + " FPS");
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("max_fps", oldFps);
            
            options.getMaxFps().setValue(targetFps);
            options.write();
            
            NozhConstants.LOGGER.info("Changed max FPS: {} -> {}", oldFps, targetFps);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set max FPS", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("max_fps")) {
            throw new IllegalArgumentException("Invalid snapshot");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        int oldFps = (int) snapshot.get("max_fps");
        
        client.options.getMaxFps().setValue(oldFps);
        client.options.write();
        
        NozhConstants.LOGGER.info("Rolled back max FPS to: {}", oldFps);
    }
    
    private int parseFps(Object param) {
        if (param instanceof Integer) return (Integer) param;
        if (param instanceof Number) return ((Number) param).intValue();
        return Integer.parseInt(param.toString());
    }
}
