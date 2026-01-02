package dev.nozh.core.hardening;

import dev.nozh.core.issues.Issue;
import dev.nozh.core.issues.IssueType;
import dev.nozh.core.issues.IssueSeverity;
import dev.nozh.core.preset.HardwareTier;

import java.util.ArrayList;
import java.util.List;

/**
 * Compat matrix detector (Release Hardening).
 * 
 * Detects common performance mods and suggests appropriate presets.
 * 
 * Note: In real implementation, this would check loaded mods.
 * For PROMPT 3, providing structure with stubs.
 */
public final class CompatDetector {

    /**
     * Detect compatibility issues and suggest presets.
     * 
     * @param loadedMods List of loaded mod IDs (from FabricLoader)
     * @return Issues + suggestions
     */
    public static List<Issue> detect(List<String> loadedMods) {
        List<Issue> issues = new ArrayList<>();
        long now = System.currentTimeMillis();

        boolean hasSodium = loadedMods.contains("sodium");
        boolean hasIris = loadedMods.contains("iris");
        boolean hasDH = loadedMods.contains("distanthorizons");

        // INFO: Performance mod detected
        if (hasSodium) {
            issues.add(Issue.create(
                    IssueType.UNKNOWN,
                    IssueSeverity.INFO,
                    "nozh.compat.sodium.detected",
                    now));
        }

        // INFO: Shader mod detected
        if (hasIris) {
            issues.add(Issue.create(
                    IssueType.UNSUPPORTED_SHADER,
                    IssueSeverity.INFO,
                    "nozh.compat.iris.detected",
                    now));
        }

        // WARNING: DH is heavy
        if (hasDH) {
            issues.add(Issue.create(
                    IssueType.UNKNOWN,
                    IssueSeverity.WARNING,
                    "nozh.compat.dh.heavy",
                    now));
        }

        return issues;
    }

    /**
     * Suggest preset based on detected mods.
     * 
     * @param loadedMods Loaded mod IDs
     * @return Suggested preset tier
     */
    public static HardwareTier suggestPreset(List<String> loadedMods) {
        boolean hasDH = loadedMods.contains("distanthorizons");
        boolean hasShaders = loadedMods.contains("iris") || loadedMods.contains("oculus");

        if (hasDH && hasShaders) {
            return HardwareTier.EXTREME; // Very demanding
        } else if (hasDH || hasShaders) {
            return HardwareTier.HIGH;
        } else {
            return HardwareTier.MEDIUM; // Default
        }
    }

    private CompatDetector() {
        // Static utility
    }
}
