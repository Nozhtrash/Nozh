package dev.nozh.core.benchmark;

import dev.nozh.core.telemetry.TelemetrySnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NoiseDetector (Phase 3).
 */
class NoiseDetectorTest {

    @Test
    void lowVarianceClassifiesAsValid() {
        // Low CV scenario: avg=16ms, p95=17ms → CV ~6%
        TelemetrySnapshot snapshot = TelemetrySnapshot.of(16.0, 17.0, 2, 50, 0);

        BenchmarkValidity validity = NoiseDetector.classify(snapshot);

        assertEquals(BenchmarkValidity.VALID, validity, "Low variance should be VALID");
    }

    @Test
    void highVarianceClassifiesAsNoisy() {
        // High CV scenario: avg=16ms, p95=25ms → CV ~56%
        TelemetrySnapshot snapshot = TelemetrySnapshot.of(16.0, 25.0, 15, 50, 0);

        BenchmarkValidity validity = NoiseDetector.classify(snapshot);

        assertEquals(BenchmarkValidity.NOISY, validity, "High variance should be NOISY");
    }

    @Test
    void insufficientDataClassifiesAsInconclusive() {
        TelemetrySnapshot snapshot = TelemetrySnapshot.of(16.0, 18.0, 0, 10, 0);

        BenchmarkValidity validity = NoiseDetector.classify(snapshot);

        assertEquals(BenchmarkValidity.INCONCLUSIVE, validity, "Insufficient samples should be INCONCLUSIVE");
    }

    @Test
    void cvCalculationIsCorrect() {
        TelemetrySnapshot snapshot = TelemetrySnapshot.of(20.0, 25.0, 0, 50, 0);

        double cv = NoiseDetector.calculateCV(snapshot);

        // CV = (25-20)/20 = 0.25 (25%)
        assertEquals(0.25, cv, 0.01, "CV calculation should be accurate");
    }
}
