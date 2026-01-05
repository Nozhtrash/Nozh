package dev.nozh.core.capability;

/**
 * Provider coverage report (effective control coverage).
 */
public record ProviderCoverage(
        int totalCapabilities,
        int controlledCapabilities,
        double coveragePercent) {

    public static ProviderCoverage of(int totalCapabilities, int controlledCapabilities) {
        if (totalCapabilities <= 0) {
            return new ProviderCoverage(0, controlledCapabilities, 0.0);
        }
        double percent = (controlledCapabilities * 100.0) / totalCapabilities;
        return new ProviderCoverage(totalCapabilities, controlledCapabilities, percent);
    }
}
