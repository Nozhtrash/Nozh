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
        mods.put("sodium-extra", new ModProfile("Sodium Extra", ModCategory.PERFORMANCE, "Advanced render options"));
        mods.put("reeses-sodium-options", new ModProfile("Reese's Sodium Options", ModCategory.PERFORMANCE,
                "Extended Sodium controls"));
        mods.put("lithium", new ModProfile("Lithium", ModCategory.PERFORMANCE, "General optimization"));
        mods.put("phosphor", new ModProfile("Phosphor", ModCategory.PERFORMANCE, "Lighting optimization"));
        mods.put("starlight", new ModProfile("Starlight", ModCategory.PERFORMANCE, "Lighting engine"));
        mods.put("ferritecore", new ModProfile("FerriteCore", ModCategory.PERFORMANCE, "Memory optimization"));
        mods.put("modernfix", new ModProfile("ModernFix", ModCategory.PERFORMANCE, "Memory and loading fixes"));
        mods.put("krypton", new ModProfile("Krypton", ModCategory.PERFORMANCE, "Network optimization"));
        mods.put("c2me", new ModProfile("C2ME", ModCategory.PERFORMANCE, "Chunk parallelism"));
        mods.put("bobby", new ModProfile("Bobby", ModCategory.PERFORMANCE, "View distance caching"));
        mods.put("distant-horizons", new ModProfile("Distant Horizons", ModCategory.PERFORMANCE,
                "Far terrain rendering"));
        mods.put("nvidium", new ModProfile("Nvidium", ModCategory.PERFORMANCE, "GPU optimization"));
        mods.put("immediatelyfast", new ModProfile("ImmediatelyFast", ModCategory.PERFORMANCE, "Immediate rendering"));
        mods.put("exordium", new ModProfile("Exordium", ModCategory.PERFORMANCE, "GUI render throttling"));
        mods.put("entityculling", new ModProfile("Entity Culling", ModCategory.PERFORMANCE, "Entity culling"));
        mods.put("moreculling", new ModProfile("More Culling", ModCategory.PERFORMANCE, "Block/entity culling"));
        mods.put("cullleaves", new ModProfile("Cull Leaves", ModCategory.PERFORMANCE, "Leaf culling"));
        mods.put("enhancedblockentities", new ModProfile("Enhanced Block Entities", ModCategory.PERFORMANCE,
                "Block entity optimization"));
        mods.put("lambdynlights", new ModProfile("LambDynamicLights", ModCategory.PERFORMANCE, "Dynamic lighting"));
        mods.put("dynamic_fps", new ModProfile("Dynamic FPS", ModCategory.PERFORMANCE, "FPS throttling"));
        mods.put("fpsreducer", new ModProfile("FPS Reducer", ModCategory.PERFORMANCE, "FPS throttling"));
        mods.put("dashloader", new ModProfile("DashLoader", ModCategory.PERFORMANCE, "Asset caching"));
        mods.put("memoryleakfix", new ModProfile("Memory Leak Fix", ModCategory.PERFORMANCE, "Memory leak guard"));
        mods.put("lazydfu", new ModProfile("LazyDFU", ModCategory.PERFORMANCE, "Startup optimization"));
        mods.put("lazy-language-loader", new ModProfile("Lazy Language Loader", ModCategory.PERFORMANCE,
                "Language loading optimization"));
        mods.put("smoothboot", new ModProfile("Smooth Boot", ModCategory.PERFORMANCE, "Threaded loading"));
        mods.put("fastload", new ModProfile("Fastload", ModCategory.PERFORMANCE, "Fast asset loading"));
        mods.put("fastquit", new ModProfile("FastQuit", ModCategory.PERFORMANCE, "Faster exit"));
        mods.put("threadtweak", new ModProfile("ThreadTweak", ModCategory.PERFORMANCE, "Thread tuning"));
        mods.put("vmp", new ModProfile("VMP", ModCategory.PERFORMANCE, "Multiplayer performance"));
        mods.put("servercore", new ModProfile("ServerCore", ModCategory.PERFORMANCE, "Server tick tuning"));
        mods.put("debugify", new ModProfile("Debugify", ModCategory.PERFORMANCE, "Bugfix collection"));

        // Shader mods
        mods.put("iris", new ModProfile("Iris", ModCategory.SHADER, "Shader support"));
        mods.put("optifabric", new ModProfile("OptiFabric", ModCategory.SHADER, "OptiFine compatibility"));
        mods.put("canvas", new ModProfile("Canvas", ModCategory.SHADER, "Rendering pipeline replacement"));
        mods.put("vulkanmod", new ModProfile("VulkanMod", ModCategory.SHADER, "Vulkan renderer"));
        mods.put("indium", new ModProfile("Indium", ModCategory.SHADER, "FRAPI bridge"));

        // Visual mods
        mods.put("lambdabettergrass", new ModProfile("LambdaBetterGrass", ModCategory.VISUAL, "Better grass"));
        mods.put("continuity", new ModProfile("Continuity", ModCategory.VISUAL, "Connected textures"));
        mods.put("visuality", new ModProfile("Visuality", ModCategory.VISUAL, "Particles and effects"));
        mods.put("cullclouds", new ModProfile("Cull Clouds", ModCategory.VISUAL, "Cloud culling"));
        mods.put("betterclouds", new ModProfile("Better Clouds", ModCategory.VISUAL, "Improved clouds"));
        mods.put("animatica", new ModProfile("Animatica", ModCategory.VISUAL, "Animated textures"));
        mods.put("entity_texture_features", new ModProfile("Entity Texture Features", ModCategory.VISUAL,
                "Entity rendering tweaks"));
        mods.put("entity_model_features", new ModProfile("Entity Model Features", ModCategory.VISUAL,
                "Entity model tweaks"));
        mods.put("presencefootsteps", new ModProfile("Presence Footsteps", ModCategory.VISUAL, "Footstep effects"));
        mods.put("fabricskyboxes", new ModProfile("FabricSkyBoxes", ModCategory.VISUAL, "Skybox rendering"));
        mods.put("blur", new ModProfile("Blur", ModCategory.VISUAL, "UI blur effects"));

        // Gameplay / worldgen
        mods.put("terralith", new ModProfile("Terralith", ModCategory.GAMEPLAY, "World generation overhaul"));
        mods.put("tectonic", new ModProfile("Tectonic", ModCategory.GAMEPLAY, "Terrain generation"));
        mods.put("betterend", new ModProfile("BetterEnd", ModCategory.GAMEPLAY, "End expansion"));
        mods.put("betternether", new ModProfile("BetterNether", ModCategory.GAMEPLAY, "Nether expansion"));
        mods.put("bclib", new ModProfile("BCLib", ModCategory.GAMEPLAY, "Biome config library"));
        mods.put("yungsapi", new ModProfile("YUNG's API", ModCategory.GAMEPLAY, "Structure tooling"));

        // Utility / diagnostics
        mods.put("modmenu", new ModProfile("Mod Menu", ModCategory.UTILITY, "Mod list UI"));
        mods.put("cloth-config", new ModProfile("Cloth Config", ModCategory.UTILITY, "Config UI"));
        mods.put("yet_another_config_lib_v3", new ModProfile("YACL", ModCategory.UTILITY, "Config UI"));
        mods.put("spark", new ModProfile("spark", ModCategory.UTILITY, "Performance profiler"));
        mods.put("notenoughcrashes", new ModProfile("Not Enough Crashes", ModCategory.UTILITY, "Crash screens"));
        mods.put("neruina", new ModProfile("Neruina", ModCategory.UTILITY, "Crash prevention"));
        mods.put("observable", new ModProfile("Observable", ModCategory.UTILITY, "Telemetry tooling"));
        
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

        rules.add(new ConflictRule(
            "optifabric", "iris",
            "OptiFine and Iris cannot run together",
            ConflictSeverity.CRITICAL,
            "Use Iris for shaders when running Sodium-based stacks"
        ));
        
        // High severity
        rules.add(new ConflictRule(
            "sodium", "canvas",
            "Both mods replace rendering pipeline",
            ConflictSeverity.HIGH,
            "Use Sodium (Canvas is experimental)"
        ));

        rules.add(new ConflictRule(
            "iris", "canvas",
            "Canvas and Iris both replace shader pipeline",
            ConflictSeverity.HIGH,
            "Pick one shader pipeline (Iris recommended)"
        ));

        rules.add(new ConflictRule(
            "vulkanmod", "sodium",
            "Both mods replace core rendering backend",
            ConflictSeverity.HIGH,
            "Choose either VulkanMod OR Sodium"
        ));

        rules.add(new ConflictRule(
            "nvidium", "vulkanmod",
            "Both mods alter GPU rendering pipeline",
            ConflictSeverity.HIGH,
            "Keep VulkanMod on NVIDIA GPUs, otherwise remove Nvidium"
        ));
        
        // Medium severity
        rules.add(new ConflictRule(
            "sodium", "lambdynlights",
            "LambDynamicLights may have reduced performance with Sodium",
            ConflictSeverity.MEDIUM,
            "NOZH will coordinate settings between both mods"
        ));

        rules.add(new ConflictRule(
            "distant-horizons", "bobby",
            "Both mods extend view distance with different pipelines",
            ConflictSeverity.MEDIUM,
            "Keep Distant Horizons for far LODs, disable Bobby"
        ));

        rules.add(new ConflictRule(
            "dynamic_fps", "fpsreducer",
            "Both mods manage FPS throttling",
            ConflictSeverity.MEDIUM,
            "Keep only one FPS throttling mod"
        ));

        rules.add(new ConflictRule(
            "entityculling", "moreculling",
            "Both mods cull entities and block entities",
            ConflictSeverity.MEDIUM,
            "Keep the more feature-complete culling mod"
        ));
        
        return rules;
    }
    
    private Map<String, List<CapabilityId>> initializeCapabilityMap() {
        var map = new HashMap<String, List<CapabilityId>>();
        
        map.put("sodium", List.of(
            CapabilityId.RENDER_DISTANCE,
            CapabilityId.CHUNK_UPDATE_THREADS,
            CapabilityId.VSYNC,
            CapabilityId.CLOUDS,
            CapabilityId.FOG,
            CapabilityId.BIOME_BLEND,
            CapabilityId.SMOOTH_LIGHTING,
            CapabilityId.MIPMAP_LEVELS,
            CapabilityId.ANISOTROPIC_FILTERING,
            CapabilityId.PARTICLES
        ));

        map.put("sodium-extra", List.of(
            CapabilityId.PARTICLES,
            CapabilityId.CLOUDS,
            CapabilityId.FOG,
            CapabilityId.SMOOTH_LIGHTING,
            CapabilityId.MIPMAP_LEVELS
        ));

        map.put("reeses-sodium-options", List.of(
            CapabilityId.MIPMAP_LEVELS,
            CapabilityId.ANISOTROPIC_FILTERING,
            CapabilityId.SMOOTH_LIGHTING
        ));
        
        map.put("iris", List.of(
            CapabilityId.SHADER_QUALITY,
            CapabilityId.CLOUDS,
            CapabilityId.FOG
        ));

        map.put("optifabric", List.of(
            CapabilityId.SHADER_QUALITY,
            CapabilityId.CLOUDS,
            CapabilityId.FOG,
            CapabilityId.SMOOTH_LIGHTING
        ));

        map.put("canvas", List.of(
            CapabilityId.SHADER_QUALITY,
            CapabilityId.CLOUDS,
            CapabilityId.FOG
        ));

        map.put("vulkanmod", List.of(
            CapabilityId.RENDER_DISTANCE,
            CapabilityId.VSYNC,
            CapabilityId.CLOUDS
        ));

        map.put("nvidium", List.of(
            CapabilityId.RENDER_DISTANCE,
            CapabilityId.VSYNC
        ));

        map.put("bobby", List.of(
            CapabilityId.RENDER_DISTANCE
        ));

        map.put("distant-horizons", List.of(
            CapabilityId.RENDER_DISTANCE,
            CapabilityId.FOG
        ));
        
        map.put("entityculling", List.of(
            CapabilityId.ENTITY_CULLING
        ));

        map.put("moreculling", List.of(
            CapabilityId.ENTITY_CULLING,
            CapabilityId.ARMOR_STAND_CULLING,
            CapabilityId.ITEM_FRAME_CULLING,
            CapabilityId.BLOCK_ENTITY_CULLING
        ));

        map.put("enhancedblockentities", List.of(
            CapabilityId.BLOCK_ENTITY_CULLING
        ));

        map.put("lambdynlights", List.of(
            CapabilityId.DYNAMIC_LIGHTING
        ));

        map.put("dynamic_fps", List.of(
            CapabilityId.MAX_FPS
        ));

        map.put("fpsreducer", List.of(
            CapabilityId.MAX_FPS
        ));

        map.put("cullleaves", List.of(
            CapabilityId.LEAF_QUALITY
        ));

        map.put("continuity", List.of(
            CapabilityId.GRASS_DETAIL,
            CapabilityId.LEAF_QUALITY,
            CapabilityId.WATER_QUALITY,
            CapabilityId.MIPMAP_LEVELS
        ));

        map.put("lambdabettergrass", List.of(
            CapabilityId.GRASS_DETAIL
        ));

        map.put("betterclouds", List.of(
            CapabilityId.CLOUDS
        ));

        map.put("cullclouds", List.of(
            CapabilityId.CLOUDS
        ));

        map.put("fabricskyboxes", List.of(
            CapabilityId.CLOUDS
        ));

        map.put("visuality", List.of(
            CapabilityId.PARTICLES
        ));

        map.put("presencefootsteps", List.of(
            CapabilityId.PARTICLES
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
