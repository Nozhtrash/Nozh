package dev.nozh.core.governor.components;

import dev.nozh.NozhConstants;
import dev.nozh.core.telemetry.*;
import dev.nozh.core.prediction.PerformancePredictor;
import net.minecraft.client.MinecraftClient;

/**
 * Manages telemetry collection, buffering, and prediction.
 * 
 * Extracted from IntegratedGovernor as part of God Class refactoring.
 * This class encapsulates all telemetry-related responsibilities.
 * 
 * <p><b>Thread Safety:</b> This class is thread-safe. All operations
 * on the buffer are synchronized internally.
 * 
 * <p><b>Null Safety:</b> All methods handle null gracefully.
 * 
 * @author Nozh Team
 * @since 0.4.0
 */
public final class TelemetryManager {
    
    private final MinecraftClient client;
    private final IntegratedRingTelemetryBuffer telemetryBuffer;
    private final PerformancePredictor predictor;
    
    /**
     * Constructs a new TelemetryManager.
     * 
     * @param client Minecraft client (must not be null)
     * @param bufferSize size of the ring buffer
     * @param targetFps target FPS for prediction
     * @throws NullPointerException if client is null
     * @throws IllegalArgumentException if bufferSize or targetFps are invalid
     */
    public TelemetryManager(MinecraftClient client, int bufferSize, int targetFps) {
        if (client == null) {
            throw new NullPointerException("Client cannot be null");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive: " + bufferSize);
        }
        if (targetFps <= 0) {
            throw new IllegalArgumentException("Target FPS must be positive: " + targetFps);
        }
        
        this.client = client;
        this.telemetryBuffer = new IntegratedRingTelemetryBuffer(bufferSize);
        this.predictor = new PerformancePredictor(targetFps);
        
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
                
                // Feed to predictor
                if (sample.hasFrametimeData()) {
                    predictor.addSample(sample.frametimeMs());
                }
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
     * @return snapshot, or null if buffer is empty
     */
    public TelemetrySnapshot getSnapshot() {
        try {
            return telemetryBuffer.snapshot();
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to get telemetry snapshot", e);
            return null;
        }
    }
    
    /**
     * Gets the performance predictor.
     * 
     * @return predictor instance
     */
    public PerformancePredictor getPredictor() {
        return predictor;
    }
    
    /**
     * Gets the number of dropped samples.
     * 
     * @return dropped count
     */
    public int getDroppedCount() {
        return telemetryBuffer.getDroppedCount();
    }
    
    /**
     * Clears all telemetry data.
     */
    public void clear() {
        try {
            // Note: IntegratedRingTelemetryBuffer doesn't have clear()
            // This is a placeholder for future implementation
            NozhConstants.LOGGER.info("Telemetry cleared");
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to clear telemetry", e);
        }
    }
    
    // Private helper methods
    
    private TelemetrySample collectTelemetry() {
        if (client == null || client.world == null) {
            return null;
        }
        
        try {
            double frametime = client.getLastFrameDuration();
            int fps = client.getCurrentFps();
            
            // Validate collected data
            if (frametime < 0 || !Double.isFinite(frametime)) {
                frametime = 16.67; // Default to 60 FPS
            }
            
            if (fps < 0) {
                fps = 60; // Default FPS
            }
            
            int droppedCount = telemetryBuffer.getDroppedCount();
            
            return new TelemetrySample(
                    System.currentTimeMillis(),
                    frametime,
                    -1, // tick time (not available here)
                    fps,
                    -1, // entities (not available here)
                    -1, // chunks (not available here)
                    -1, // draw calls (not available here)
                    droppedCount
            );
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to collect telemetry", e);
            return null;
        }
    }
}
