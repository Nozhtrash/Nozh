package dev.nozh.core.matrix;

/**
 * ActionMatrix tuning profile derived from hardware + modpack presets.
 * 
 * Used to bias candidate scoring and apply caps to target values.
 */
public record ActionMatrixTuning(
        double confidenceBonus,
        double pressureMultiplier,
        double scenarioWeightMultiplier,
        int maxRenderDistance,
        int maxSimulationDistance,
        int maxEntityDistance,
        int maxFpsCap) {

    public static ActionMatrixTuning defaults() {
        return new ActionMatrixTuning(
                0.0,
                1.0,
                1.0,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE);
    }

    public ActionMatrixTuning merge(ActionMatrixTuning other) {
        if (other == null) {
            return this;
        }
        return new ActionMatrixTuning(
                confidenceBonus + other.confidenceBonus,
                pressureMultiplier * other.pressureMultiplier,
                scenarioWeightMultiplier * other.scenarioWeightMultiplier,
                Math.min(maxRenderDistance, other.maxRenderDistance),
                Math.min(maxSimulationDistance, other.maxSimulationDistance),
                Math.min(maxEntityDistance, other.maxEntityDistance),
                Math.min(maxFpsCap, other.maxFpsCap));
    }
}
