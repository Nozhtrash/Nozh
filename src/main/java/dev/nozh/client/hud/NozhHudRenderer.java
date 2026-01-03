package dev.nozh.client.hud;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.state.PendingAction;
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
        int x = PADDING_X;
        int y = PADDING_Y;

        context.drawTextWithShadow(textRenderer, Text.translatable("nozh.hud.title"), x, y, 0xFFFFFF);
        y += lineHeight;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("nozh.hud.mode", viewModel.governorMode().name()), x, y, 0xE0E0E0);
        y += lineHeight;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("nozh.hud.scenario", viewModel.currentScenario().name()), x, y, 0xE0E0E0);
        y += lineHeight;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("nozh.hud.metrics.avg", formatMs(viewModel.avgFrametimeMs())), x, y, 0xE0E0E0);
        y += lineHeight;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("nozh.hud.metrics.p95", formatMs(viewModel.p95FrametimeMs())), x, y, 0xE0E0E0);
        y += lineHeight;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("nozh.hud.metrics.spikes", viewModel.spikeCount()), x, y, 0xE0E0E0);
        y += lineHeight;

        if (state.pendingAction().isPresent()) {
            y = drawPendingAction(context, textRenderer, x, y, lineHeight, state.pendingAction().get());
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

    private int drawPendingAction(DrawContext context, TextRenderer textRenderer, int x, int y,
            int lineHeight, PendingAction pendingAction) {
        String actionSummary = formatAction(pendingAction.capability().toString(),
                pendingAction.newValue().toString());
        String suggestion = pendingAction.previousValue()
                .map(value -> Text.translatable("nozh.hud.suggestion.revert", value.toString()).getString())
                .orElseGet(() -> Text.translatable("nozh.hud.suggestion.pending").getString());

        context.drawTextWithShadow(textRenderer,
                Text.translatable("nozh.hud.last_action", actionSummary), x, y, 0xE0E0E0);
        y += lineHeight;

        context.drawTextWithShadow(textRenderer,
                Text.translatable("nozh.hud.suggestion", suggestion), x, y, 0xE0E0E0);
        y += lineHeight;
        return y;
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
}
