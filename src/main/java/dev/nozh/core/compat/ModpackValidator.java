package dev.nozh.core.compat;

import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.config.OptimizationProfile;
import net.fabricmc.loader.api.FabricLoader;
import java.util.*;

/**
 * Validates known modpacks and provides optimal configurations.
 */
public final class ModpackValidator {
    private final Map<String, ModpackProfile> knownModpacks;
    
    public ModpackValidator() {
        this.knownModpacks = initializeModpackProfiles();
    }
    
    public Optional<ModpackProfile> detectModpack() {
        var loader = FabricLoader.getInstance();
        
        for (var profile : knownModpacks.values()) {
            boolean allSignaturesPresent = profile.signatureMods().stream()
                .allMatch(loader::isModLoaded);
            
            if (allSignaturesPresent) {
                return Optional.of(profile);
            }
        }
        
        return Optional.empty();
    }
    
    public ValidationResult validate(ModpackProfile profile) {
        var loader = FabricLoader.getInstance();
        var issues = new ArrayList<String>();
        var suggestions = new ArrayList<String>();
        
        // Check required mods
        for (var requiredMod : profile.requiredMods()) {
            if (!loader.isModLoaded(requiredMod)) {
                issues.add("Missing required mod: " + requiredMod);
            }
        }
        
        // Check recommended mods
        for (var recommendedMod : profile.recommendedMods()) {
            if (!loader.isModLoaded(recommendedMod)) {
                suggestions.add("Consider installing: " + recommendedMod);
            }
        }
        
        // Check incompatible mods
        for (var incompatibleMod : profile.incompatibleMods()) {
            if (loader.isModLoaded(incompatibleMod)) {
                issues.add("Incompatible mod detected: " + incompatibleMod);
            }
        }
        
        boolean isValid = issues.isEmpty();
        return new ValidationResult(profile.name(), isValid, issues, suggestions);
    }
    
    public NozhConfig getOptimalConfig(ModpackProfile profile) {
        var config = new NozhConfig();
        
        // Apply profile-specific settings (convert enum to String)
        config.optimizationProfile = profile.recommendedProfile().name();
        config.targetFps = profile.targetFps();
        
        // Note: NozhConfig doesn't have aggressivenessLevel, buildingScenarioThreshold, 
        // or combatScenarioThreshold fields, so we skip those for now
        // These would need to be added to NozhConfig if needed
        
        return config;
    }
    
    private Map<String, ModpackProfile> initializeModpackProfiles() {
        var profiles = new HashMap<String, ModpackProfile>();
        
        // All of Fabric (AOF)
        profiles.put("aof", new ModpackProfile(
            "All of Fabric",
            ModpackType.KITCHEN_SINK,
            List.of("fabric-api", "roughly-enough-items"),
            List.of("sodium", "lithium"),
            List.of("iris", "lambdynamiclights"),
            List.of("optifabric"),
            OptimizationProfile.BALANCED,
            60,
            0.6
        ));
        
        // Better Minecraft (BMC)
        profiles.put("bmc", new ModpackProfile(
            "Better Minecraft",
            ModpackType.ADVENTURE,
            List.of("fabric-api", "betterend", "betternether"),
            List.of("sodium", "lithium", "starlight"),
            List.of("iris"),
            List.of("phosphor"),
            OptimizationProfile.BALANCED,
            60,
            0.5
        ));
        
        // Create: Fabric
        profiles.put("create", new ModpackProfile(
            "Create Fabric",
            ModpackType.TECH,
            List.of("fabric-api", "create"),
            List.of("sodium", "lithium", "ferritecore"),
            List.of("iris"),
            List.of(),
            OptimizationProfile.AGGRESSIVE,
            50,
            0.7
        ));
        
        // Fabulously Optimized
        profiles.put("fabulously-optimized", new ModpackProfile(
            "Fabulously Optimized",
            ModpackType.PERFORMANCE,
            List.of("fabric-api", "sodium", "lithium", "iris"),
            List.of("entityculling", "ferritecore"),
            List.of(),
            List.of("optifabric", "phosphor"),
            OptimizationProfile.CONSERVATIVE,
            144,
            0.3
        ));
        
        return profiles;
    }
    
    public record ModpackProfile(
        String name,
        ModpackType type,
        List<String> signatureMods,
        List<String> requiredMods,
        List<String> recommendedMods,
        List<String> incompatibleMods,
        OptimizationProfile recommendedProfile,
        int targetFps,
        double aggressiveness
    ) {}
    
    public record ValidationResult(
        String modpackName,
        boolean isValid,
        List<String> issues,
        List<String> suggestions
    ) {}
    
    public enum ModpackType {
        PERFORMANCE,
        ADVENTURE,
        TECH,
        KITCHEN_SINK,
        VANILLA_PLUS
    }
}
