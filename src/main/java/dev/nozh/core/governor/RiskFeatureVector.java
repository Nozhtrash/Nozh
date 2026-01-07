package dev.nozh.core.governor;

/**
 * Feature vector for multi-variable risk prediction.
 */
public record RiskFeatureVector(
        double tickMs,
        double renderMs,
        double gcMs,
        int entityCount,
        int chunkLoadRate) {

    public boolean hasSignal() {
        return tickMs > 0
                || renderMs > 0
                || gcMs > 0
                || entityCount > 0
                || chunkLoadRate > 0;
    }
}
