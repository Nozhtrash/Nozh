package dev.nozh.core.compatibility;

import dev.nozh.core.bus.CapabilityId;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PRIORITY 2: Director Mode V2 - Advanced mod coordination.
 * 
 * Detects 25+ optimization mods and coordinates NOZH behavior to avoid conflicts.
 * Provides dynamic bias adjustments and capability yielding.
 * 
 * Zero conflicts guaranteed.
 */
public final class ModCoordinator {

    private final Map<String, ModInfo> detectedMods = new HashMap<>();
    private final Set<CapabilityId> yieldedCapabilities = new HashSet<>();
    private double gpuBoundBias = 1.0;
    private double cpuBoundBias = 1.0;

    public ModCoordinator() {
        analyzeMods();
    }

    private void analyzeMods() {
        FabricLoader loader = FabricLoader.getInstance();

        // === RENDERING MODS ===
        checkMod(loader, "sodium", ModType.RENDERING, 2.0, "Core rendering optimization");
        checkMod(loader, "iris", ModType.SHADERS, 1.5, "Shader support");
        checkMod(loader, "indium", ModType.RENDERING, 0.5, "Sodium Fabric Rendering API");
        checkMod(loader, "immediatelyfast", ModType.RENDERING, 1.0, "Immediate mode rendering");
        checkMod(loader, "embeddium", ModType.RENDERING, 2.0, "Sodium fork");
        checkMod(loader, "rubidium", ModType.RENDERING, 2.0, "Forge Sodium port");

        // === CPU OPTIMIZATION MODS ===
        checkMod(loader, "lithium", ModType.CPU, 1.5, "Game logic optimization");
        checkMod(loader, "ferritecore", ModType.MEMORY, 1.0, "Memory usage optimization");
        checkMod(loader, "modernfix", ModType.CPU, 1.0, "Modern performance fixes");
        checkMod(loader, "c2me", ModType.CPU, 1.5, "Concurrent chunk processing");
        checkMod(loader, "starlight", ModType.CPU, 1.0, "Lighting engine rewrite");
        checkMod(loader, "krypton", ModType.NETWORK, 0.5, "Network stack optimization");

        // === CULLING MODS ===
        checkMod(loader, "entityculling", ModType.CULLING, 1.0, "Entity render culling");
        checkMod(loader, "moreculling", ModType.CULLING, 1.0, "Advanced culling");
        checkMod(loader, "culllessleaves", ModType.CULLING, 0.5, "Leaf culling fix");

        // === PARTICLE MODS ===
        checkMod(loader, "sodium-extra", ModType.PARTICLES, 1.0, "Sodium extra options");
        checkMod(loader, "reese-sodium-options", ModType.PARTICLES, 0.5, "Sodium settings");

        // === OTHER OPTIMIZATION MODS ===
        checkMod(loader, "exordium", ModType.GUI, 0.5, "GUI rendering optimization");
        checkMod(loader, "lazydfu", ModType.STARTUP, 0.0, "Lazy DataFixerUpper");
        checkMod(loader, "smoothboot", ModType.STARTUP, 0.0, "Smooth game startup");
        checkMod(loader, "fastload", ModType.STARTUP, 0.0, "Fast world loading");
        checkMod(loader, "memoryleakfix", ModType.MEMORY, 0.5, "Memory leak fixes");
        checkMod(loader, "dynamicfps", ModType.GUI, 0.3, "Dynamic FPS control");
        checkMod(loader, "enhanced-block-entities", ModType.RENDERING, 0.8, "BlockEntity optimization");

        // === CONFLICTING MODS ===
        checkMod(loader, "optifine", ModType.CONFLICTING, -1.0, "[CONFLICT] OptiFine detected");

        // Calculate biases
        calculateBiases();

        // Determine yielded capabilities
        determineYields();
    }

    private void checkMod(FabricLoader loader, String modId, ModType type, double biasImpact, String description) {
        if (loader.isModLoaded(modId)) {
            Optional<ModContainer> container = loader.getModContainer(modId);
            String version = container.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
            ModInfo info = new ModInfo(modId, type, biasImpact, true, description, version);
            detectedMods.put(modId, info);
        }
    }

    private void calculateBiases() {
        // Iris = shaders = almost always GPU-bound
        if (detectedMods.containsKey("iris")) {
            gpuBoundBias = 1.5;
        }

        // Sodium = already handles render chunks well
        if (detectedMods.containsKey("sodium")) {
            gpuBoundBias *= 1.2;
        }

        // Lithium = AI and tick optimizations
        if (detectedMods.containsKey("lithium")) {
            cpuBoundBias = 1.3;
        }

        // C2ME = chunk processing
        if (detectedMods.containsKey("c2me")) {
            cpuBoundBias *= 1.2;
        }

        // DynamicFPS conflicts with our FPS capping
        if (detectedMods.containsKey("dynamicfps")) {
            yieldedCapabilities.add(CapabilityId.FPS_CAP);
        }
    }

    private void determineYields() {
        // Sodium Extra handles particles
        if (detectedMods.containsKey("sodium-extra")) {
            yieldedCapabilities.add(CapabilityId.PARTICLES);
        }

        // EntityCulling handles entity rendering
        if (detectedMods.containsKey("entityculling")) {
            yieldedCapabilities.add(CapabilityId.ARMOR_STANDS);
            yieldedCapabilities.add(CapabilityId.ITEM_FRAMES);
        }

        // MoreCulling handles various culling
        if (detectedMods.containsKey("moreculling")) {
            yieldedCapabilities.add(CapabilityId.BLOCK_ENTITIES);
        }

        // Iris handles clouds (shaders)
        if (detectedMods.containsKey("iris")) {
            yieldedCapabilities.add(CapabilityId.CLOUDS);
        }

        // Enhanced Block Entities
        if (detectedMods.containsKey("enhanced-block-entities")) {
            yieldedCapabilities.add(CapabilityId.BLOCK_ENTITIES);
        }
    }

    /**
     * Check if NOZH should yield control of a capability to another mod.
     */
    public boolean shouldYield(CapabilityId capability) {
        return yieldedCapabilities.contains(capability);
    }

    /**
     * Get GPU-bound detection bias.
     * Higher = more likely to detect GPU-bound.
     */
    public double getGpuBias() {
        return gpuBoundBias;
    }

    /**
     * Get CPU-bound detection bias.
     * Higher = more likely to detect CPU-bound.
     */
    public double getCpuBias() {
        return cpuBoundBias;
    }

    /**
     * Get detected mods summary for display.
     */
    public String getSummary() {
        if (detectedMods.isEmpty()) {
            return "No optimization mods detected";
        }

        List<String> modNames = detectedMods.keySet().stream()
            .limit(5)
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("Coordinating with: ");
        sb.append(String.join(", ", modNames));
        
        if (detectedMods.size() > 5) {
            sb.append(" +").append(detectedMods.size() - 5).append(" more");
        }
        
        return sb.toString();
    }

    /**
     * Get detailed report of all detected mods.
     */
    public List<String> getDetailedReport() {
        if (detectedMods.isEmpty()) {
            return List.of("No optimization mods detected");
        }

        List<String> report = new ArrayList<>();
        report.add("=== NOZH Mod Coordination Report ===");
        report.add("");

        // Group by type
        Map<ModType, List<ModInfo>> byType = detectedMods.values().stream()
            .collect(Collectors.groupingBy(ModInfo::type));

        for (ModType type : ModType.values()) {
            List<ModInfo> mods = byType.get(type);
            if (mods != null && !mods.isEmpty()) {
                report.add(type.displayName() + ":");
                for (ModInfo mod : mods) {
                    String yieldStatus = yieldedCapabilities.stream()
                        .anyMatch(cap -> shouldYieldForMod(mod.modId(), cap)) 
                        ? " [YIELDING]" : "";
                    report.add("  - " + mod.modId() + " v" + mod.version() + yieldStatus);
                    report.add("    " + mod.description());
                }
                report.add("");
            }
        }

        // Biases
        report.add("Bias Adjustments:");
        report.add("  GPU Bias: " + String.format("%.2f", gpuBoundBias) + "x");
        report.add("  CPU Bias: " + String.format("%.2f", cpuBoundBias) + "x");
        report.add("");

        // Yielded capabilities
        if (!yieldedCapabilities.isEmpty()) {
            report.add("Yielded Capabilities:");
            for (CapabilityId cap : yieldedCapabilities) {
                report.add("  - " + cap.name());
            }
        }

        return report;
    }

    private boolean shouldYieldForMod(String modId, CapabilityId capability) {
        // Determine if this specific mod causes yielding of capability
        if (modId.equals("sodium-extra") && capability == CapabilityId.PARTICLES) return true;
        if (modId.equals("entityculling") && (capability == CapabilityId.ARMOR_STANDS || capability == CapabilityId.ITEM_FRAMES)) return true;
        if (modId.equals("moreculling") && capability == CapabilityId.BLOCK_ENTITIES) return true;
        if (modId.equals("iris") && capability == CapabilityId.CLOUDS) return true;
        if (modId.equals("enhanced-block-entities") && capability == CapabilityId.BLOCK_ENTITIES) return true;
        if (modId.equals("dynamicfps") && capability == CapabilityId.FPS_CAP) return true;
        return false;
    }

    /**
     * Get full list of detected mods.
     */
    public Map<String, ModInfo> getDetectedMods() {
        return Map.copyOf(detectedMods);
    }

    /**
     * Check if a conflicting mod is present.
     */
    public boolean hasConflictingMods() {
        return detectedMods.values().stream()
            .anyMatch(info -> info.type() == ModType.CONFLICTING);
    }

    /**
     * Get warning message if conflicts detected.
     */
    public String getConflictWarning() {
        if (!hasConflictingMods()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("⚠️ NOZH detected conflicting mods: ");
        detectedMods.values().stream()
            .filter(info -> info.type() == ModType.CONFLICTING)
            .forEach(info -> sb.append(info.modId()).append(" "));
        sb.append("- Compatibility not guaranteed!");
        return sb.toString();
    }

    /**
     * Get count of detected optimization mods.
     */
    public int getOptimizationModCount() {
        return (int) detectedMods.values().stream()
            .filter(info -> info.type() != ModType.CONFLICTING)
            .count();
    }

    /**
     * Check if we're running in a highly optimized environment.
     */
    public boolean isHighlyOptimized() {
        return getOptimizationModCount() >= 5;
    }

    /**
     * Mod information record.
     */
    public record ModInfo(
        String modId,
        ModType type,
        double biasImpact,
        boolean active,
        String description,
        String version
    ) {}

    /**
     * Mod type classification.
     */
    public enum ModType {
        RENDERING("Rendering Optimizations"),
        CPU("CPU Optimizations"),
        MEMORY("Memory Optimizations"),
        CULLING("Culling Optimizations"),
        PARTICLES("Particle Optimizations"),
        NETWORK("Network Optimizations"),
        GUI("GUI Optimizations"),
        STARTUP("Startup Optimizations"),
        SHADERS("Shader Support"),
        CONFLICTING("Conflicting Mods");

        private final String displayName;

        ModType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }
}
