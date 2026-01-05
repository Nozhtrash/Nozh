package dev.nozh.core.context;

/**
 * Scenario confidence signal for governor modulation.
 */
public record ScenarioConfidence(
        double value,
        Band band,
        double stability) {

    public enum Band {
        LOW,
        MEDIUM,
        HIGH
    }

    public ScenarioConfidence {
        if (band == null) {
            throw new IllegalArgumentException("ScenarioConfidence band cannot be null");
        }
    }

    public static ScenarioConfidence from(double value, double stability) {
        double clampedValue = clamp(value);
        double clampedStability = clamp(stability);
        Band band;
        if (clampedValue >= 0.75 && clampedStability >= 0.65) {
            band = Band.HIGH;
        } else if (clampedValue >= 0.5 && clampedStability >= 0.45) {
            band = Band.MEDIUM;
        } else {
            band = Band.LOW;
        }
        return new ScenarioConfidence(clampedValue, band, clampedStability);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
