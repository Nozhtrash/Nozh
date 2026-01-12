package dev.nozh.core.capability.providers;

import dev.nozh.NozhConstants;
import dev.nozh.core.capability.OptimizationProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;

/**
 * Provider for reducing graphics quality.
 * This switches from Fabulous/Fancy to Fast graphics mode.
 */
public final class GraphicsModeProvider implements OptimizationProvider {

    private static final String ID = "graphics_mode";
    private static final String NAME = "Graphics Quality";
    private static final String DESCRIPTION = "Reduces graphics quality from Fancy/Fabulous to Fast";
    
    private GraphicsMode previousMode = null;

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
        GraphicsMode current = client.options.getGraphicsMode().getValue();
        return current != GraphicsMode.FAST;
    }

    @Override
    public boolean execute() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                NozhConstants.LOGGER.warn("[GraphicsModeProvider] Client or options null");
                return false;
            }

            var graphicsOption = client.options.getGraphicsMode();
            previousMode = graphicsOption.getValue();
            
            GraphicsMode newMode;
            if (previousMode == GraphicsMode.FABULOUS) {
                newMode = GraphicsMode.FANCY;
            } else if (previousMode == GraphicsMode.FANCY) {
                newMode = GraphicsMode.FAST;
            } else {
                NozhConstants.LOGGER.debug("[GraphicsModeProvider] Already at FAST");
                return true;
            }

            graphicsOption.setValue(newMode);
            NozhConstants.LOGGER.info("[GraphicsModeProvider] Changed graphics from {} to {}", previousMode, newMode);
            
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("[GraphicsModeProvider] Failed to execute", e);
            return false;
        }
    }

    @Override
    public double getExpectedFpsImpact() {
        return 10.0; // Estimated +10 FPS
    }

    @Override
    public String getCategory() {
        return "rendering";
    }

    @Override
    public boolean isReversible() {
        return true;
    }
    
    public GraphicsMode getPreviousMode() {
        return previousMode;
    }
}
