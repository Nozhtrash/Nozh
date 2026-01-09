package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class MipmapLevelsProvider implements CapabilityProvider {
    
    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 4;
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing mipmap level");
        }
        
        try {
            int targetLevel = parseLevel(params[0]);
            
            if (targetLevel < MIN_LEVEL || targetLevel > MAX_LEVEL) {
                return ActionResult.invalid(
                    String.format("Mipmap level must be %d-%d, got %d", 
                                  MIN_LEVEL, MAX_LEVEL, targetLevel)
                );
            }
            
            GameOptions options = client.options;
            int oldLevel = options.getMipmapLevels().getValue();
            
            if (oldLevel == targetLevel) {
                return ActionResult.noChange("Already at level " + targetLevel);
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("mipmap_levels", oldLevel);
            
            options.getMipmapLevels().setValue(targetLevel);
            options.write();
            
            // Reload textures
            if (client.getTextureManager() != null) {
                client.reloadResources();
            }
            
            NozhConstants.LOGGER.info("Changed mipmap levels: {} -> {}", oldLevel, targetLevel);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set mipmap levels", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("mipmap_levels")) {
            throw new IllegalArgumentException("Invalid snapshot");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        int oldLevel = (int) snapshot.get("mipmap_levels");
        
        client.options.getMipmapLevels().setValue(oldLevel);
        client.options.write();
        client.reloadResources();
        
        NozhConstants.LOGGER.info("Rolled back mipmap levels to: {}", oldLevel);
    }
    
    private int parseLevel(Object param) {
        if (param instanceof Integer) return (Integer) param;
        if (param instanceof Number) return ((Number) param).intValue();
        return Integer.parseInt(param.toString());
    }
}
