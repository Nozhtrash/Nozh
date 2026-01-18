package dev.nozh.client.gui;

import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.state.PendingAction;
import dev.nozh.core.state.RuntimeState;
import dev.nozh.core.state.StateStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Quick Actions Menu - triggered by K key.
 * Provides fast access to all NOZH actions in one place.
 */
public class NozhQuickMenu extends Screen {

    private final Screen parent;
    private final StateStore stateStore;
    private final Runnable onApplySuggestion;
    private final Runnable onDismissSuggestion;
    private final Runnable onExportReport;

    private static final int MENU_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    public NozhQuickMenu(
            Screen parent,
            StateStore stateStore,
            Runnable onApplySuggestion,
            Runnable onDismissSuggestion,
            Runnable onExportReport) {
        super(Text.translatable("nozh.quickmenu.title"));
        this.parent = parent;
        this.stateStore = stateStore;
        this.onApplySuggestion = onApplySuggestion;
        this.onDismissSuggestion = onDismissSuggestion;
        this.onExportReport = onExportReport;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 80;
        int buttonX = centerX - MENU_WIDTH / 2;

        NozhConfig config = ConfigManager.getConfig();

        // Toggle HUD
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("nozh.quickmenu.toggle_hud",
                        config.showHud ? Text.translatable("options.on") : Text.translatable("options.off")),
                btn -> {
                    config.showHud = !config.showHud;
                    ConfigManager.saveAndNotify();
                    btn.setMessage(Text.translatable("nozh.quickmenu.toggle_hud",
                            config.showHud ? Text.translatable("options.on") : Text.translatable("options.off")));
                })
                .dimensions(buttonX, startY, MENU_WIDTH, BUTTON_HEIGHT)
                .build());
        startY += BUTTON_SPACING;

        // Apply Suggestion
        RuntimeState state = stateStore != null ? stateStore.snapshotSafe() : null;
        List<PendingAction> suggestions = state != null ? state.suggestedActions() : null;
        boolean hasSuggestions = suggestions != null && !suggestions.isEmpty();

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("nozh.quickmenu.apply_suggestion"),
                btn -> {
                    if (onApplySuggestion != null) {
                        onApplySuggestion.run();
                    }
                    close();
                })
                .dimensions(buttonX, startY, MENU_WIDTH, BUTTON_HEIGHT)
                .build()).active = hasSuggestions;
        startY += BUTTON_SPACING;

        // Dismiss Suggestion
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("nozh.quickmenu.dismiss_suggestion"),
                btn -> {
                    if (onDismissSuggestion != null) {
                        onDismissSuggestion.run();
                    }
                    close();
                })
                .dimensions(buttonX, startY, MENU_WIDTH, BUTTON_HEIGHT)
                .build()).active = hasSuggestions;
        startY += BUTTON_SPACING;

        // Export Telemetry
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("nozh.quickmenu.export"),
                btn -> {
                    if (onExportReport != null) {
                        onExportReport.run();
                    }
                    close();
                })
                .dimensions(buttonX, startY, MENU_WIDTH, BUTTON_HEIGHT)
                .build());
        startY += BUTTON_SPACING;

        // Open Full Config
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("nozh.quickmenu.open_config"),
                btn -> {
                    this.client.setScreen(new NozhConfigScreen(parent));
                })
                .dimensions(buttonX, startY, MENU_WIDTH, BUTTON_HEIGHT)
                .build());
        startY += BUTTON_SPACING;

        // Close
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("nozh.quickmenu.close"),
                btn -> close())
                .dimensions(buttonX, startY, MENU_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent background
        this.renderBackground(context);

        int centerX = this.width / 2;
        int panelWidth = MENU_WIDTH + 40;
        int panelHeight = 180;
        int panelX = centerX - panelWidth / 2;
        int panelY = this.height / 2 - 110;

        // Draw panel background
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC1A1A1A);
        // Panel border
        context.drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY, 0xFF4CAF50);
        context.drawHorizontalLine(panelX, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF4CAF50);
        context.drawVerticalLine(panelX, panelY, panelY + panelHeight - 1, 0xFF4CAF50);
        context.drawVerticalLine(panelX + panelWidth - 1, panelY, panelY + panelHeight - 1, 0xFF4CAF50);

        // Draw title with color
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                centerX, panelY + 8, 0x4CAF50);

        // Draw mod status
        NozhConfig config = ConfigManager.getConfig();
        String statusText = config.enabled ? "[ACTIVE]" : "[DISABLED]";
        int statusColor = config.enabled ? 0x00FF00 : 0xFF5555;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(statusText),
                centerX, panelY + 20, statusColor);

        // Draw suggestion info if available
        if (stateStore != null) {
            RuntimeState state = stateStore.snapshotSafe();
            if (state != null && state.suggestedActions() != null && !state.suggestedActions().isEmpty()) {
                PendingAction next = state.suggestedActions().get(0);
                String suggestionText = next.capability().name() + " -> " + next.newValue();
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.translatable("nozh.quickmenu.pending", suggestionText),
                        centerX, this.height / 2 - 88, 0xFFFF00);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause game
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close on K or Escape
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_K || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
