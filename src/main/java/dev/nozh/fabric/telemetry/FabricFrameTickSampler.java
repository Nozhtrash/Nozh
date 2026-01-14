package dev.nozh.fabric.telemetry;

import dev.nozh.NozhConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

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
