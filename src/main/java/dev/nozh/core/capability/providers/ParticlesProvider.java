package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.CapabilityProvider;
import dev.nozh.core.capability.StateSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;

/**
 * Provider for particle rendering adjustments.
 * HIGH IMPACT provider - particles can significantly affect performance.
 */
public class ParticlesProvider implements CapabilityProvider {
    
    @Override
    public boolean canExecute(MinecraftClient client) {
        return client != null && client.options != null;
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public StateSnapshot execute(MinecraftClient client, Object targetValue) {
        if (!canExecute(client)) {
            return null;
        }
        
        if (!(targetValue instanceof ParticlesMode)) {
            NozhConstants.LOGGER.error("Invalid particle mode: {}", targetValue);
            return null;
        }
        
        try {
            ParticlesMode newMode = (ParticlesMode) targetValue;
            ParticlesMode oldMode = client.options.getParticles().getValue();
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("particles", oldMode);
            
            client.options.getParticles().setValue(newMode);
            client.options.write();
            
            NozhConstants.LOGGER.info("Particles: {} -> {}", oldMode, newMode);
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to change particles", e);
            return null;
        }
    }
    
    @Override
    public boolean rollback(MinecraftClient client, StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("particles")) {
            return false;
        }
        
        try {
            ParticlesMode oldMode = (ParticlesMode) snapshot.get("particles");
            client.options.getParticles().setValue(oldMode);
            client.options.write();
            return true;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Particles rollback failed", e);
            return false;
        }
    }
    
    @Override
    public String getProviderId() {
        return "particles";
    }
    
    @Override
    public String getDescription() {
        return "Controls particle rendering (ALL/DECREASED/MINIMAL)";
    }
}