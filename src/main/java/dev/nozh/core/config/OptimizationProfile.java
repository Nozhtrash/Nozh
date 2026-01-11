package dev.nozh.core.config;

public enum OptimizationProfile {
    AGGRESSIVE(0.9, 30),
    CONSERVATIVE(0.3, 120),
    BALANCED(0.6, 60),
    CUSTOM(0.5, 60);
    
    private final double aggressiveness;
    private final int targetFps;
    
    OptimizationProfile(double aggressiveness, int targetFps) {
        this.aggressiveness = aggressiveness;
        this.targetFps = targetFps;
    }
    
    public String getDisplayName() {
        return switch (this) {
            case AGGRESSIVE -> "Potato PC (Aggressive)";
            case CONSERVATIVE -> "High-End PC (Conservative)";
            case BALANCED -> "Mid-Range PC (Balanced)";
            case CUSTOM -> "Custom";
        };
    }
    
    public int getTargetFps() {
        return this.targetFps;
    }
    
    public double aggressiveness() {
        return this.aggressiveness;
    }
    
    /**
     * Convert from config value (either OptimizationProfile or String) to OptimizationProfile enum.
     */
    public static OptimizationProfile fromConfig(Object config) {
        if (config instanceof OptimizationProfile) {
            return (OptimizationProfile) config;
        }
        if (config instanceof String) {
            try {
                return OptimizationProfile.valueOf(((String) config).toUpperCase());
            } catch (IllegalArgumentException e) {
                return BALANCED; // Default fallback
            }
        }
        return BALANCED; // Default fallback
    }
}