package dev.nozh.core.benchmark;

import dev.nozh.core.telemetry.TelemetrySnapshot;

/**
 * Noise detector (Contract 8 - Phase 3).
 * 
 * Analyzes telemetry variance to classify benchmark validity.
 * Uses coefficient of variation (CV) as primary metric.
 */
public final class NoiseDetector {

    private static final double CV_THRESHOLD_VALID = 0.15; // <15% CV = VALID
    private static final double CV_THRESHOLD_NOISY = 0.30; // >30% CV = NOISY
    private static final int MIN_SAMPLES_REQUIRED = 30;

    /**
     * Classify benchmark validity based on telemetry variance.
     * 
     * @param snapshot Telemetry snapshot from benchmark run
     * @return Validity classification
     */
    public static BenchmarkValidity classify(TelemetrySnapshot snapshot) {
        if (!snapshot.sufficientData() || snapshot.sampleCount() < MIN_SAMPLES_REQUIRED) {
            return BenchmarkValidity.INCONCLUSIVE;
        }

        double cv = calculateCV(snapshot);

        if (cv < CV_THRESHOLD_VALID) {
            return BenchmarkValidity.VALID;
        } else if (cv > CV_THRESHOLD_NOISY) {
            return BenchmarkValidity.NOISY;
        } else {
            return BenchmarkValidity.INCONCLUSIVE;
        }
    }

    /**
     * Calculate coefficient of variation from telemetry.
     * 
     * CV = (std dev / mean) * 100
     * 
     * Approximation: Use P95-Avg spread as proxy for std dev.
     */
    public static double calculateCV(TelemetrySnapshot snapshot) {
        double avg = snapshot.avgFrametimeMs();
        double p95 = snapshot.p95FrametimeMs();

        if (avg <= 0) {
            return Double.MAX_VALUE; // Invalid
        }

        // Approximate CV using P95 spread
        double spread = p95 - avg;
        double cv = Math.abs(spread / avg);

        return cv;
    }

    private NoiseDetector() {
        // Static utility
    }
}
