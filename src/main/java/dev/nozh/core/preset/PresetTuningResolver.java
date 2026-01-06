package dev.nozh.core.preset;

import dev.nozh.core.matrix.ActionMatrixTuning;

public final class PresetTuningResolver {

    private PresetTuningResolver() {
    }

    public static ActionMatrixTuning resolve(HardwareProfile hardwareProfile, ModpackProfile modpackProfile) {
        ActionMatrixTuning tuning = ActionMatrixTuning.defaults();
        if (hardwareProfile != null) {
            tuning = tuning.merge(tuningForHardware(hardwareProfile));
        }
        if (modpackProfile != null) {
            tuning = tuning.merge(modpackProfile.tuning());
        }
        return tuning;
    }

    private static ActionMatrixTuning tuningForHardware(HardwareProfile profile) {
        HardwareTier tier = profile.overallTier();
        return switch (tier) {
            case CAFETERA -> new ActionMatrixTuning(0.08, 1.35, 1.15, 8, 5, 70, 60);
            case LOW -> new ActionMatrixTuning(0.06, 1.25, 1.1, 10, 6, 80, 75);
            case MEDIUM -> new ActionMatrixTuning(0.03, 1.1, 1.05, 12, 7, 90, 120);
            case HIGH -> new ActionMatrixTuning(0.0, 1.0, 1.0, 16, 10, 110, 144);
            case EXTREME -> new ActionMatrixTuning(-0.01, 0.95, 0.95, 24, 12, 130, 240);
            case NASA -> new ActionMatrixTuning(-0.02, 0.9, 0.9, Integer.MAX_VALUE,
                    Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        };
    }
}
