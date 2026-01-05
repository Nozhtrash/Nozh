package dev.nozh.core.profiler;

import dev.nozh.api.PerfSnapshot;

/**
 * Simple spike predictor based on trend and saturation.
 */
public final class SpikeTrendPredictor {

    private static final double P95_RISE_THRESHOLD_MS = 2.0;
    private static final double SATURATION_P95_MS = 45.0;
    private static final double SATURATION_STDDEV_MS = 8.0;

    private double lastP95 = Double.NaN;
    private int lastSpikeCount = -1;
    private int p95RiseStreak = 0;
    private int spikeRiseStreak = 0;
    private SpikePrediction lastPrediction = new SpikePrediction(false, 0.0, "NO_DATA");

    public SpikePrediction update(PerfSnapshot snapshot) {
        if (snapshot == null || !snapshot.sufficientData()) {
            lastPrediction = new SpikePrediction(false, 0.0, "INSUFFICIENT_DATA");
            return lastPrediction;
        }

        double p95 = snapshot.p95FrametimeMs();
        int spikes = snapshot.spikeCount();
        double stddev = snapshot.frametimeStddevMs();

        boolean p95Rising = isValid(lastP95) && isValid(p95) && p95 > lastP95 + P95_RISE_THRESHOLD_MS;
        boolean spikesRising = lastSpikeCount >= 0 && spikes > lastSpikeCount;

        p95RiseStreak = p95Rising ? p95RiseStreak + 1 : 0;
        spikeRiseStreak = spikesRising ? spikeRiseStreak + 1 : 0;

        double confidence = 0.0;
        StringBuilder reason = new StringBuilder();
        if (p95RiseStreak >= 2) {
            confidence += 0.45;
            reason.append("p95_rising");
        }
        if (spikeRiseStreak >= 1) {
            confidence += 0.3;
            if (reason.length() > 0) {
                reason.append("+");
            }
            reason.append("spike_rising");
        }
        if (isValid(p95) && p95 >= SATURATION_P95_MS) {
            confidence += 0.2;
            if (reason.length() > 0) {
                reason.append("+");
            }
            reason.append("p95_saturation");
        }
        if (isValid(stddev) && stddev >= SATURATION_STDDEV_MS) {
            confidence += 0.1;
            if (reason.length() > 0) {
                reason.append("+");
            }
            reason.append("variance");
        }

        confidence = Math.max(0.0, Math.min(1.0, confidence));
        boolean spikeLikely = confidence >= 0.5;
        lastPrediction = new SpikePrediction(spikeLikely, confidence,
                reason.length() > 0 ? reason.toString() : "stable");

        lastP95 = p95;
        lastSpikeCount = spikes;

        return lastPrediction;
    }

    public SpikePrediction getLastPrediction() {
        return lastPrediction;
    }

    public void reset() {
        lastP95 = Double.NaN;
        lastSpikeCount = -1;
        p95RiseStreak = 0;
        spikeRiseStreak = 0;
        lastPrediction = new SpikePrediction(false, 0.0, "RESET");
    }

    private boolean isValid(double value) {
        return Double.isFinite(value) && value > 0;
    }
}
