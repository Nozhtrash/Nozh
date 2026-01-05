package dev.nozh.core.compat;

import dev.nozh.core.capability.CapabilityId;
import net.fabricmc.loader.api.FabricLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive compatibility matrix for mod ecosystem analysis.
 * Detects conflicts, provides recommendations, and validates configurations.
 */
public final class CompatMatrix {
    private final Map<String, ModProfile> knownMods;
    private final List<ConflictRule> conflictRules;
    private final Map<String, List<CapabilityId>> modCapabilityMap;
    
    public CompatMatrix() {
        this.knownMods = initializeKnownMods();
        this.conflictRules = initializeConflictRules();
        this.modCapabilityMap = initializeCapabilityMap();
    }
    
    public CompatAnalysis analyze() {
        var loader = FabricLoader.getInstance();
        var loadedMods = new ArrayList<LoadedMod>();
        var conflicts = new ArrayList<DetectedConflict>();
        var recommendations = new ArrayList<String>();
        
        // Detect loaded mods
        for (var modId : knownMods.keySet()) {
            if (loader.isModLoaded(modId)) {
                var profile = knownMods.get(modId);
                var version = loader.getModContainer(modId)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
                loadedMods.add(new LoadedMod(modId, profile.name(), version, profile.category()));
            }
        }
        
        // Check for conflicts
        for (var rule : conflictRules) {
            if (loader.isModLoaded(rule.mod1()) && loader.isModLoaded(rule.mod2())) {
                var severity = rule.severity();
                conflicts.add(new DetectedConflict(
                    rule.mod1(),
                    rule.mod2(),
                    rule.description(),
                    severity,
                    rule.resolution()
                ));
                
                if (severity == ConflictSeverity.HIGH || severity == ConflictSeverity.CRITICAL) {
                    recommendations.add(rule.resolution());
                }
            }
        }
        
        // Capability stewardship analysis
        var stewardshipConflicts = analyzeCapabilityStewardship(loadedMods);
        conflicts.addAll(stewardshipConflicts);
        
        // Generate recommendations
        if (!conflicts.isEmpty()) {
            recommendations.add("Run '/nozh selfcheck' to see full compatibility report");
        }
        
        if (loadedMods.stream().anyMatch(m -> m.category() == ModCategory.SHADER) &&
            loadedMods.stream().noneMatch(m -> m.modId().equals("iris"))) {
            recommendations.add("Consider installing Iris for better shader compatibility");
        }
        
        return new CompatAnalysis(
            loadedMods,
            conflicts,
            recommendations,
            calculateCompatScore(loadedMods, conflicts)
        );
    }
    
    private List<DetectedConflict> analyzeCapabilityStewardship(List<LoadedMod> loadedMods) {
        var conflicts = new ArrayList<DetectedConflict>();
        var capabilityOwnership = new HashMap<CapabilityId, List<String>>();
        
        // Map which mods manage which capabilities
        for (var mod : loadedMods) {
            var caps = modCapabilityMap.getOrDefault(mod.modId(), List.of());
            for (var cap : caps) {
                capabilityOwnership.computeIfAbsent(cap, k -> new ArrayList<>())
                    .add(mod.modId());
            }
        }
        
        // Detect multiple stewards for same capability
        for (var entry : capabilityOwnership.entrySet()) {
            if (entry.getValue().size() > 1) {
                var cap = entry.getKey();
                var mods = entry.getValue();
                conflicts.add(new DetectedConflict(
                    mods.get(0),
                    mods.get(1),
                    "Both mods manage " + cap.name() + " setting",
                    ConflictSeverity.MEDIUM,
                    "NOZH will defer to " + mods.get(0) + " for " + cap.name()
                ));
            }
        }
        
        return conflicts;
    }
    
    private double calculateCompatScore(List<LoadedMod> mods, List<DetectedConflict> conflicts) {
        if (mods.isEmpty()) return 100.0;
        
        double baseScore = 100.0;
        for (var conflict : conflicts) {
            baseScore -= switch (conflict.severity()) {
                case LOW -> 2.0;
                case MEDIUM -> 5.0;
                case HIGH -> 10.0;
                case CRITICAL -> 25.0;
            };
        }
        
        return Math.max(0.0, baseScore);
    }
    
    private Map<String, ModProfile> initializeKnownMods() {
        var mods = new HashMap<String, ModProfile>();
        
        // Performance mods
        mods.put("sodium", new ModProfile("Sodium", ModCategory.PERFORMANCE, "Rendering optimization"));
        mods.put("lithium", new ModProfile("Lithium", ModCategory.PERFORMANCE, "General optimization"));
        mods.put("phosphor", new ModProfile("Phosphor", ModCategory.PERFORMANCE, "Lighting optimization"));
        mods.put("starlight", new ModProfile("Starlight", ModCategory.PERFORMANCE, "Lighting engine"));
        mods.put("ferritecore", new ModProfile("FerriteCore", ModCategory.PERFORMANCE, "Memory optimization"));
        mods.put("lazydfu", new ModProfile("LazyDFU", ModCategory.PERFORMANCE, "Startup optimization"));
        
        // Shader mods
        mods.put("iris", new ModProfile("Iris", ModCategory.SHADER, "Shader support"));
        mods.put("optifabric", new ModProfile("OptiFabric", ModCategory.SHADER, "OptiFine compatibility"));
        
        // Visual mods
        mods.put("lambdynamiclights", new ModProfile("LambDynamicLights", ModCategory.VISUAL, "Dynamic lighting"));
        mods.put("lambdabettergrass", new ModProfile("LambdaBetterGrass", ModCategory.VISUAL, "Better grass"));
        
        return mods;
    }
    
    private List<ConflictRule> initializeConflictRules() {
        var rules = new ArrayList<ConflictRule>();
        
        // Critical conflicts
        rules.add(new ConflictRule(
            "phosphor", "starlight",
            "Both mods modify lighting engine",
            ConflictSeverity.CRITICAL,
            "Remove Phosphor (Starlight is newer and faster)"
        ));
        
        rules.add(new ConflictRule(
            "optifabric", "sodium",
            "OptiFabric and Sodium are incompatible",
            ConflictSeverity.CRITICAL,
            "Choose either OptiFine+OptiFabric OR Sodium+Iris"
        ));
        
        // High severity
        rules.add(new ConflictRule(
            "sodium", "canvas",
            "Both mods replace rendering pipeline",
            ConflictSeverity.HIGH,
            "Use Sodium (Canvas is experimental)"
        ));
        
        // Medium severity
        rules.add(new ConflictRule(
            "sodium", "lambdynamiclights",
            "LambDynamicLights may have reduced performance with Sodium",
            ConflictSeverity.MEDIUM,
            "NOZH will coordinate settings between both mods"
        ));
        
        return rules;
    }
    
    private Map<String, List<CapabilityId>> initializeCapabilityMap() {
        var map = new HashMap<String, List<CapabilityId>>();
        
        map.put("sodium", List.of(
            CapabilityId.RENDER_DISTANCE,
            CapabilityId.CHUNK_UPDATE_THREADS,
            CapabilityId.VSYNC
        ));
        
        map.put("iris", List.of(
            CapabilityId.SHADER_QUALITY
        ));
        
        map.put("lambdynamiclights", List.of(
            CapabilityId.DYNAMIC_LIGHTING
        ));
        
        return map;
    }
    
    public record CompatAnalysis(
        List<LoadedMod> loadedMods,
        List<DetectedConflict> conflicts,
        List<String> recommendations,
        double compatibilityScore
    ) {
        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }
        
        public boolean hasCriticalConflicts() {
            return conflicts.stream()
                .anyMatch(c -> c.severity() == ConflictSeverity.CRITICAL);
        }
    }
    
    public record LoadedMod(
        String modId,
        String name,
        String version,
        ModCategory category
    ) {}
    
    public record DetectedConflict(
        String mod1,
        String mod2,
        String description,
        ConflictSeverity severity,
        String resolution
    ) {}
    
    record ModProfile(
        String name,
        ModCategory category,
        String description
    ) {}
    
    record ConflictRule(
        String mod1,
        String mod2,
        String description,
        ConflictSeverity severity,
        String resolution
    ) {}
    
    public enum ModCategory {
        PERFORMANCE,
        SHADER,
        VISUAL,
        GAMEPLAY,
        UTILITY
    }
    
    public enum ConflictSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}