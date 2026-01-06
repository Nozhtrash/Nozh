package dev.nozh.client.hud;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import dev.nozh.core.ui.HudViewModel;
import dev.nozh.core.ui.HudViewModelBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class NozhHudRenderer implements HudRenderCallback {

    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 8;

    private final StateStore stateStore;
    private final ProviderRegistry providerRegistry;
    private final Supplier<PerfSnapshot> perfSnapshotSupplier;

    public NozhHudRenderer(StateStore stateStore, ProviderRegistry providerRegistry,
            Supplier<PerfSnapshot> perfSnapshotSupplier) {
        this.stateStore = stateStore;
        this.providerRegistry = providerRegistry;
        this.perfSnapshotSupplier = perfSnapshotSupplier;
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        NozhConfig config = ConfigManager.getConfig();
        if (config == null || !config.showHud) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        RuntimeState state = stateStore != null ? stateStore.snapshotSafe() : RuntimeState.defaults();
        TelemetrySnapshot telemetry = buildTelemetry();
        HudViewModel viewModel = HudViewModelBuilder.build(
                state,
                telemetry,
                List.of(),
                HardwareTier.MEDIUM,
                providerRegistry);

        TextRenderer textRenderer = client.textRenderer;
        int lineHeight = textRenderer.fontHeight + 2;
        List<Text> lines = buildHudLines(viewModel, state);
        int maxWidth = 0;
        for (Text line : lines) {
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(line));
        }

        float scale = (float) config.hudScale;
        int scaledMaxWidth = Math.round(maxWidth * scale);
        int scaledLineHeight = Math.round(lineHeight * scale);
        int x = resolveAnchorX(config, scaledMaxWidth, client.getWindow().getScaledWidth());
        int y = resolveAnchorY(config, scaledLineHeight, lines.size(), client.getWindow().getScaledHeight());

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);

        int drawX = Math.round(x / scale);
        int drawY = Math.round(y / scale);
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            int color = i == 0 ? 0xFFFFFF : 0xE0E0E0;
            context.drawTextWithShadow(textRenderer, line, drawX, drawY, color);
            drawY += lineHeight;
        }
        context.getMatrices().pop();
    }

    private TelemetrySnapshot buildTelemetry() {
        if (perfSnapshotSupplier == null) {
            return TelemetrySnapshot.EMPTY;
        }
        PerfSnapshot snapshot = perfSnapshotSupplier.get();
        if (snapshot == null) {
            return TelemetrySnapshot.EMPTY;
        }
        return TelemetrySnapshot.of(
                snapshot.avgFrametimeMs(),
                snapshot.p95FrametimeMs(),
                snapshot.spikeCount(),
                snapshot.sampleCount(),
                0);
    }

    private List<Text> buildHudLines(HudViewModel viewModel, RuntimeState state) {
        List<Text> lines = new java.util.ArrayList<>();
        lines.add(Text.translatable("nozh.hud.title"));
        lines.add(Text.translatable("nozh.hud.mode", resolveMode(state)));
        lines.add(Text.translatable("nozh.hud.scenario", Text.translatable(viewModel.scenarioKey())));
        lines.add(Text.translatable("nozh.hud.metrics.fps", formatFps(viewModel.avgFrametimeMs())));
        lines.add(Text.translatable(
                "nozh.hud.metrics.p95_spikes",
                formatMs(viewModel.p95FrametimeMs()),
                viewModel.spikeCount()));

        lines.add(Text.translatable("nozh.hud.last_action", resolveLastAction(viewModel)));
        lines.add(Text.translatable("nozh.hud.last_outcome", resolveLastOutcome(viewModel)));
        NozhConfig config = ConfigManager.getConfig();
        if (config == null || config.showHudSuggestions) {
            if (state != null && !state.autoTuning()) {
                lines.add(Text.translatable("nozh.hud.suggestion", resolveSuggestion(state)));
            }
        }
        return lines;
    }

    private String resolveMode(RuntimeState state) {
        if (state == null) {
            return Text.translatable("nozh.hud.mode.unknown").getString();
        }
        if (!state.enabled()) {
            return Text.translatable("nozh.hud.mode.off").getString();
        }
        if (state.safeMode()) {
            return Text.translatable("nozh.hud.mode.safemode").getString();
        }
        if (!state.autoTuning()) {
            return Text.translatable("nozh.hud.mode.manual").getString();
        }
        return Text.translatable("nozh.hud.mode.auto").getString();
    }

    private String resolveLastAction(HudViewModel viewModel) {
        if (viewModel == null || viewModel.lastActionSummary() == null || viewModel.lastActionSummary().isBlank()) {
            return Text.translatable("nozh.hud.last_action.none").getString();
        }
        return viewModel.lastActionSummary();
    }

    private String resolveLastOutcome(HudViewModel viewModel) {
        if (viewModel == null || viewModel.lastActionOutcome() == null || viewModel.lastActionOutcome().isBlank()) {
            return Text.translatable("nozh.hud.last_outcome.none").getString();
        }
        String outcome = viewModel.lastActionOutcome().trim().toUpperCase();
        return Text.translatable("nozh.hud.outcome." + outcome.toLowerCase()).getString();
    }

    private String resolveSuggestion(RuntimeState state) {
        if (state == null || state.suggestedActions() == null || state.suggestedActions().isEmpty()) {
            return Text.translatable("nozh.hud.suggestion.none").getString();
        }
        var pending = state.suggestedActions().get(0);
        String summary = formatAction(pending.capability().name(), pending.newValue().toString());
        String reason = state.lastDecisionReason();
        if (reason != null && !reason.isBlank()) {
            summary = summary + " (" + reason + ")";
        }
        int remaining = state.suggestedActions().size() - 1;
        if (remaining > 0) {
            return Text.translatable("nozh.hud.suggestion.apply_hint_many", summary, remaining).getString();
        }
        return Text.translatable("nozh.hud.suggestion.apply_hint", summary).getString();
    }

    private String formatAction(String capability, String value) {
        return capability + " → " + value;
    }

    private String formatMs(double value) {
        if (Double.isNaN(value) || value <= 0) {
            return "--";
        }
        return String.format("%.1f", value);
    }

    private String formatFps(double avgMs) {
        if (Double.isNaN(avgMs) || avgMs <= 0) {
            return "--";
        }
        double fps = 1000.0 / avgMs;
        return String.format("%.0f", fps);
    }

    private int resolveAnchorX(NozhConfig config, int maxWidth, int screenWidth) {
        int base = PADDING_X + config.hudOffsetX;
        if ("TOP_RIGHT".equals(config.hudAnchor) || "BOTTOM_RIGHT".equals(config.hudAnchor)) {
            return screenWidth - maxWidth - PADDING_X + config.hudOffsetX;
        }
        return base;
    }

    private int resolveAnchorY(NozhConfig config, int lineHeight, int totalLines, int screenHeight) {
        int base = PADDING_Y + config.hudOffsetY;
        if ("BOTTOM_LEFT".equals(config.hudAnchor) || "BOTTOM_RIGHT".equals(config.hudAnchor)) {
            return screenHeight - (lineHeight * totalLines) - PADDING_Y + config.hudOffsetY;
        }
        return base;
    }
}
