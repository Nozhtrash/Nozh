package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.OptimizationProvider;
import net.minecraft.client.MinecraftClient;

/**
 * Provider for reducing entity render distance.
 * This reduces CPU load from entity processing while keeping terrain visible.
 */
public final class EntityDistanceProvider implements OptimizationProvider {

    private static final String ID = "entity_distance";
    private static final String NAME = "Entity Render Distance";
    private static final String DESCRIPTION = "Reduces the distance at which entities are rendered";
    private static final double MIN_ENTITY_DISTANCE = 0.5; // 50% of view distance
    private static final double REDUCTION_STEP = 0.25;
    
    private double previousDistance = 1.0;

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
        double current = client.options.getEntityDistanceScaling().getValue();
        return current > MIN_ENTITY_DISTANCE;
    }

    @Override
    public boolean execute() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                NozhConstants.LOGGER.warn("[EntityDistanceProvider] Client or options null");
                return false;
            }

            var entityDistanceOption = client.options.getEntityDistanceScaling();
            previousDistance = entityDistanceOption.getValue();
            
            if (previousDistance <= MIN_ENTITY_DISTANCE) {
                NozhConstants.LOGGER.debug("[EntityDistanceProvider] Already at minimum ({}%)", 
                    (int)(previousDistance * 100));
                return true;
            }

            double newDistance = Math.max(MIN_ENTITY_DISTANCE, previousDistance - REDUCTION_STEP);
            entityDistanceOption.setValue(newDistance);
            
            NozhConstants.LOGGER.info("[EntityDistanceProvider] Reduced entity distance from {}% to {}%", 
                (int)(previousDistance * 100), (int)(newDistance * 100));
            
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[EntityDistanceProvider] Failed to execute", e);
            return false;
        }
    }

    @Override
    public double getExpectedFpsImpact() {
        return 8.0; // Estimated +8 FPS (significant for entity-heavy areas)
    }

    @Override
    public String getCategory() {
        return "entities";
    }

    @Override
    public boolean isReversible() {
        return true;
    }
    
    public double getPreviousDistance() {
        return previousDistance;
    }
}
