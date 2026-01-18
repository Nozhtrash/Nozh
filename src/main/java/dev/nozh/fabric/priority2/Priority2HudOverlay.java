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

        int x = 6;
        int y = 6;
        int dy = 10;

        DeepScenarioSnapshot s = Priority2Signals.deepScenario.get();
        CpuGpuBottleneckClassifier.Result b = Priority2Signals.bottleneck.get();

        String bottleneckLine = "NOZH v0.2: bottleneck=unknown";
        if (b != null) {
            bottleneckLine = "NOZH v0.2: bottleneck=" + b.kind + " (conf="
                    + String.format(java.util.Locale.ROOT, "%.2f", b.confidence01) + ")";
        }

        ctx.drawTextWithShadow(client.textRenderer, bottleneckLine, x, y, 0xE6FFFFFF);
        y += dy;

        if (s != null) {
            ctx.drawTextWithShadow(client.textRenderer,
                    "Scenario: dim=" + s.dimensionKey + " hostiles=" + s.hostileMobsNearby,
                    x, y, 0xE6FFFFFF);
            y += dy;
            ctx.drawTextWithShadow(client.textRenderer,
                    "Blocks/min: place=" + String.format(java.util.Locale.ROOT, "%.1f", s.blocksPlacedPerMin)
                            + " break=" + String.format(java.util.Locale.ROOT, "%.1f", s.blocksBrokenPerMin),
                    x, y, 0xE6FFFFFF);
            y += dy;
        }

        int pendingN = pending.size();
        PendingSuggestionQueue.PendingSuggestion next = pending.peek();
        if (pendingN > 0 && next != null) {
            ctx.drawTextWithShadow(client.textRenderer,
                    "Pending(" + pendingN + "): " + next.id + "  [Press K]",
                    x, y, 0xFFFFE08A);
            y += dy;
            if (next.reason != null && !next.reason.isBlank()) {
                ctx.drawTextWithShadow(client.textRenderer, "Reason: " + next.reason, x, y, 0xFFD7D7D7);
            }
        }
    }
}
