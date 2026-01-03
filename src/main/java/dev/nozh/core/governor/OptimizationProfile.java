package dev.nozh.core.governor;

/**
 * Optimization aggressiveness profile.
 */
public enum OptimizationProfile {
    BALANCED,
    AGGRESSIVE;

    public static OptimizationProfile fromConfig(String value) {
        if (value == null) {
            return BALANCED;
        }
        for (OptimizationProfile profile : values()) {
            if (profile.name().equalsIgnoreCase(value)) {
                return profile;
            }
        }
        return BALANCED;
    }

    public boolean isAggressive() {
        return this == AGGRESSIVE;
    }
}
