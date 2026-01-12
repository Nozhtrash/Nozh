package dev.nozh.core.priority3;

/**
 * v0.3: Spike prediction result.
 */
public final class SpikePrediction {

    public final boolean likely;
    public final double probability01;
    public final double predictedNextMs;

    public SpikePrediction(boolean likely, double probability01, double predictedNextMs) {
        this.likely = likely;
        this.probability01 = clamp01(probability01);
        this.predictedNextMs = sanitizeMs(predictedNextMs);
    }

    private static double clamp01(double v) {
        if (!Double.isFinite(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static double sanitizeMs(double ms) {
        if (!Double.isFinite(ms) || ms < 0.0) return -1.0;
        if (ms > 60_000.0) return 60_000.0;
        return ms;
    }
}
