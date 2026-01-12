package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.OptimizationProvider;
import net.minecraft.client.MinecraftClient;

/**
 * Provider for reducing render distance.
 * This is a significant optimization that reduces both CPU and GPU load.
 */
public final class RenderDistanceProvider implements OptimizationProvider {

    private static final String ID = "render_distance";
    private static final String NAME = "Render Distance";
    private static final String DESCRIPTION = "Reduces render distance to improve performance";
    private static final int MIN_RENDER_DISTANCE = 4;
    private static final int REDUCTION_STEP = 2;
    
    private int previousDistance = -1;

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
        int current = client.options.getViewDistance().getValue();
        return current > MIN_RENDER_DISTANCE;
    }

    @Override
    public boolean execute() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                NozhConstants.LOGGER.warn("[RenderDistanceProvider] Client or options null");
                return false;
            }

            var viewDistanceOption = client.options.getViewDistance();
            previousDistance = viewDistanceOption.getValue();
            
            if (previousDistance <= MIN_RENDER_DISTANCE) {
                NozhConstants.LOGGER.debug("[RenderDistanceProvider] Already at minimum ({} chunks)", previousDistance);
                return true;
            }

            int newDistance = Math.max(MIN_RENDER_DISTANCE, previousDistance - REDUCTION_STEP);
            viewDistanceOption.setValue(newDistance);
            
            NozhConstants.LOGGER.info("[RenderDistanceProvider] Reduced render distance from {} to {} chunks", 
                previousDistance, newDistance);
            
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[RenderDistanceProvider] Failed to execute", e);
            return false;
        }
    }

    @Override
    public double getExpectedFpsImpact() {
        return 15.0; // Estimated +15 FPS (significant impact)
    }

    @Override
    public String getCategory() {
        return "rendering";
    }

    @Override
    public boolean isReversible() {
        return true;
    }
    
    public int getPreviousDistance() {
        return previousDistance;
    }
}
