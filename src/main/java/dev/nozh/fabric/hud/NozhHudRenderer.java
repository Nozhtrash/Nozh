package dev.nozh.fabric.hud;

import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.context.Scenario;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * NOZH HUD Renderer - Real-time informative overlay.
 * 
 * Shows:
 * - Current mode (AUTO/MANUAL)
 * - Current scenario
 * - FPS and P95 metrics
 * - Last action (temporary, 5s)
 * - Manual mode suggestions
 * 
 * Bilingual support (English/Spanish) via translation keys.
 */
public final class NozhHudRenderer {

    private static final int HUD_X = 10;
    private static final int HUD_Y = 10;
    private static final int LINE_HEIGHT = 12;
    private static final long ACTION_DISPLAY_DURATION_MS = 5000;

    private final MinecraftClient client;
    private final boolean enabled;
    private final boolean compact;

    private long lastActionTime = 0;
    private String lastActionText = "";
    private double lastActionImprovement = 0.0;

    public NozhHudRenderer(MinecraftClient client, boolean enabled, boolean compact) {
        this.client = client;
        this.enabled = enabled;
        this.compact = compact;
    }

    public void render(DrawContext context, RuntimeState state) {
        if (!enabled || state == null || client.options.debugEnabled) {
            return; // Don't render over F3 debug screen
        }

        TextRenderer font = client.textRenderer;
        List<String> lines = buildHudLines(state);

        int y = HUD_Y;
        for (String line : lines) {
            if (line.isEmpty()) {
                y += LINE_HEIGHT / 2; // Half space for empty lines
                continue;
            }

            Text text = Text.literal(line);
            context.drawTextWithShadow(font, text, HUD_X, y, getColorForLine(line));
            y += LINE_HEIGHT;
        }
    }

    private List<String> buildHudLines(RuntimeState state) {
        List<String> lines = new ArrayList<>();

        // Line 1: Mode
        String modeText = translate(
            state.autoTuning() ? "nozh.hud.mode_auto" : "nozh.hud.mode_manual",
            state.autoTuning() ? "NOZH: AUTO" : "NOZH: MANUAL"
        );
        lines.add(modeText);

        if (compact) {
            // Compact mode: only show mode and FPS
            lines.add(buildMetricsLine(state));
            return lines;
        }

        // Line 2: Scenario
        Scenario scenario = state.currentScenario();
        if (scenario != null) {
            String scenarioText = translate(
                "nozh.hud.scenario." + scenario.name().toLowerCase(),
                "Scenario: " + scenario.name()
            );
            lines.add(scenarioText);
        }

        // Line 3: Metrics
        lines.add(buildMetricsLine(state));

        // Line 4: Last action (if recent)
        long now = System.currentTimeMillis();
        if (now - lastActionTime < ACTION_DISPLAY_DURATION_MS && !lastActionText.isEmpty()) {
            String improvement = lastActionImprovement > 0 
                ? String.format("+%.1f%%", lastActionImprovement)
                : "applied";
            String actionLine = "✓ " + lastActionText + " (" + improvement + ")";
            lines.add(actionLine);
        }

        // Line 5: Manual mode suggestion
        if (!state.autoTuning() && state.suggestedActions() != null && !state.suggestedActions().isEmpty()) {
            PendingAction suggestion = state.suggestedActions().get(0);
            String suggestionText = translate(
                "nozh.hud.suggestion",
                "⚡ Suggestion: " + suggestion.capability().name() + " (Press K)"
            );
            lines.add(""); // Empty line for spacing
            lines.add(suggestionText);
        }

        return lines;
    }

    private String buildMetricsLine(RuntimeState state) {
        int fps = calculateFps(state.avgFrametimeMs());
        double p95 = state.p95FrametimeMs();

        if (fps <= 0 || p95 <= 0) {
            return "FPS: -- | P95: --";
        }

        return String.format("FPS: %d | P95: %.1fms", fps, p95);
    }

    private int calculateFps(double avgFrametimeMs) {
        if (avgFrametimeMs <= 0) {
            return 0;
        }
        return (int) Math.round(1000.0 / avgFrametimeMs);
    }

    private int getColorForLine(String line) {
        if (line.contains("AUTO")) {
            return 0x00FF00; // Green
        }
        if (line.contains("MANUAL")) {
            return 0xFFFF00; // Yellow
        }
        if (line.contains("Scenario:") || line.contains("Escenario:")) {
            return 0xFFAA00; // Orange
        }
        if (line.startsWith("✓")) {
            return 0x00FFFF; // Cyan
        }
        if (line.startsWith("⚡")) {
            return 0xFF8800; // Orange
        }
        return 0xFFFFFF; // White (default)
    }

    private String translate(String key, String fallback) {
        try {
            Text translated = Text.translatable(key);
            String result = translated.getString();
            return result.equals(key) ? fallback : result;
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Called when an action is successfully applied.
     * Updates the HUD to show the action temporarily.
     */
    public void notifyActionApplied(String actionText, double improvementPercent) {
        this.lastActionTime = System.currentTimeMillis();
        this.lastActionText = actionText;
        this.lastActionImprovement = improvementPercent;
    }

    /**
     * Clear the last action display.
     */
    public void clearLastAction() {
        this.lastActionTime = 0;
        this.lastActionText = "";
        this.lastActionImprovement = 0.0;
    }
}
