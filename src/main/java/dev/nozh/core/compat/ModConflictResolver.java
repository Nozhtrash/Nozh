package dev.nozh.core.compat;

import dev.nozh.NozhConstants;

import java.util.*;

/**
 * Automatically resolves conflicts between mods.
 * Adjusts NOZH behavior to avoid stepping on other mods.
 *
 * @since 0.3.0
 * @author NOZH Team
 */
public final class ModConflictResolver {

    /**
     * Conflict resolution result.
     */
    public record ConflictResolution(
            String modA,
            String modB,
            String conflictedCapability,
            String winner,
            String resolution,
            boolean userCanOverride) {
        public String describe() {
            return String.format("%s vs %s: %s wins (%s)", modA, modB, winner, resolution);
        }
    }

    /**
     * PRIORITY 2: Director Mode bias profile.
     *
     * <p>
     * This is intentionally generic (CPU/GPU focus) so it can be applied by the Governor
     * without hard dependencies on specific mod APIs.
     */
    public record DirectorBiasProfile(
            double cpuBias,
            double gpuBias,
            Set<String> detectedSynergies,
            String summary) {
    }

    private final Set<String> loadedMods;
    private final List<ConflictResolution> resolvedConflicts;
    private final Map<String, String> userOverrides; // capability -> winner

    /**
     * Constructs a new ModConflictResolver.
     */
    public ModConflictResolver() {
        this.loadedMods = new HashSet<>();
        this.resolvedConflicts = new ArrayList<>();
        this.userOverrides = new HashMap<>();
    }

    /**
     * Registers a loaded mod for conflict checking.
     *
     * @param modId mod identifier
     */
    public void registerMod(String modId) {
        loadedMods.add(modId.toLowerCase());
    }

    /**
     * Analyzes all potential conflicts.
     *
     * @return list of detected conflicts and their resolutions
     */
    public List<ConflictResolution> analyzeConflicts() {
        resolvedConflicts.clear();

        for (String modId : loadedMods) {
            Optional<ModKnowledgeBase.ModInfo> infoOpt = ModKnowledgeBase.getModInfo(modId);

            if (infoOpt.isEmpty())
                continue;

            ModKnowledgeBase.ModInfo info = infoOpt.get();

            // Check for conflicts with other loaded mods
            for (String conflictModId : info.conflictsWith()) {
                if (loadedMods.contains(conflictModId)) {
                    ConflictResolution resolution = resolveConflict(modId, conflictModId, info);
                    resolvedConflicts.add(resolution);
                }
            }
        }

        return List.copyOf(resolvedConflicts);
    }

    /**
     * Resolves a conflict between two mods.
     */
    private ConflictResolution resolveConflict(String modA, String modB, ModKnowledgeBase.ModInfo infoA) {
        Optional<ModKnowledgeBase.ModInfo> infoBOpt = ModKnowledgeBase.getModInfo(modB);

        // Determine winner based on mod category and impact
        String winner;
        String resolution;

        if (infoA.nozhShouldAvoid()) {
            winner = modA;
            resolution = "NOZH yields to " + modA + " (configured to avoid)";
        } else if (infoBOpt.isPresent() && infoBOpt.get().nozhShouldAvoid()) {
            winner = modB;
            resolution = "NOZH yields to " + modB + " (configured to avoid)";
        } else if (infoA.category() == ModKnowledgeBase.ModCategory.OPTIMIZATION) {
            winner = modA;
            resolution = "Optimization mod takes precedence";
        } else if (infoBOpt.isPresent() &&
                infoBOpt.get().category() == ModKnowledgeBase.ModCategory.OPTIMIZATION) {
            winner = modB;
            resolution = "Optimization mod takes precedence";
        } else {
            // Default: first detected wins
            winner = modA;
            resolution = "First-detected priority";
        }

        return new ConflictResolution(
                modA, modB,
                "settings_control",
                winner,
                resolution,
                true);
    }

    /**
     * PRIORITY 2: Director Mode V2 - compute bias based on detected mod synergies.
     *
     * <p>
     * Heuristics:
     * - Sodium + Iris tends to move bottlenecks toward GPU; bias GPU-friendly actions.
     * - Lithium + C2ME tends to reduce CPU world-gen/tick pressure; bias toward GPU/visual.
     * - Distant Horizons can become GPU/memory heavy; advise conservative GPU actions.
     */
    public DirectorBiasProfile computeDirectorBiasProfile() {
        boolean sodium = loadedMods.contains("sodium");
        boolean iris = loadedMods.contains("iris");
        boolean lithium = loadedMods.contains("lithium");
        boolean c2me = loadedMods.contains("c2me") || loadedMods.contains("c2me-fabric");
        boolean distantHorizons = loadedMods.contains("distanthorizons");

        double cpuBias = 1.0;
        double gpuBias = 1.0;
        Set<String> synergies = new HashSet<>();

        if (sodium && iris) {
            synergies.add("sodium+iris");
            // Shaders pipeline can become GPU-heavy; reduce aggressiveness on GPU-heavy changes.
            gpuBias *= 1.15;
        }

        if (lithium && c2me) {
            synergies.add("lithium+c2me");
            // CPU side is already optimized; slightly bias toward visual quality/less CPU cuts.
            cpuBias *= 0.90;
        }

        if (distantHorizons) {
            // Can increase GPU + VRAM pressure; be conservative.
            gpuBias *= 1.25;
        }

        String summary = "DirectorBias cpu=" + String.format(Locale.ROOT, "%.2f", cpuBias)
                + " gpu=" + String.format(Locale.ROOT, "%.2f", gpuBias)
                + " synergies=" + synergies;

        return new DirectorBiasProfile(cpuBias, gpuBias, Collections.unmodifiableSet(synergies), summary);
    }

    /**
     * Applies all conflict resolutions.
     */
    public void resolveAll() {
        if (resolvedConflicts.isEmpty()) {
            analyzeConflicts();
        }

        for (ConflictResolution resolution : resolvedConflicts) {
            NozhConstants.LOGGER.info("Conflict resolved: {}", resolution.describe());
        }

        NozhConstants.LOGGER.info("Resolved {} conflicts", resolvedConflicts.size());
    }

    /**
     * Gets a human-readable conflict report.
     *
     * @return conflict report
     */
    public String getConflictReport() {
        if (resolvedConflicts.isEmpty()) {
            return "No conflicts detected";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Mod Conflict Report ===\n");
        report.append(String.format("Total conflicts: %d\n\n", resolvedConflicts.size()));

        for (ConflictResolution res : resolvedConflicts) {
            report.append(String.format("• %s\n", res.describe()));
            if (res.userCanOverride()) {
                report.append("  (User can override this resolution)\n");
            }
        }

        return report.toString();
    }

    /**
     * Sets a user override for a conflict resolution.
     *
     * @param capability   conflicted capability
     * @param preferredMod preferred mod to control this capability
     */
    public void setUserOverride(String capability, String preferredMod) {
        userOverrides.put(capability, preferredMod);
        NozhConstants.LOGGER.info("User override set: {} -> {}", capability, preferredMod);
    }

    /**
     * Checks if NOZH should control a capability.
     *
     * @param capability capability to check
     * @return true if NOZH should handle it
     */
    public boolean shouldNozhControl(String capability) {
        // Check user overrides first
        if (userOverrides.containsKey(capability)) {
            return userOverrides.get(capability).equalsIgnoreCase("nozh");
        }

        // Check resolved conflicts
        for (ConflictResolution res : resolvedConflicts) {
            if (res.conflictedCapability().equals(capability)) {
                return res.winner().equalsIgnoreCase("nozh");
            }
        }

        return true; // Default: NOZH controls
    }

    /**
     * Gets resolved conflict count.
     *
     * @return number of conflicts
     */
    public int getConflictCount() {
        return resolvedConflicts.size();
    }
}
