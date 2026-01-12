package dev.nozh.core.preset;

import dev.nozh.core.governor.OptimizationProfile;
import dev.nozh.core.matrix.ActionMatrixTuning;
import java.util.List;

/**
 * Modpack Profile - Applies tweaks based on detected environment.
 * Reduced to a Record to match ModpackRegistry usage.
 */
public record ModpackProfile(
    String id,
    String name,
    ModpackType type,
    List<String> signatureMods,
    List<String> requiredMods,
    List<String> recommendedMods,
    List<String> incompatibleMods,
    OptimizationProfile governorProfile,
    dev.nozh.core.config.OptimizationProfile configProfile,
    int targetFps,
    ActionMatrixTuning tuning
) {
    // Static utility for applying logic if needed, but the record holds the data.
}
