package dev.nozh.fabric.priority2;

import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.manual.PendingSuggestionQueue;
import dev.nozh.core.priority2.Priority2Signals;
import dev.nozh.core.scenario.DeepScenarioSnapshot;
import dev.nozh.core.system.CpuGpuBottleneckClassifier;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * v0.2: HUD overlay for Priority2 signals and manual suggestions.
 * Only renders when config.showHud AND config.showDebugOverlay are both true.
 */
public final class Priority2HudOverlay implements HudRenderCallback {

    private final MinecraftClient client;
    private final PendingSuggestionQueue pending;

    // Cache fields
    private long lastUpdateMillis = 0;
    private static final long UPDATE_INTERVAL_MS = 200; // Update 5 times per second

    // Cached strings
    private String cachedBottleneck = "";
    private String cachedScenario = "";
    private String cachedBlocks = "";
    private String cachedPendingTitle = "";
    private String cachedReason = "";

    public Priority2HudOverlay(MinecraftClient client, PendingSuggestionQueue pending) {
        if (client == null)
            throw new NullPointerException("client");
        if (pending == null)
            throw new NullPointerException("pending");
        this.client = client;
        this.pending = pending;
    }

    public static void register(MinecraftClient client, PendingSuggestionQueue pending) {
        HudRenderCallback.EVENT.register(new Priority2HudOverlay(client, pending));
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        // Respect unified HUD toggle
        NozhConfig config = ConfigManager.getConfig();
        if (config == null || !config.showHud || !config.showDebugOverlay) {
            return;
        }

        if (client == null || client.options == null || client.textRenderer == null)
            return;
        if (client.player == null)
            return;

        updateCacheIfNeeded();

        int x = 6;
        int y = 6;
        int dy = 10;

        ctx.drawTextWithShadow(client.textRenderer, cachedBottleneck, x, y, 0xE6FFFFFF);
        y += dy;

        if (!cachedScenario.isEmpty()) {
            ctx.drawTextWithShadow(client.textRenderer, cachedScenario, x, y, 0xE6FFFFFF);
            y += dy;
            ctx.drawTextWithShadow(client.textRenderer, cachedBlocks, x, y, 0xE6FFFFFF);
            y += dy;
        }

        if (!cachedPendingTitle.isEmpty()) {
            ctx.drawTextWithShadow(client.textRenderer, cachedPendingTitle, x, y, 0xFFFFE08A);
            y += dy;
            if (!cachedReason.isEmpty()) {
                ctx.drawTextWithShadow(client.textRenderer, cachedReason, x, y, 0xFFD7D7D7);
            }
        }
    }

    private void updateCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastUpdateMillis < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateMillis = now;

        // 1. Bottleneck
        CpuGpuBottleneckClassifier.Result b = Priority2Signals.bottleneck.get();
        if (b != null) {
            cachedBottleneck = "NOZH v0.2: bottleneck=" + b.kind + " (conf="
                    + String.format(java.util.Locale.ROOT, "%.2f", b.confidence01) + ")";
        } else {
            cachedBottleneck = "NOZH v0.2: bottleneck=unknown";
        }

        // 2. Scenario
        DeepScenarioSnapshot s = Priority2Signals.deepScenario.get();
        if (s != null) {
            cachedScenario = "Scenario: dim=" + s.dimensionKey + " hostiles=" + s.hostileMobsNearby;
            cachedBlocks = "Blocks/min: place=" + String.format(java.util.Locale.ROOT, "%.1f", s.blocksPlacedPerMin)
                    + " break=" + String.format(java.util.Locale.ROOT, "%.1f", s.blocksBrokenPerMin);
        } else {
            cachedScenario = "";
            cachedBlocks = "";
        }

        // 3. Pending
        int pendingN = pending.size();
        PendingSuggestionQueue.PendingSuggestion next = pending.peek();
        if (pendingN > 0 && next != null) {
            cachedPendingTitle = "Pending(" + pendingN + "): " + next.id + "  [Press K]";
            cachedReason = (next.reason != null && !next.reason.isBlank()) ? "Reason: " + next.reason : "";
        } else {
            cachedPendingTitle = "";
            cachedReason = "";
        }
    }
}
