package dev.nozh.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.nozh.NozhConstants;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.safety.CrashLoopGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * ModMenu integration for NOZH with professional UI and tooltips.
 */
@Environment(EnvType.CLIENT)
public class NozhModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NozhConfigScreen::new;
    }

    /**
     * Professional config screen with tooltips and bilingual support.
     */
    private static class NozhConfigScreen extends Screen {
        private final Screen parent;
        private final List<TooltipButton> tooltipButtons = new ArrayList<>();

        protected NozhConfigScreen(Screen parent) {
            super(Text.translatable("nozh.config.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();
            tooltipButtons.clear();

            NozhConfig config = ConfigManager.getConfig();
            int centerX = this.width / 2;
            int y = 60;

            // Header info
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("nozh.config.version", NozhConstants.getVersion()),
                    button -> {
                    }).dimensions(centerX - 150, 20, 300, 20).build());

            // Safe Mode Warning (if active)
            if (CrashLoopGuard.isInSafeMode()) {
                y += 10;
                addDrawableChild(ButtonWidget.builder(
                        Text.translatable("nozh.config.safemode"),
                        button -> {
                        }).dimensions(centerX - 150, y, 300, 20).build());
                y += 25;

                String reason = CrashLoopGuard.getSafeModeReason();
                addDrawableChild(ButtonWidget.builder(
                        Text.translatable("nozh.config.safemode.reason", reason),
                        button -> {
                        }).dimensions(centerX - 150, y, 300, 20).build());
                y += 25;

                int attempts = CrashLoopGuard.getBootAttempts();
                addDrawableChild(ButtonWidget.builder(
                        Text.translatable("nozh.config.boot_attempts", attempts),
                        button -> {
                        }).dimensions(centerX - 150, y, 300, 20).build());
                y += 35;

                // Reset Safe Mode button
                TooltipButton resetSafeMode = new TooltipButton(
                        centerX - 100, y, 200, 20,
                        Text.translatable("nozh.config.reset_safemode"),
                        Text.translatable("nozh.config.reset_safemode.tooltip"),
                        button -> {
                            CrashLoopGuard.resetSafeMode();
                            this.clearAndInit();
                        });
                addDrawableChild(resetSafeMode);
                tooltipButtons.add(resetSafeMode);
                y += 30;
            }

            y += 10;

            // Toggle: Enabled
            TooltipButton enabledButton = new TooltipButton(
                    centerX - 150, y, 300, 20,
                    Text.translatable("nozh.config.enabled", config.enabled ? "ON" : "OFF"),
                    Text.translatable("nozh.config.enabled.tooltip"),
                    button -> {
                        config.enabled = !config.enabled;
                        ConfigManager.saveAndNotify();
                        this.clearAndInit();
                    });
            addDrawableChild(enabledButton);
            tooltipButtons.add(enabledButton);
            y += 25;

            // Toggle: Debug Logs
            TooltipButton debugButton = new TooltipButton(
                    centerX - 150, y, 300, 20,
                    Text.translatable("nozh.config.debug", config.debugLogs ? "ON" : "OFF"),
                    Text.translatable("nozh.config.debug.tooltip"),
                    button -> {
                        config.debugLogs = !config.debugLogs;
                        ConfigManager.saveAndNotify();
                        this.clearAndInit();
                    });
            addDrawableChild(debugButton);
            tooltipButtons.add(debugButton);
            y += 25;

            // Toggle: HUD
            TooltipButton hudButton = new TooltipButton(
                    centerX - 150, y, 300, 20,
                    Text.translatable("nozh.config.hud", config.showHud ? "ON" : "OFF"),
                    Text.translatable("nozh.config.hud.tooltip"),
                    button -> {
                        config.showHud = !config.showHud;
                        ConfigManager.saveAndNotify();
                        this.clearAndInit();
                    });
            addDrawableChild(hudButton);
            tooltipButtons.add(hudButton);
            y += 25;

            // Toggle: Rollback
            TooltipButton rollbackButton = new TooltipButton(
                    centerX - 150, y, 300, 20,
                    Text.translatable("nozh.config.rollback", config.rollbackEnabled ? "ON" : "OFF"),
                    Text.translatable("nozh.config.rollback.tooltip"),
                    button -> {
                        config.rollbackEnabled = !config.rollbackEnabled;
                        ConfigManager.saveAndNotify();
                        this.clearAndInit();
                    });
            addDrawableChild(rollbackButton);
            tooltipButtons.add(rollbackButton);
            y += 25;

            // Toggle: Auto-Tuning
            TooltipButton autoTuningButton = new TooltipButton(
                    centerX - 150, y, 300, 20,
                    Text.translatable("nozh.config.autotuning", config.allowAutoTuning ? "ON" : "OFF"),
                    Text.translatable("nozh.config.autotuning.tooltip"),
                    button -> {
                        config.allowAutoTuning = !config.allowAutoTuning;
                        ConfigManager.saveAndNotify();
                        this.clearAndInit();
                    });
            addDrawableChild(autoTuningButton);
            tooltipButtons.add(autoTuningButton);
            y += 25;

            // Target FPS (cycle 30/60/90/120)
            TooltipButton targetFpsButton = new TooltipButton(
                    centerX - 150, y, 300, 20,
                    Text.translatable("nozh.config.targetfps", config.targetFps),
                    Text.translatable("nozh.config.targetfps.tooltip"),
                    button -> {
                        int[] fpsOptions = { 30, 60, 90, 120 };
                        int currentIndex = 0;
                        for (int i = 0; i < fpsOptions.length; i++) {
                            if (fpsOptions[i] == config.targetFps) {
                                currentIndex = i;
                                break;
                            }
                        }
                        config.targetFps = fpsOptions[(currentIndex + 1) % fpsOptions.length];
                        ConfigManager.saveAndNotify();
                        this.clearAndInit();
                    });
            addDrawableChild(targetFpsButton);
            tooltipButtons.add(targetFpsButton);
            y += 35;

            // Preset Buttons
            int presetWidth = 95;
            int gap = 5;
            int startX = centerX - (presetWidth * 3 + gap * 2) / 2;

            TooltipButton lowButton = new TooltipButton(
                    startX, y, presetWidth, 20,
                    Text.translatable("nozh.config.preset.low"),
                    Text.translatable("nozh.config.preset.low.tooltip"),
                    button -> {
                        ConfigPresets.applyLowEnd();
                        this.clearAndInit();
                    });
            addDrawableChild(lowButton);
            tooltipButtons.add(lowButton);

            TooltipButton midButton = new TooltipButton(
                    startX + presetWidth + gap, y, presetWidth, 20,
                    Text.translatable("nozh.config.preset.mid"),
                    Text.translatable("nozh.config.preset.mid.tooltip"),
                    button -> {
                        ConfigPresets.applyMidRange();
                        this.clearAndInit();
                    });
            addDrawableChild(midButton);
            tooltipButtons.add(midButton);

            TooltipButton highButton = new TooltipButton(
                    startX + (presetWidth + gap) * 2, y, presetWidth, 20,
                    Text.translatable("nozh.config.preset.high"),
                    Text.translatable("nozh.config.preset.high.tooltip"),
                    button -> {
                        ConfigPresets.applyHighEnd();
                        this.clearAndInit();
                    });
            addDrawableChild(highButton);
            tooltipButtons.add(highButton);
            y += 35;

            // Reset Config Button
            TooltipButton resetButton = new TooltipButton(
                    centerX - 100, y, 200, 20,
                    Text.translatable("nozh.config.reset"),
                    Text.translatable("nozh.config.reset.tooltip"),
                    button -> {
                        this.client.setScreen(new ConfirmResetScreen(this));
                    });
            addDrawableChild(resetButton);
            tooltipButtons.add(resetButton);
            y += 35;

            // Done Button
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("nozh.config.done"),
                    button -> this.client.setScreen(parent))
                    .dimensions(centerX - 100, this.height - 30, 200, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);

            // Render tooltips for hovered buttons
            for (TooltipButton button : tooltipButtons) {
                button.renderTooltip(context, mouseX, mouseY, textRenderer);
            }
        }

        @Override
        public void close() {
            this.client.setScreen(parent);
        }
    }

    /**
     * Custom button widget with tooltip support.
     */
    private static class TooltipButton extends ButtonWidget {
        private final Text tooltip;

        public TooltipButton(int x, int y, int width, int height, Text message, Text tooltip, PressAction onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.tooltip = tooltip;
        }

        public void renderTooltip(DrawContext context, int mouseX, int mouseY,
                net.minecraft.client.font.TextRenderer textRenderer) {
            if (this.isHovered()) {
                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    /**
     * Confirmation screen for config reset.
     */
    private static class ConfirmResetScreen extends Screen {
        private final Screen parent;

        protected ConfirmResetScreen(Screen parent) {
            super(Text.translatable("nozh.config.confirm.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            super.init();

            int centerX = this.width / 2;
            int centerY = this.height / 2;

            // Confirm button
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("nozh.config.confirm.yes"),
                    button -> {
                        ConfigManager.resetToDefaults();
                        this.client.setScreen(parent);
                    }).dimensions(centerX - 105, centerY + 20, 100, 20).build());

            // Cancel button
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("nozh.config.confirm.no"),
                    button -> this.client.setScreen(parent))
                    .dimensions(centerX + 5, centerY + 20, 100, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);

            // Draw warning message
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("nozh.config.confirm.message"),
                    this.width / 2,
                    this.height / 2 - 10,
                    0xFFFFFF);
        }

        @Override
        public void close() {
            this.client.setScreen(parent);
        }
    }
}
