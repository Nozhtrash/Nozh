package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;

public class ParticlesProvider implements CapabilityProvider {
    
    @Override
    public ActionResult execute(MinecraftClient client, Object... params) {
        if (params.length == 0) {
            return ActionResult.error("Missing particles mode parameter");
        }
        
        try {
            ParticlesMode targetMode = parseMode(params[0]);
            
            GameOptions options = client.options;
            ParticlesMode oldMode = options.getParticles().getValue();
            
            if (oldMode == targetMode) {
                return ActionResult.noChange("Already at " + targetMode);
            }
            
            StateSnapshot snapshot = new StateSnapshot();
            snapshot.put("particles", oldMode);
            
            options.getParticles().setValue(targetMode);
            options.write();
            
            NozhConstants.LOGGER.info("Changed particles: {} -> {}", oldMode, targetMode);
            
            return ActionResult.success(snapshot);
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set particles", e);
            return ActionResult.error(e.getMessage());
        }
    }
    
    @Override
    public boolean canRollback() {
        return true;
    }
    
    @Override
    public void rollback(StateSnapshot snapshot) {
        if (snapshot == null || !snapshot.has("particles")) {
            throw new IllegalArgumentException("Invalid snapshot for particles");
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        ParticlesMode oldMode = (ParticlesMode) snapshot.get("particles");
        
        client.options.getParticles().setValue(oldMode);
        client.options.write();
        
        NozhConstants.LOGGER.info("Rolled back particles to: {}", oldMode);
    }
    
    private ParticlesMode parseMode(Object param) {
        if (param instanceof ParticlesMode) {
            return (ParticlesMode) param;
        }
        
        String str = param.toString().toUpperCase();
        
        // Try direct match
        try {
            return ParticlesMode.valueOf(str);
        } catch (IllegalArgumentException e) {
            // Try fuzzy match
            if (str.contains("ALL") || str.contains("FULL")) {
                return ParticlesMode.ALL;
            }
            if (str.contains("DECREASE") || str.contains("REDUCE")) {
                return ParticlesMode.DECREASED;
            }
            if (str.contains("MINIMAL") || str.contains("MIN")) {
                return ParticlesMode.MINIMAL;
            }
            
            throw new IllegalArgumentException("Unknown particles mode: " + param);
        }
    }
}
