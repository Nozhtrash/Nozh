package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.OptimizationProvider;
import net.minecraft.client.MinecraftClient;

/**
 * Provider for disabling entity shadows.
 * This is a quick GPU optimization that has minimal visual impact.
 */
public final class EntityShadowsProvider implements OptimizationProvider {

    private static final String ID = "entity_shadows";
    private static final String NAME = "Entity Shadows";
    private static final String DESCRIPTION = "Disables shadow rendering for entities";
    
    private boolean previousState = true;

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
        // Can only execute if shadows are currently ON
        return client.options.getEntityShadows().getValue();
    }

    @Override
    public boolean execute() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                NozhConstants.LOGGER.warn("[EntityShadowsProvider] Client or options null");
                return false;
            }

            var entityShadowsOption = client.options.getEntityShadows();
            previousState = entityShadowsOption.getValue();
            
            if (!previousState) {
                NozhConstants.LOGGER.debug("[EntityShadowsProvider] Shadows already disabled");
                return true;
            }

            entityShadowsOption.setValue(false);
            NozhConstants.LOGGER.info("[EntityShadowsProvider] Disabled entity shadows");
            
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[EntityShadowsProvider] Failed to execute", e);
            return false;
        }
    }

    @Override
    public double getExpectedFpsImpact() {
        return 2.0; // Estimated +2 FPS
    }

    @Override
    public String getCategory() {
        return "rendering";
    }

    @Override
    public boolean isReversible() {
        return true;
    }
    
    public boolean getPreviousState() {
        return previousState;
    }
}
