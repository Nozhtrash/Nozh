package dev.nozh.core.director;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Locale;
import java.util.Set;

/**
 * v0.2: DirectorMode V2 - Known mod detection and dynamic hints.
 *
 * <p>Goal: identify major optimization stacks (Sodium/Lithium/Iris) and adjust heuristics.
 * This class is deliberately lightweight and deterministic.</p>
 */
public final class DirectorModeV2 {

    private static final Set<String> SODIUM_STACK = Set.of(
            "sodium",
            "sodium-extra",
            "reeses-sodium-options",
            "indium"
    );

    private static final Set<String> IRIS_STACK = Set.of(
            "iris"
    );

    private static final Set<String> LITHIUM_STACK = Set.of(
            "lithium"
    );

    private final FabricLoader loader;

    public DirectorModeV2() {
        this(FabricLoader.getInstance());
    }

    public DirectorModeV2(FabricLoader loader) {
        if (loader == null) throw new NullPointerException("loader");
        this.loader = loader;
    }

    public DirectorBiasHints computeBiasHints() {
        boolean hasSodium = anyLoaded(SODIUM_STACK);
        boolean hasIris = anyLoaded(IRIS_STACK);
        boolean hasLithium = anyLoaded(LITHIUM_STACK);

        ModEcosystem eco;
        if (hasSodium && hasIris) {
            eco = ModEcosystem.IRIS_STACK;
        } else if (hasSodium) {
            eco = ModEcosystem.SODIUM_STACK;
        } else if (hasLithium) {
            eco = ModEcosystem.LITHIUM_STACK;
        } else {
            eco = ModEcosystem.VANILLA;
        }

        // Bias rules:
        // - Sodium stack typically shifts load toward GPU; slightly positive.
        // - Lithium tends to reduce CPU pressure; slightly GPU-biased too.
        double bias = 0.0;
        switch (eco) {
            case IRIS_STACK -> bias = 0.25; // shaders often GPU
            case SODIUM_STACK -> bias = 0.15;
            case LITHIUM_STACK -> bias = 0.10;
            default -> bias = 0.0;
        }

        return new DirectorBiasHints(bias, eco);
    }

    private boolean anyLoaded(Set<String> ids) {
        for (String id : ids) {
            if (loader.isModLoaded(id.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
