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
import dev.nozh.core.safety.NozhState;
import dev.nozh.core.safety.StateManager;
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

        int x = resolveAnchorX(config, maxWidth, client.getWindow().getScaledWidth());
        int y = resolveAnchorY(config, lineHeight, lines.size(), client.getWindow().getScaledHeight());

        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            int color = i == 0 ? 0xFFFFFF : 0xE0E0E0;
            context.drawTextWithShadow(textRenderer, line, x, y, color);
            y += lineHeight;
        }
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
        lines.add(Text.translatable("nozh.hud.mode", viewModel.governorMode().name()));
        lines.add(Text.translatable("nozh.hud.scenario", viewModel.currentScenario().name()));
        lines.add(Text.translatable("nozh.hud.metrics.p95", formatMs(viewModel.p95FrametimeMs())));
        lines.add(Text.translatable("nozh.hud.metrics.spikes", viewModel.spikeCount()));

        lines.add(Text.translatable("nozh.hud.last_action", resolveLastAction()));
        lines.add(Text.translatable("nozh.hud.suggestion", resolveSuggestion(state)));
        return lines;
    }

    private String resolveLastAction() {
        NozhState state = StateManager.getState();
        if (state == null || state.executionHistory.isEmpty()) {
            return Text.translatable("nozh.hud.last_action.none").getString();
        }

        var last = state.executionHistory.get(state.executionHistory.size() - 1);
        return formatAction(last.type().name(), last.oldValue(), last.newValue());
    }

    private String resolveSuggestion(RuntimeState state) {
        if (state == null || state.suggestedAction().isEmpty()) {
            return Text.translatable("nozh.hud.suggestion.none").getString();
        }
        var pending = state.suggestedAction().get();
        String summary = formatAction(pending.capability().name(), pending.newValue().toString());
        return Text.translatable("nozh.hud.suggestion.apply_hint", summary).getString();
    }

    private String formatAction(String capability, String previousValue, String newValue) {
        return capability + ": " + previousValue + " → " + newValue;
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
