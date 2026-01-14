package dev.nozh.core.config;

public enum OptimizationProfile {
    AGGRESSIVE,
    EXTREME,
    CONSERVATIVE,
    BALANCED,
    CUSTOM;

    public String getDisplayName() {
        return switch (this) {
            case AGGRESSIVE -> "Potato PC (Aggressive)";
            case EXTREME -> "Extreme Potato (Survival Mode)";
            case CONSERVATIVE -> "High-End PC (Conservative)";
            case BALANCED -> "Mid-Range PC (Balanced)";
            case CUSTOM -> "Custom";
        };
    }

    public int getTargetFps() {
        return switch (this) {
            case AGGRESSIVE -> 30;
            case EXTREME -> 20; // Just playable
            case CONSERVATIVE -> 120;
            case BALANCED -> 60;
            case CUSTOM -> 60;
        };
    }
}
