package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;

public class GraphicsModeProvider implements CapabilityProvider {
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing graphics mode parameter");
        }
        
        try {
            GraphicsMode targetMode = parseMode(params[0]);
            
            GameOptions options = client.options;
            GraphicsMode oldMode = options.getGraphicsMode().getValue();
            
            if (oldMode == targetMode) {
                return ActionResult.noChange("Already using " + targetMode);
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("graphics_mode", oldMode);
            
            options.getGraphicsMode().setValue(targetMode);
            options.write();
            
            // Graphics mode change requires chunk reload
            if (client.worldRenderer != null) {
                client.worldRenderer.reload();
            }
            
            NozhConstants.LOGGER.info("Changed graphics mode: {} -> {}", oldMode, targetMode);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set graphics mode", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("graphics_mode")) {
            throw new IllegalArgumentException("Invalid snapshot for graphics mode");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        GraphicsMode oldMode = (GraphicsMode) snapshot.get("graphics_mode");
        
        client.options.getGraphicsMode().setValue(oldMode);
        client.options.write();
        
        if (client.worldRenderer != null) {
            client.worldRenderer.reload();
        }
        
        NozhConstants.LOGGER.info("Rolled back graphics mode to: {}", oldMode);
    }
    
    private GraphicsMode parseMode(Object param) {
        if (param instanceof GraphicsMode) {
            return (GraphicsMode) param;
        }
        
        String str = param.toString().toUpperCase();
        
        try {
            return GraphicsMode.valueOf(str);
        } catch (IllegalArgumentException e) {
            // Fuzzy matching
            if (str.contains("FANCY")) return GraphicsMode.FANCY;
            if (str.contains("FAST")) return GraphicsMode.FAST;
            if (str.contains("FABULOUS")) return GraphicsMode.FABULOUS;
            
            throw new IllegalArgumentException("Unknown graphics mode: " + param);
        }
    }
}
