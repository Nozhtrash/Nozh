package dev.nozh.core.state;

/**
 * Stability snapshot derived from telemetry and scenario stability.
 */
public record StabilityStats(
        double score,
        double variance,
        int flapCount,
        long lastFlapTimestamp,
        long lastUpdateTimestamp) {

    private static final double DEFAULT_SCORE = 0.5;
    private static final double FLAP_THRESHOLD = 0.35;
    private static final long FLAP_WINDOW_MS = 30_000L;
    private static final double VARIANCE_NORMALIZER = 100.0;
    private static final double STDDEV_NORMALIZER = 15.0;
    private static final double SPIKE_NORMALIZER = 8.0;

    public static StabilityStats defaults() {
        return new StabilityStats(DEFAULT_SCORE, 0.0, 0, 0L, 0L);
    }

    public StabilityStats update(double avgMs, double p95Ms, double stddevMs, int spikes,
            double scenarioStability, long nowMillis) {
        if (avgMs <= 0 || p95Ms <= 0) {
            return new StabilityStats(score, variance, flapCount, lastFlapTimestamp, lastUpdateTimestamp);
        }

        double spread = p95Ms - avgMs;
        double computedVariance = spread * spread;
        double varianceScore = 1.0 - clamp(computedVariance / VARIANCE_NORMALIZER);
        double stddevScore = stddevMs > 0 ? 1.0 - clamp(stddevMs / STDDEV_NORMALIZER) : varianceScore;
        double spikeScore = 1.0 - clamp(spikes / SPIKE_NORMALIZER);
        double scenarioScore = clamp(scenarioStability);

        double computedScore = clamp(0.4 * varianceScore + 0.3 * stddevScore + 0.2 * spikeScore + 0.1 * scenarioScore);
        boolean flapping = computedScore < FLAP_THRESHOLD;
        int nextFlapCount = flapCount;
        long nextFlapTimestamp = lastFlapTimestamp;

        if (flapping) {
            if (lastFlapTimestamp > 0 && nowMillis - lastFlapTimestamp <= FLAP_WINDOW_MS) {
                nextFlapCount = flapCount + 1;
            } else {
                nextFlapCount = 1;
            }
            nextFlapTimestamp = nowMillis;
        } else if (nextFlapCount > 0 && lastFlapTimestamp > 0 && nowMillis - lastFlapTimestamp > FLAP_WINDOW_MS) {
            nextFlapCount = Math.max(0, flapCount - 1);
        }

        return new StabilityStats(computedScore, computedVariance, nextFlapCount, nextFlapTimestamp, nowMillis);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
