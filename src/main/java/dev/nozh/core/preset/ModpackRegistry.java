package dev.nozh.core.preset;

import dev.nozh.core.config.OptimizationProfile;
import dev.nozh.core.matrix.ActionMatrixTuning;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModpackRegistry {

    private static final Map<String, ModpackProfile> PROFILES = initializeProfiles();

    private ModpackRegistry() {
    }

    public static Optional<ModpackProfile> detect() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            for (ModpackProfile profile : PROFILES.values()) {
                boolean allSignaturesPresent = profile.signatureMods().stream()
                        .allMatch(loader::isModLoaded);
                if (allSignaturesPresent) {
                    return Optional.of(profile);
                }
            }
        } catch (Throwable ignored) {
            // FabricLoader not available (tests/server)
        }
        return Optional.empty();
    }

    public static Map<String, ModpackProfile> profiles() {
        return PROFILES;
    }

    private static Map<String, ModpackProfile> initializeProfiles() {
        Map<String, ModpackProfile> profiles = new HashMap<>();

        profiles.put("aof", new ModpackProfile(
                "aof",
                "All of Fabric",
                ModpackType.KITCHEN_SINK,
                List.of("fabric-api", "roughly-enough-items"),
                List.of("sodium", "lithium"),
                List.of("iris", "lambdynamiclights"),
                List.of("optifabric"),
                OptimizationProfile.BALANCED,
                dev.nozh.core.config.OptimizationProfile.BALANCED,
                60,
                new ActionMatrixTuning(0.05, 1.2, 1.1, 12, 8, 90, 120)
        ));

        profiles.put("bmc", new ModpackProfile(
                "bmc",
                "Better Minecraft",
                ModpackType.ADVENTURE,
                List.of("fabric-api", "betterend", "betternether"),
                List.of("sodium", "lithium", "starlight"),
                List.of("iris"),
                List.of("phosphor"),
                OptimizationProfile.BALANCED,
                dev.nozh.core.config.OptimizationProfile.BALANCED,
                60,
                new ActionMatrixTuning(0.03, 1.15, 1.05, 14, 9, 100, 120)
        ));

        profiles.put("create", new ModpackProfile(
                "create",
                "Create Fabric",
                ModpackType.TECH,
                List.of("fabric-api", "create"),
                List.of("sodium", "lithium", "ferritecore"),
                List.of("iris"),
                List.of(),
                OptimizationProfile.AGGRESSIVE,
                dev.nozh.core.config.OptimizationProfile.AGGRESSIVE,
                50,
                new ActionMatrixTuning(0.05, 1.2, 1.1, 12, 8, 90, 90)
        ));

        profiles.put("fabulously-optimized", new ModpackProfile(
                "fabulously-optimized",
                "Fabulously Optimized",
                ModpackType.PERFORMANCE,
                List.of("fabric-api", "sodium", "lithium", "iris"),
                List.of("entityculling", "ferritecore"),
                List.of(),
                List.of("optifabric", "phosphor"),
                OptimizationProfile.BALANCED,
                dev.nozh.core.config.OptimizationProfile.BALANCED,
                144,
                new ActionMatrixTuning(-0.01, 0.9, 0.9, Integer.MAX_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
        ));

        return profiles;
    }
}
