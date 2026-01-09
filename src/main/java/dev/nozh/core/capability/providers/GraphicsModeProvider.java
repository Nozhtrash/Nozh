package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;

public class GraphicsModeProvider implements CapabilityProvider {
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        GraphicsMode oldValue = options.getGraphicsMode().getValue();
        
        GraphicsMode targetValue;
        if (params != null && params.length > 0 && params[0] instanceof GraphicsMode) {
            targetValue = (GraphicsMode) params[0];
        } else {
            targetValue = GraphicsMode.FAST;
        }
        
        if (targetValue == oldValue) {
            return ActionResult.success(StateSnapshot.single("graphics_mode", oldValue));
        }
        
        StateSnapshot snapshot = StateSnapshot.single("graphics_mode", oldValue);
        
        try {
            options.getGraphicsMode().setValue(targetValue);
            options.write();
            NozhConstants.LOGGER.info("Graphics mode changed: {} -> {}", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set graphics mode", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("graphics_mode")) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        
        try {
            Object oldValue = snapshot.get("graphics_mode");
            if (oldValue instanceof GraphicsMode mode) {
                client.options.getGraphicsMode().setValue(mode);
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back graphics mode to: {}", mode);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for graphics mode", e);
        }
    }
}
