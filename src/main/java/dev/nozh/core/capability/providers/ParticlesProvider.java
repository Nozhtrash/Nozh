package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.OptimizationProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;

/**
 * Provider for reducing particle effects.
 * This is a GPU optimization that can significantly improve FPS.
 */
public final class ParticlesProvider implements OptimizationProvider {

    private static final String ID = "particles";
    private static final String NAME = "Particle Effects";
    private static final String DESCRIPTION = "Reduces particle effects from All to Decreased or Minimal";
    
    private ParticlesMode previousMode = null;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public boolean canExecute() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return false;
        }
        // Can only reduce if not already at minimum
        ParticlesMode current = client.options.getParticles().getValue();
        return current != ParticlesMode.MINIMAL;
    }

    @Override
    public boolean execute() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                NozhConstants.LOGGER.warn("[ParticlesProvider] Client or options null");
                return false;
            }

            var particlesOption = client.options.getParticles();
            previousMode = particlesOption.getValue();
            
            // Step down one level
            ParticlesMode newMode;
            if (previousMode == ParticlesMode.ALL) {
                newMode = ParticlesMode.DECREASED;
            } else if (previousMode == ParticlesMode.DECREASED) {
                newMode = ParticlesMode.MINIMAL;
            } else {
                NozhConstants.LOGGER.debug("[ParticlesProvider] Already at MINIMAL");
                return true; // Already at minimum, consider it success
            }

            particlesOption.setValue(newMode);
            NozhConstants.LOGGER.info("[ParticlesProvider] Changed particles from {} to {}", previousMode, newMode);
            
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[ParticlesProvider] Failed to execute", e);
            return false;
        }
    }

    @Override
    public double getExpectedFpsImpact() {
        return 5.0; // Estimated +5 FPS
    }

    @Override
    public String getCategory() {
        return "rendering";
    }

    @Override
    public boolean isReversible() {
        return true;
    }
    
    /**
     * Get the mode before execution for rollback purposes.
     */
    public ParticlesMode getPreviousMode() {
        return previousMode;
    }
}
