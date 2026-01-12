package dev.nozh.core.priority3;

import java.util.Arrays;

/**
 * v0.3: Predictive analyzer (simple linear regression) for frametime.
 *
 * <p>Designed to be tiny, deterministic, allocation-free on hot path, and safe.
 */
public final class PredictiveAnalyzer {

    private final double[] ring;
    private int size;
    private int head;

    private double last;

    public PredictiveAnalyzer(int window) {
        int w = Math.max(8, Math.min(120, window));
        this.ring = new double[w];
        Arrays.fill(this.ring, -1.0);
    }

    public void addSampleMs(double ms) {
        double v = sanitizeMs(ms);
        last = v;
        ring[head] = v;
        head = (head + 1) % ring.length;
        if (size < ring.length) size++;
    }

    public double lastSampleMs() {
        return last;
    }

    /**
     * Predict next frametime using linear regression on the last N samples.
     */
    public double predictNextFrametimeMs() {
        if (size < 8) {
            return last;
        }

        // x: 0..(n-1), y: samples
        int n = size;
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXX = 0.0;
        double sumXY = 0.0;

        for (int i = 0; i < n; i++) {
            double y = sampleFromTail(i);
            if (y < 0.0) {
                // Unknown sample; shrink window.
                n = i;
                break;
            }
            double x = i;
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
        }

        if (n < 8) {
            return last;
        }

        double denom = (n * sumXX - sumX * sumX);
        if (Math.abs(denom) < 1e-9) {
            return last;
        }

        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        double predicted = slope * (n) + intercept;
        return sanitizeMs(predicted);
    }

    /**
     * If prediction suggests improvement without intervention, prefer waiting.
     */
    public boolean shouldWaitForRecovery() {
        double predicted = predictNextFrametimeMs();
        if (predicted < 0.0 || last < 0.0) return false;

        // If we are trending down by at least 12% and already not terrible, wait.
        if (predicted <= last * 0.88 && last <= 28.0) {
            return true;
        }

        // If we are near-stable under ~18ms, avoid actions.
        if (last <= 18.0 && predicted <= 18.0) {
            return true;
        }

        return false;
    }

    /**
     * Predict spikes by comparing predicted next vs last.
     */
    public SpikePrediction predictSpike() {
        double predicted = predictNextFrametimeMs();
        if (predicted < 0.0 || last < 0.0) {
            return new SpikePrediction(false, 0.0, predicted);
        }

        double ratio = (last <= 0.001) ? 1.0 : (predicted / last);
        // If predicted is 30% higher and last is already above a threshold, mark likely.
        if (ratio >= 1.30 && predicted >= 28.0) {
            double p = Math.min(1.0, 0.55 + (ratio - 1.30) * 0.9);
            return new SpikePrediction(true, p, predicted);
        }

        return new SpikePrediction(false, 0.15, predicted);
    }

    private double sampleFromTail(int tailIndex) {
        int idx = head - 1 - tailIndex;
        while (idx < 0) idx += ring.length;
        return ring[idx];
    }

    private static double sanitizeMs(double ms) {
        if (!Double.isFinite(ms) || ms < 0.0) return -1.0;
        if (ms > 60_000.0) return 60_000.0;
        return ms;
    }
}
