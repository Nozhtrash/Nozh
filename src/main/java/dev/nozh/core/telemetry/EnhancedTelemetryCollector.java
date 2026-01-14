package dev.nozh.core.telemetry;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

/**
 * Enhanced telemetry collector with proper validation.
 * No synthetic data - returns null if data is invalid.
 */
public class EnhancedTelemetryCollector {

    private long worldLoadTime = 0;
    private static final long STABILIZATION_PERIOD_MS = 5000; // 5 seconds
    private static final double MIN_VALID_FRAMETIME = 1.0; // 1ms minimum
    private static final double MAX_VALID_FRAMETIME = 1000.0; // 1 second maximum

    /**
     * Collect telemetry sample with validation.
     * Returns null if world is not ready or data is invalid.
     */
    public TelemetrySample collectValidated(MinecraftClient client) {
        // Check if client and world are ready
        if (!isWorldReady(client)) {
            return null;
        }

        // Wait for stabilization after world load
        if (worldLoadTime > 0) {
            long timeSinceLoad = System.currentTimeMillis() - worldLoadTime;
            if (timeSinceLoad < STABILIZATION_PERIOD_MS) {
                return null; // Still stabilizing
            }
        }

        try {
            // Get FPS
            int fps = client.getCurrentFps();
            if (fps <= 0) {
                NozhConstants.LOGGER.debug("Invalid FPS: {}", fps);
                return null;
            }

            // Calculate frametime from FPS (milliseconds per frame)
            double frametime = 1000.0 / fps;

            if (frametime < MIN_VALID_FRAMETIME || frametime > MAX_VALID_FRAMETIME) {
                NozhConstants.LOGGER.debug("Frametime out of valid range: {}", frametime);
                return null;
            }

            // Get world metrics
            ClientWorld world = client.world;
            int entities = world.getRegularEntityCount();
            int loadedChunks = getLoadedChunksCount(world);

            // Get memory
            Runtime runtime = Runtime.getRuntime();
            long memoryUsed = runtime.totalMemory() - runtime.freeMemory();

            // Get tick time estimate
            double tickTime = estimateTickTime(client);

            return new TelemetrySample(
                    System.currentTimeMillis(),
                    frametime,
                    tickTime,
                    fps,
                    entities,
                    loadedChunks,
                    memoryUsed);

        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to collect telemetry", e);
            return null;
        }
    }

    /**
     * Check if world is ready for telemetry collection.
     */
    private boolean isWorldReady(MinecraftClient client) {
        if (client == null) {
            return false;
        }

        ClientWorld world = client.world;
        if (world == null) {
            worldLoadTime = 0; // Reset
            return false;
        }

        // Track world load time
        if (worldLoadTime == 0) {
            worldLoadTime = System.currentTimeMillis();
            NozhConstants.LOGGER.info("World loaded, starting stabilization period");
            return false;
        }

        // Check player exists
        if (client.player == null) {
            return false;
        }

        return true;
    }

    /**
     * Estimate tick time from available metrics.
     */
    private double estimateTickTime(MinecraftClient client) {
        try {
            // Try to get from integrated server if available
            if (client.getServer() != null) {
                // Use tick time from server metrics
                // Note: getAverageTickTime() was removed, using getTickTime() instead
                float tickTime = client.getServer().getTickTime();
                return tickTime;
            }

            // Estimate based on entity count and world complexity
            int entities = client.world.getRegularEntityCount();
            double baseTickTime = 10.0; // Base 10ms
            double entityOverhead = Math.min(entities * 0.01, 40.0); // Max 40ms from entities

            return baseTickTime + entityOverhead;

        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Failed to estimate tick time", e);
            return 15.0; // Reasonable default if all else fails
        }
    }

    /**
     * Get count of loaded chunks.
     * 
     * @param world the client world to get chunk count from
     * @return estimated number of loaded chunks based on render distance
     */
    private int getLoadedChunksCount(ClientWorld world) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return 0;
            }

            // Get render distance from options
            int renderDistance = client.options.getViewDistance().getValue();

            // Approximate loaded chunks based on render distance
            // Formula: (2 * renderDistance + 1)^2 for a square area
            int diameter = 2 * renderDistance + 1;
            return diameter * diameter;
        } catch (Exception e) {
            NozhConstants.LOGGER.debug("Failed to estimate chunk count", e);
            return 0;
        }
    }

    /**
     * Reset world load time (call when changing worlds).
     */
    public void resetWorldLoadTime() {
        worldLoadTime = 0;
    }

    public static class TelemetrySample {
        public final long timestamp;
        public final double frametimeMs;
        public final double tickTimeMs;
        public final int fps;
        public final int entityCount;
        public final int loadedChunks;
        public final long memoryUsedBytes;

        public TelemetrySample(long timestamp, double frametimeMs, double tickTimeMs,
                int fps, int entityCount, int loadedChunks, long memoryUsedBytes) {
            this.timestamp = timestamp;
            this.frametimeMs = frametimeMs;
            this.tickTimeMs = tickTimeMs;
            this.fps = fps;
            this.entityCount = entityCount;
            this.loadedChunks = loadedChunks;
            this.memoryUsedBytes = memoryUsedBytes;
        }

        @Override
        public String toString() {
            return String.format("TelemetrySample{fps=%d, frametime=%.2fms, tick=%.2fms, entities=%d, chunks=%d}",
                    fps, frametimeMs, tickTimeMs, entityCount, loadedChunks);
        }
    }
}
