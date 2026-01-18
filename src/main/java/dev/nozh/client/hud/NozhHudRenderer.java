package dev.nozh.client.hud;

import dev.nozh.api.PerfSnapshot;
import dev.nozh.core.capability.ProviderRegistry;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.preset.HardwareTier;
import dev.nozh.core.profiler.PerfDiagnosticsSnapshot;
import dev.nozh.core.profiler.PerfManager;
import dev.nozh.core.profiler.RenderPhase;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import dev.nozh.core.telemetry.TelemetrySnapshot;
import dev.nozh.core.ui.HudMode;
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
    private final Supplier<TelemetrySnapshot> telemetrySupplier;
    private final PerfManager perfManager;
    private final Supplier<String> exportKeySupplier;
    private final FrametimeGraphRenderer graphRenderer;
    private double lastHudRenderMs = 0.0;
    private long lastRenderTime = 0;

    public NozhHudRenderer(StateStore stateStore, ProviderRegistry providerRegistry,
            Supplier<TelemetrySnapshot> telemetrySupplier, PerfManager perfManager,
            Supplier<String> exportKeySupplier) {
        this.stateStore = stateStore;
        this.providerRegistry = providerRegistry;
        this.telemetrySupplier = telemetrySupplier;
        this.perfManager = perfManager;
        this.exportKeySupplier = exportKeySupplier;
        this.graphRenderer = new FrametimeGraphRenderer();
    }

    private float slideProgress = 0f;

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        NozhConfig config = ConfigManager.getConfig();
        if (config == null || !config.showHud) {
            slideProgress = 0f; // Reset when hidden
            return;
        }

        // Animation logic
        if (slideProgress < 1.0f) {
            slideProgress += 0.05f; // approx 20 frames (0.33s)
            if (slideProgress > 1.0f)
                slideProgress = 1.0f;
        }

        float smoothedProgress = (float) (1.0 - Math.pow(1.0 - slideProgress, 3)); // Cubic ease-out

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        long renderStartNanos = System.nanoTime();

        // Update graph (God Mode Polish)
        if (lastRenderTime != 0) {
            double frameTimeMs = (renderStartNanos - lastRenderTime) / 1_000_000.0;
            graphRenderer.addSample(frameTimeMs);
        }
        lastRenderTime = renderStartNanos;

        if (perfManager != null) {
            perfManager.onRenderPhaseStart(RenderPhase.HUD);
        }

        RuntimeState state = stateStore != null ? stateStore.snapshotSafe() : RuntimeState.defaults();
        TelemetrySnapshot telemetry = buildTelemetry();
        PerfDiagnosticsSnapshot diagnostics = buildDiagnostics();
        HudViewModel viewModel = HudViewModelBuilder.build(
                state,
                telemetry,
                diagnostics,
                List.of(),
                HardwareTier.MEDIUM,
                providerRegistry);

        TextRenderer textRenderer = client.textRenderer;
        int lineHeight = textRenderer.fontHeight + 2;
        HudMode hudMode = HudMode.fromConfig(config.hudMode);
        List<Text> lines = buildHudLines(viewModel, state, hudMode);
        int maxWidth = 0;
        for (Text line : lines) {
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(line));
        }

        // Add extra space for graph if needed
        int graphHeight = (hudMode == HudMode.EXPERT || hudMode == HudMode.ANALYST) ? 65 : 0;

        float scale = (float) config.hudScale;
        int scaledMaxWidth = Math.round(maxWidth * scale);
        // Graph is typically wider, check graph width
        if (graphHeight > 0) {
            scaledMaxWidth = Math.max(scaledMaxWidth, Math.round(200 * scale));
        }

        int totalContentHeight = (lines.size() * lineHeight) + graphHeight;
        int scaledContentHeight = Math.round(totalContentHeight * scale);

        int x = resolveAnchorX(config, scaledMaxWidth, client.getWindow().getScaledWidth());

        // Apply visual slide-in
        int slideOffset = Math.round(20 * (1.0f - smoothedProgress));

        int y;
        if ("BOTTOM_LEFT".equals(config.hudAnchor) || "BOTTOM_RIGHT".equals(config.hudAnchor)) {
            // Bottom align: ScreenHeight - ContentHeight - Padding + OffsetY + SlideOffset
            y = client.getWindow().getScaledHeight() - scaledContentHeight - PADDING_Y + config.hudOffsetY;
            y += slideOffset;
        } else {
            // Top align: Padding + OffsetY - SlideOffset
            y = resolveAnchorY(config, scaledContentHeight, 1, client.getWindow().getScaledHeight());
            y -= slideOffset;
        }

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);

        int drawX = Math.round(x / scale);
        int drawY = Math.round(y / scale);

        // Draw Text
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            int color = i == 0 ? 0xFFFFFF : 0xE0E0E0;

            context.drawTextWithShadow(textRenderer, line, drawX, drawY, color);
            drawY += lineHeight;
        }

        // Draw Graph
        if (graphHeight > 0) {
            drawY += 4; // Spacing
            graphRenderer.render(context, drawX, drawY);
        }

        context.getMatrices().pop();

        if (perfManager != null) {
            perfManager.onRenderPhaseEnd(RenderPhase.HUD);
        }

        long renderEndNanos = System.nanoTime();
        lastHudRenderMs = (renderEndNanos - renderStartNanos) / 1_000_000.0;
    }

    private TelemetrySnapshot buildTelemetry() {
        if (telemetrySupplier == null) {
            return TelemetrySnapshot.EMPTY;
        }
        TelemetrySnapshot snapshot = telemetrySupplier.get();
        return snapshot != null ? snapshot : TelemetrySnapshot.EMPTY;
    }

    private PerfDiagnosticsSnapshot buildDiagnostics() {
        if (perfManager == null) {
            return PerfDiagnosticsSnapshot.empty();
        }
        PerfDiagnosticsSnapshot snapshot = perfManager.getDiagnosticsSnapshot();
        return snapshot != null ? snapshot : PerfDiagnosticsSnapshot.empty();
    }

    private List<Text> buildHudLines(HudViewModel viewModel, RuntimeState state, HudMode hudMode) {
        List<Text> lines = new java.util.ArrayList<>();
        lines.add(Text.translatable("nozh.hud.title", Text.translatable(hudMode.translationKey())));
        lines.add(Text.translatable("nozh.hud.mode", resolveMode(state)));
        if (hudMode != HudMode.COMPACT) {
            lines.add(Text.translatable("nozh.hud.scenario", Text.translatable(viewModel.scenarioKey())));
        }
        lines.add(Text.translatable("nozh.hud.metrics.fps", formatFps(viewModel.avgFrametimeMs())));
        if (hudMode != HudMode.COMPACT) {
            // New Phase 2 Metrics
            lines.add(Text.translatable(
                    "nozh.hud.metrics.p99",
                    formatMs(viewModel.p99FrametimeMs())));
            lines.add(Text.translatable(
                    "nozh.hud.metrics.variance",
                    formatMs(viewModel.frametimeVariance())));

            lines.add(Text.translatable(
                    "nozh.hud.metrics.p95_spikes",
                    formatMs(viewModel.p95FrametimeMs()),
                    viewModel.spikeCount()));
        }
        lines.add(Text.translatable("nozh.hud.bound", Text.translatable(viewModel.currentBound())));
        lines.add(Text.translatable(
                "nozh.hud.stutter.cause",
                Text.translatable(viewModel.stutterCauseKey()),
                formatStutterDetail(viewModel.stutterDetail(), viewModel.stutterConfidence())));
        if (hudMode == HudMode.EXPERT) {
            lines.add(Text.translatable(
                    "nozh.hud.metrics.gc",
                    formatMsAllowZero(viewModel.gcRecentMs()),
                    formatPercent(viewModel.gcPressureScore())));
            lines.add(Text.translatable(
                    "nozh.hud.metrics.pauses",
                    viewModel.pauseCount(),
                    formatMsAllowZero(viewModel.pauseMaxMs())));
            lines.add(Text.translatable(
                    "nozh.hud.metrics.render_hot",
                    Text.translatable(viewModel.hottestRenderPhaseKey()),
                    formatMsAllowZero(viewModel.hottestRenderPhaseMs()),
                    viewModel.hottestRenderPhaseTicks()));
        }
        if (hudMode != HudMode.COMPACT) {
            lines.add(Text.translatable("nozh.hud.last_action", resolveLastAction(viewModel)));
            lines.add(Text.translatable("nozh.hud.last_outcome", resolveLastOutcome(viewModel)));
        }
        appendSuggestedActions(lines, state);
        if (hudMode == HudMode.EXPERT) {
            appendDirectorTraces(lines, viewModel);
            lines.add(Text.translatable(
                    "nozh.hud.hud_time",
                    formatMsAllowZero(lastHudRenderMs),
                    resolveHudBudgetStatus(lastHudRenderMs)));
            lines.add(Text.translatable("nozh.hud.export_hint", resolveExportKeyLabel()));
        }
        return lines;
    }

    private void appendSuggestedActions(List<Text> lines, RuntimeState state) {
        NozhConfig config = ConfigManager.getConfig();
        if (config != null && !config.showHudSuggestions) {
            return;
        }
        if (state == null || state.suggestedActions() == null || state.suggestedActions().isEmpty()) {
            lines.add(Text.translatable("nozh.hud.actions.none"));
            return;
        }
        int limit = Math.min(state.suggestedActions().size(), 2);
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            var pending = state.suggestedActions().get(i);
            if (i > 0) {
                summary.append(" | ");
            }
            summary.append(formatAction(pending.capability().name(), pending.newValue().toString()));
        }
        int remaining = state.suggestedActions().size() - limit;
        if (remaining > 0) {
            summary.append(" +").append(remaining);
        }
        lines.add(Text.translatable("nozh.hud.actions.suggested", summary.toString()));
    }

    private String resolveExportKeyLabel() {
        if (exportKeySupplier == null) {
            return Text.translatable("nozh.hud.export.unbound").getString();
        }
        String label = exportKeySupplier.get();
        if (label == null || label.isBlank()) {
            return Text.translatable("nozh.hud.export.unbound").getString();
        }
        return label;
    }

    private String resolveHudBudgetStatus(double renderMs) {
        if (renderMs <= 0) {
            return Text.translatable("nozh.hud.hud_time.unknown").getString();
        }
        return renderMs <= 0.5
                ? Text.translatable("nozh.hud.hud_time.ok").getString()
                : Text.translatable("nozh.hud.hud_time.slow").getString();
    }

    private void appendDirectorTraces(List<Text> lines, HudViewModel viewModel) {
        if (viewModel == null || viewModel.stewardshipTraces() == null || viewModel.stewardshipTraces().isEmpty()) {
            return;
        }
        lines.add(Text.translatable("nozh.hud.director.title"));
        int limit = Math.min(viewModel.stewardshipTraces().size(), 5);
        for (int i = 0; i < limit; i++) {
            HudViewModel.DirectorTrace trace = viewModel.stewardshipTraces().get(i);
            Text mode = Text.translatable(trace.modeKey());
            if (trace.reason() != null && !trace.reason().isBlank()) {
                lines.add(Text.translatable("nozh.hud.director.trace_reason",
                        trace.capabilityId(), trace.steward(), mode, trace.reason()));
            } else {
                lines.add(Text.translatable("nozh.hud.director.trace",
                        trace.capabilityId(), trace.steward(), mode));
            }
        }
        int remaining = viewModel.stewardshipTraces().size() - limit;
        if (remaining > 0) {
            lines.add(Text.translatable("nozh.hud.director.more", remaining));
        }
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

    private String formatMsAllowZero(double value) {
        if (Double.isNaN(value) || value < 0) {
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

    private String formatPercent(double value) {
        if (!Double.isFinite(value) || value < 0) {
            return "--";
        }
        return String.format("%.0f%%", value * 100.0);
    }

    private String formatStutterDetail(String detail, double confidence) {
        String base = formatPercent(confidence);
        if (detail == null || detail.isBlank()) {
            return base;
        }
        return detail + " • " + base;
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
