package dev.nozh.core.telemetry;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that telemetry buffer never blocks (Contract 4 Rule 4.7).
 */
class TelemetryNeverBlocksTest {

    @Test
    void concurrentAddsNeverBlock() throws InterruptedException {
        RingTelemetryBuffer buffer = new RingTelemetryBuffer(100);
        int threadCount = 10;
        int samplesPerThread = 100;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger exceptions = new AtomicInteger(0);

        // Spawn threads that hammer the buffer
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for go signal

                    for (int j = 0; j < samplesPerThread; j++) {
                        // Use factory method with current timestamp
                        buffer.add(TelemetrySample.forTesting(16.0));
                    }
                } catch (Exception e) {
                    exceptions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // Go!
        doneLatch.await(); // Wait for completion

        assertEquals(0, exceptions.get(), "No thread should throw exception");

        // Buffer should have data (some may be dropped)
        TelemetrySnapshot snapshot = buffer.snapshot();
        assertNotNull(snapshot);
    }

    @Test
    void snapshotNeverBlocksAdd() throws InterruptedException {
        RingTelemetryBuffer buffer = new RingTelemetryBuffer(50);

        AtomicInteger snapshotCount = new AtomicInteger(0);
        AtomicInteger addCount = new AtomicInteger(0);

        // Thread continuously taking snapshots
        Thread snapshotThread = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                buffer.snapshot();
                snapshotCount.incrementAndGet();
            }
        });

        // Thread continuously adding
        Thread addThread = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                // Use factory method with current timestamp
                buffer.add(TelemetrySample.forTesting(16.0));
                addCount.incrementAndGet();
            }
        });

        snapshotThread.start();
        addThread.start();

        snapshotThread.join(1000); // Timeout
        addThread.join(1000); // Timeout

        assertTrue(snapshotCount.get() > 0, "Snapshots should complete");
        assertTrue(addCount.get() > 0, "Adds should complete");
    }
}
