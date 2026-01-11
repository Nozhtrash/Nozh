package dev.nozh.core.preset;

import dev.nozh.core.config.OptimizationProfile;
import dev.nozh.core.matrix.ActionMatrixTuning;

import java.util.List;

public record ModpackProfile(
        String id,
        String name,
        ModpackType type,
        List<String> signatureMods,
        List<String> requiredMods,
        List<String> recommendedMods,
        List<String> incompatibleMods,
        OptimizationProfile actionProfile,
        dev.nozh.core.config.OptimizationProfile configProfile,
        int targetFps,
        ActionMatrixTuning tuning) {
}
