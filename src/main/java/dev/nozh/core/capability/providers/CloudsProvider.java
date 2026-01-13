package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.OptimizationProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;

/**
 * Provider for disabling cloud rendering.
 * This is a GPU optimization that can improve FPS especially on lower-end systems.
 */
public final class CloudsProvider implements OptimizationProvider {

    private static final String ID = "clouds";
    private static final String NAME = "Cloud Rendering";
    private static final String DESCRIPTION = "Disables or reduces cloud rendering";
    
    private CloudRenderMode previousMode = null;

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
        CloudRenderMode current = client.options.getCloudRenderMode().getValue();
        return current != CloudRenderMode.OFF;
    }

    @Override
    public boolean execute() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                NozhConstants.LOGGER.warn("[CloudsProvider] Client or options null");
                return false;
            }

            var cloudsOption = client.options.getCloudRenderMode();
            previousMode = cloudsOption.getValue();
            
            // Step down one level
            CloudRenderMode newMode;
            if (previousMode == CloudRenderMode.FANCY) {
                newMode = CloudRenderMode.FAST;
            } else if (previousMode == CloudRenderMode.FAST) {
                newMode = CloudRenderMode.OFF;
            } else {
                NozhConstants.LOGGER.debug("[CloudsProvider] Already at OFF");
                return true;
            }

            cloudsOption.setValue(newMode);
            NozhConstants.LOGGER.info("[CloudsProvider] Changed clouds from {} to {}", previousMode, newMode);
            
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[CloudsProvider] Failed to execute", e);
            return false;
        }
    }

    @Override
    public double getExpectedFpsImpact() {
        return 3.0; // Estimated +3 FPS
    }

    @Override
    public String getCategory() {
        return "rendering";
    }

    @Override
    public boolean isReversible() {
        return true;
    }
    
    public CloudRenderMode getPreviousMode() {
        return previousMode;
    }
}
