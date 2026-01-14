package dev.nozh.core.potato;

import dev.nozh.core.potato.PotatoModeEngine.PotatoConfig;

/**
 * Interface for applying Potato Mode configurations.
 * Decouples core engine from Minecraft implementation details.
 */
public interface PotatoConfigApplicator {

    /**
     * Apply the given configuration to the game settings.
     * 
     * @param config The configuration to apply
     */
    void apply(PotatoConfig config);

    /**
     * Check if the applicator is ready/valid.
     */
    boolean isAvailable();
}
