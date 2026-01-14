package dev.nozh.core.telemetry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates telemetry buffer overhead stays within budget.
 */
class TelemetryOverheadTest {

    @Test
    void addOverheadWithinBudget() {
        RingTelemetryBuffer buffer = new RingTelemetryBuffer(256);
        TelemetrySample sample = new TelemetrySample(
                System.currentTimeMillis(),
                16.0,
                5.0,
                60,
                100,
                50,
                1000,
                0,
                0,
                0,
                0);

        int iterations = 10_000;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            buffer.add(sample);
        }
        long elapsed = System.nanoTime() - start;
        double avgMs = (elapsed / 1_000_000.0) / iterations;

        assertTrue(avgMs < 0.1, "Average add() overhead should be < 0.1ms");
    }
}
