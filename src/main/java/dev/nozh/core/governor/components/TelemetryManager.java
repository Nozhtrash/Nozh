package dev.nozh.core.governor.components;

import dev.nozh.NozhConstants;
import dev.nozh.core.telemetry.*;
import net.minecraft.client.MinecraftClient;

/**
 * Manages telemetry collection and buffering.
 * 
 * Simplified version that works with existing main branch code.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public final class TelemetryManager {

    private final MinecraftClient client;
    private final IntegratedRingTelemetryBuffer telemetryBuffer;
    private final int targetFps;

    /**
     * Constructs a new TelemetryManager.
     * 
     * @param client     Minecraft client
     * @param bufferSize size of the ring buffer
     * @param targetFps  target FPS
     */
    public TelemetryManager(MinecraftClient client, int bufferSize, int targetFps) {
        if (client == null) {
            throw new NullPointerException("Client cannot be null");
        }
        if (bufferSize <= 0 || targetFps <= 0) {
            throw new IllegalArgumentException("Buffer size and targetFps must be positive");
        }

        this.client = client;
        this.telemetryBuffer = new IntegratedRingTelemetryBuffer(bufferSize);
        this.targetFps = targetFps;

        NozhConstants.LOGGER.info("TelemetryManager initialized (buffer={}, targetFps={})",
                bufferSize, targetFps);
    }

    /**
     * Collects and stores current telemetry sample.
     * 
     * @return the collected sample, or null if collection failed
     */
    public TelemetrySample collectAndStore() {
        if (client == null || client.world == null) {
            return null;
        }

        try {
            TelemetrySample sample = collectTelemetry();
            if (sample != null) {
                telemetryBuffer.add(sample);
            }
            return sample;

        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to collect telemetry", e);
            return null;
        }
    }

    /**
     * Gets a snapshot of recent telemetry.
     * 
     * @return snapshot, or empty snapshot if buffer is empty
     */
    public TelemetrySnapshot getSnapshot() {
        try {
            TelemetrySnapshot snapshot = telemetryBuffer.snapshot();
            return snapshot != null ? snapshot : TelemetrySnapshot.EMPTY;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to get telemetry snapshot", e);
            return TelemetrySnapshot.EMPTY;
        }
    }

    /**
     * Gets the number of dropped samples.
     * 
     * @return dropped count
     */
    public int getDroppedCount() {
        try {
            return telemetryBuffer.getDroppedCount();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Clears all telemetry data.
     */
    public void clear() {
        NozhConstants.LOGGER.debug("Telemetry cleared");
    }

    private TelemetrySample collectTelemetry() {
        if (client == null || client.world == null) {
            return null;
        }

        try {
            double frametime = client.getLastFrameDuration();
            int fps = client.getCurrentFps();

            if (frametime < 0 || !Double.isFinite(frametime)) {
                frametime = 16.67;
            }

            if (fps < 0) {
                fps = targetFps;
            }

            int droppedCount = telemetryBuffer.getDroppedCount();

            return new TelemetrySample(
                    System.currentTimeMillis(),
                    frametime,
                    -1,
                    fps,
                    -1,
                    -1,
                    -1, // drawCalls
                    droppedCount, // droppedSamples
                    0, // consecutiveSlowFrames
                    0, // maxChunkEntityCount
                    0 // denseChunkCount
            );
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to collect telemetry", e);
            return null;
        }
    }
}
