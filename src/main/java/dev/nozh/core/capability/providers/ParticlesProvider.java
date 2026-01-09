package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.ActionResult;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;

public class ParticlesProvider implements CapabilityProvider {
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (client == null || client.options == null) {
            return ActionResult.error("Client or options not available");
        }
        
        GameOptions options = client.options;
        ParticlesMode oldValue = options.getParticles().getValue();
        
        ParticlesMode targetValue;
        if (params != null && params.length > 0 && params[0] instanceof ParticlesMode) {
            targetValue = (ParticlesMode) params[0];
        } else {
            targetValue = switch (oldValue) {
                case ALL -> ParticlesMode.DECREASED;
                case DECREASED -> ParticlesMode.MINIMAL;
                case MINIMAL -> ParticlesMode.MINIMAL;
            };
        }
        
        if (targetValue == oldValue) {
            return ActionResult.success(StateSnapshot.single("particles", oldValue));
        }
        
        StateSnapshot snapshot = StateSnapshot.single("particles", oldValue);
        
        try {
            options.getParticles().setValue(targetValue);
            options.write();
            NozhConstants.LOGGER.info("Particles changed: {} -> {}", oldValue, targetValue);
            return ActionResult.success(snapshot);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set particles", e);
            return ActionResult.error("Failed to apply: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("particles")) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        
        try {
            Object oldValue = snapshot.get("particles");
            if (oldValue instanceof ParticlesMode mode) {
                client.options.getParticles().setValue(mode);
                client.options.write();
                NozhConstants.LOGGER.info("Rolled back particles to: {}", mode);
            }
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Rollback failed for particles", e);
        }
    }
}
