package dev.nozh.core.telemetry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for telemetry buffer dropping behavior (Contract 4 Rule 4.7).
 */
class TelemetryDropTest {

    @Test
    void bufferDropsWhenFull() {
        RingTelemetryBuffer buffer = new RingTelemetryBuffer(3);

        // Fill buffer
        buffer.add(sample(1, 10.0));
        buffer.add(sample(2, 20.0));
        buffer.add(sample(3, 30.0));

        assertEquals(0, buffer.getDroppedCount(), "No drops yet");

        // Overflow - should drop
        buffer.add(sample(4, 40.0));

        assertEquals(1, buffer.getDroppedCount(), "Should have dropped 1 sample");

        // More overflow
        buffer.add(sample(5, 50.0));
        buffer.add(sample(6, 60.0));

        assertEquals(3, buffer.getDroppedCount(), "Should have dropped 3 total");
    }

    @Test
    void snapshotWorksAfterDrops() {
        RingTelemetryBuffer buffer = new RingTelemetryBuffer(2);

        buffer.add(sample(1, 10.0));
        buffer.add(sample(2, 20.0));
        buffer.add(sample(3, 30.0)); // Drops oldest
        buffer.add(sample(4, 40.0)); // Drops oldest

        TelemetrySnapshot snapshot = buffer.snapshot();

        assertNotNull(snapshot);
        assertTrue(snapshot.avgFrametimeMs() > 0);
        assertEquals(2, snapshot.droppedSamples());
    }

    @Test
    void nullSampleDoesNotThrow() {
        RingTelemetryBuffer buffer = new RingTelemetryBuffer(10);

        assertDoesNotThrow(() -> buffer.add(null), "Null sample should be silently ignored");

        TelemetrySnapshot snapshot = buffer.snapshot();
        assertEquals(TelemetrySnapshot.EMPTY, snapshot);
    }

    private TelemetrySample sample(long timestamp, double frametime) {
        return new TelemetrySample(timestamp, frametime, 16.0, 60, 100, 50, 0, 0);
    }
}
