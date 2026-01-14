package dev.nozh.fabric.telemetry;

import dev.nozh.NozhConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects real tick duration and render duration using Fabric events.
 *
 * Design goals:
 * - Zero allocation on hot path.
 * - Thread-safe reads via atomic raw bits.
 * - Safe degradation if rendering events are unavailable.
 */
public final class FabricFrameTickSampler {

    private final MinecraftClient client;

    private final AtomicLong tickStartNs = new AtomicLong(0L);
    private final AtomicLong renderStartNs = new AtomicLong(0L);

    private final AtomicLong lastTickMsRaw = new AtomicLong(Double.doubleToRawLongBits(-1.0));
    private final AtomicLong lastRenderMsRaw = new AtomicLong(Double.doubleToRawLongBits(-1.0));

    // Layer 1: Frame Pacing
    private volatile int consecutiveSlowFrames = 0;
    private static final double SLOW_FRAME_THRESHOLD_MS = 25.0; // 40 FPS

    // Layer 2: Entity Density
    private volatile int maxChunkEntityCount = 0;
    private volatile int denseChunkCount = 0;
    private int tickCounter = 0;
    private static final int DENSITY_SCAN_INTERVAL = 20; // 1 second
    private static final int DENSITY_THRESHOLD = 20; // Entities per chunk to consider "dense"

    // Benchmark state
    private long lastFrameEndNs = 0;

    public FabricFrameTickSampler(MinecraftClient client) {
        if (client == null) {
            throw new NullPointerException("MinecraftClient cannot be null");
        }
        this.client = client;

        ClientTickEvents.START_CLIENT_TICK.register(this::onTickStart);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTickEnd);

        try {
            WorldRenderEvents.START.register(this::onRenderStart);
            WorldRenderEvents.END.register(this::onRenderEnd);
        } catch (Exception e) {
            NozhConstants.LOGGER.warn("WorldRenderEvents unavailable; render-time sampling disabled", e);
        }
    }

    private void onTickStart(MinecraftClient ignored) {
        if (ignored != this.client) {
            return;
        }
        tickStartNs.set(System.nanoTime());
    }

    private void onTickEnd(MinecraftClient ignored) {
        if (ignored != this.client) {
            return;
        }

        long start = tickStartNs.get();
        if (start <= 0L) {
            return;
        }

        long dtNs = System.nanoTime() - start;
        if (dtNs < 0L) {
            return;
        }

        double ms = dtNs / 1_000_000.0;
        if (Double.isFinite(ms) && ms >= 0.0 && ms <= 1000.0) {
            lastTickMsRaw.set(Double.doubleToRawLongBits(ms));
        }

        // Layer 2: Entity Density Analysis (Every 1 second)
        if (tickCounter++ % DENSITY_SCAN_INTERVAL == 0 && client.world != null) {
            scanEntityDensity();
        }
    }

    private void scanEntityDensity() {
        try {
            Map<Long, Integer> chunkCounts = new HashMap<>();
            Iterable<Entity> entities = client.world.getEntities();

            for (Entity entity : entities) {
                long pos = net.minecraft.util.math.ChunkPos.toLong(entity.getChunkPos().x, entity.getChunkPos().z);
                chunkCounts.merge(pos, 1, Integer::sum);
            }

            int max = 0;
            int dense = 0;

            for (int count : chunkCounts.values()) {
                if (count > max) {
                    max = count;
                }
                if (count > DENSITY_THRESHOLD) {
                    dense++;
                }
            }

            this.maxChunkEntityCount = max;
            this.denseChunkCount = dense;
        } catch (Exception e) {
            // Fail silently to prevent crashing loop
        }
    }

    private void onRenderStart(WorldRenderContext ctx) {
        if (this.client.world == null) {
            return;
        }
        renderStartNs.set(System.nanoTime());
    }

    private void onRenderEnd(WorldRenderContext ctx) {
        if (this.client.world == null) {
            return;
        }

        long start = renderStartNs.get();
        if (start <= 0L) {
            return;
        }

        long dtNs = System.nanoTime() - start;
        if (dtNs < 0L) {
            return;
        }

        double ms = dtNs / 1_000_000.0;
        if (Double.isFinite(ms) && ms >= 0.0 && ms <= 10000.0) {
            lastRenderMsRaw.set(Double.doubleToRawLongBits(ms));

            // Layer 1 Logic: Consecutive Slow Frames
            if (ms > SLOW_FRAME_THRESHOLD_MS) {
                consecutiveSlowFrames++;
            } else {
                consecutiveSlowFrames = 0;
            }
        }

        // Calculate instantaneous FPS for benchmarking
        long end = System.nanoTime();
        if (lastFrameEndNs > 0) {
            long frameDelta = end - lastFrameEndNs;
            if (frameDelta > 0) {
                double fps = 1_000_000_000.0 / frameDelta;
                dev.nozh.core.profiler.BenchmarkSuite.getInstance().onFrame(fps);
            }
        }
        lastFrameEndNs = end;
    }

    // ... getters ...

    public int getConsecutiveSlowFrames() {
        return consecutiveSlowFrames;
    }

    public int getMaxChunkEntityCount() {
        return maxChunkEntityCount;
    }

    public int getDenseChunkCount() {
        return denseChunkCount;
    }

    public double getLastTickMs() {
        return Double.longBitsToDouble(lastTickMsRaw.get());
    }

    public double getLastRenderMs() {
        return Double.longBitsToDouble(lastRenderMsRaw.get());
    }

    public int getParticleCount() {
        if (client.particleManager == null)
            return 0;
        try {
            String s = client.particleManager.getDebugString();
            // Format: "Particles: 123"
            if (s.startsWith("Particles: ")) {
                return Integer.parseInt(s.substring(11).trim());
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public int getChunkUpdateCount() {
        if (client.worldRenderer == null)
            return 0;
        try {
            String s = client.worldRenderer.getChunksDebugString();
            // Format example: "C: 13/1000 ... U: 5 ..."
            int uIndex = s.indexOf("U: ");
            if (uIndex != -1) {
                int spaceAfter = s.indexOf(' ', uIndex + 3);
                if (spaceAfter == -1)
                    spaceAfter = s.length();
                return Integer.parseInt(s.substring(uIndex + 3, spaceAfter).trim());
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
