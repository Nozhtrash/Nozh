package dev.nozh.core.compat;

import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.preset.ModpackProfile;
import dev.nozh.core.preset.ModpackRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates known modpacks and provides optimal configurations.
 */
public final class ModpackValidator {
    public Optional<ModpackProfile> detectModpack() {
        return ModpackRegistry.detect();
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
        if (profile.configProfile() != null) {
            config.optimizationProfile = profile.configProfile().name();
        }
        config.targetFps = profile.targetFps();
        
        // Note: NozhConfig doesn't have aggressivenessLevel, buildingScenarioThreshold, 
        // or combatScenarioThreshold fields, so we skip those for now
        // These would need to be added to NozhConfig if needed
        
        return config;
    }
    
    public record ValidationResult(
        String modpackName,
        boolean isValid,
        List<String> issues,
        List<String> suggestions
    ) {}
}
