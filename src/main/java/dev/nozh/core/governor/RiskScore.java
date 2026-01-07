package dev.nozh.core.governor;

/**
 * Risk score output with component contributions.
 */
public record RiskScore(
        double total,
        double tickComponent,
        double renderComponent,
        double gcComponent,
        double entityComponent,
        double chunkComponent) {

    public boolean isHigh(double threshold) {
        return total >= threshold;
    }
}
