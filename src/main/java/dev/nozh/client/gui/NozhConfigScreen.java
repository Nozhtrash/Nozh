package dev.nozh.client.gui;

import dev.nozh.NozhConstants;
import dev.nozh.NozhMod;
import dev.nozh.client.ConfigPresets;
import dev.nozh.client.gui.widget.VitalsGraphWidget;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.config.OptimizationProfile;
import dev.nozh.core.governor.IntegratedGovernor;
import dev.nozh.core.state.StateStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium Configuration Dashboard.
 * 
 * Replaces the old list-based config screen with a modern dashboard.
 * - Tabbed navigation
 * - Real-time Vitals Graph
 * - Quick Profiles
 */
public class NozhConfigScreen extends Screen {
    private final Screen parent;
    private NozhConfig config;
    private VitalsGraphWidget graphWidget;

    // Layout constants
    private static final int SIDEBAR_WIDTH = 120;
    private static final int HEADER_HEIGHT = 40;

    private Tab currentTab = Tab.DASHBOARD;

    enum Tab {
        DASHBOARD("Dashboard"),
        GENERAL("General"),
        VISUALS("Visuals"),
        CLOUD("Cloud"),
        ADVANCED("Advanced");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    public NozhConfigScreen(Screen parent) {
        super(Text.translatable("nozh.gui.title"));
        this.parent = parent;
        this.config = ConfigManager.getConfig();
    }

    @Override
    protected void init() {
        // Initialize widgets
        int graphHeight = 60;
        IntegratedGovernor governor = NozhMod.getGovernor();
        if (governor != null) {
            this.graphWidget = new VitalsGraphWidget(
                    SIDEBAR_WIDTH + 10,
                    HEADER_HEIGHT + 10,
                    this.width - SIDEBAR_WIDTH - 20,
                    graphHeight,
                    governor.getVitalsRecorder());
        } else {
            this.graphWidget = null;
        }

        // Navigation Buttons (Sidebar)
        int y = HEADER_HEIGHT + 20;
        for (Tab tab : Tab.values()) {
            addDrawableChild(ButtonWidget.builder(Text.literal(tab.label), button -> {
                this.currentTab = tab;
                this.clearAndInit(); // Refresh layout
            })
                    .dimensions(10, y, SIDEBAR_WIDTH - 20, 20)
                    .build());
            y += 25;
        }

        // Content Area
        int contentX = SIDEBAR_WIDTH + 10;
        int contentY = HEADER_HEIGHT + 10 + graphHeight + 20;

        if (currentTab == Tab.DASHBOARD) {
            initDashboard(contentX, contentY);
        } else if (currentTab == Tab.GENERAL) {
            initGeneral(contentX, contentY);
        } else if (currentTab == Tab.VISUALS) {
            initVisuals(contentX, contentY);
        } else if (currentTab == Tab.ADVANCED) {
            initAdvanced(contentX, contentY);
        }

        // Close Button
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            ConfigManager.saveAndNotify();
            this.client.setScreen(parent);
        })
                .dimensions(this.width - 110, this.height - 30, 100, 20)
                .build());
    }

    private void initDashboard(int x, int y) {
        // Quick Profiles
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply: Potato Mode"), btn -> {
            ConfigPresets.applyLowEnd();
            ConfigManager.saveAndNotify();
        }).dimensions(x, y, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply: Balanced"), btn -> {
            ConfigPresets.applyMidRange();
            ConfigManager.saveAndNotify();
        }).dimensions(x + 160, y, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply: High Fidelity"), btn -> {
            ConfigPresets.applyHighEnd();
            ConfigManager.saveAndNotify();
        }).dimensions(x + 320, y, 150, 20).build());

        // EXTREME Mode
        y += 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("⚠️ EXTREME POTATO MODE ⚠️"), btn -> {
            // Manually set config to EXTREME profile
            ConfigPresets.applyExtreme();
            ConfigManager.saveAndNotify();
        }).dimensions(x, y, 310, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // Render Header
        context.fill(0, 0, this.width, HEADER_HEIGHT, 0xFF111111);
        context.drawCenteredTextWithShadow(this.textRenderer, "NOZH OPTIMIZER DASHBOARD", this.width / 2, 16, 0xFFFFFF);

        // Render Sidebar Background
        context.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, this.height, 0x80000000);

        // Render Graph
        if (graphWidget != null) {
            graphWidget.render(context, mouseX, mouseY, delta);
            // Graph Tooltip
            if (mouseX >= graphWidget.getX() && mouseX <= graphWidget.getX() + graphWidget.getWidth()
                    && mouseY >= graphWidget.getY() && mouseY <= graphWidget.getY() + graphWidget.getHeight()) {
                context.drawTooltip(textRenderer, Text.literal("Frame Time History (Green=60+ FPS, Red=<30 FPS)"),
                        mouseX, mouseY);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        // Render Tooltips for Buttons last
        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof TooltipRenderable tr
                    && element instanceof net.minecraft.client.gui.widget.ClickableWidget cw) {
                if (cw.isHovered()) {
                    tr.renderTooltip(context, mouseX, mouseY, textRenderer);
                }
            }
        }
    }

    private void initGeneral(int x, int y) {
        addDrawableChild(new SectionLabel(x, y, "Core Features"));
        y += 20;

        addDrawableChild(new TooltipButton(x, y, 200, 20,
                Text.translatable("nozh.option.enabled", config.enabled ? "ON" : "OFF"),
                Text.literal(
                        "Master Switch.\nDisables ALL optimizations instantly.\nUse this if you suspect Nozh is breaking a specific mechanism."),
                btn -> {
                    config.enabled = !config.enabled;
                    btn.setMessage(Text.translatable("nozh.option.enabled", config.enabled ? "ON" : "OFF"));
                }));
        y += 25;

        addDrawableChild(new TooltipButton(x, y, 200, 20,
                Text.translatable("nozh.option.autotuning", config.allowAutoTuning ? "ON" : "OFF"),
                Text.literal(
                        "Auto-Tuning.\nAllows Nozh to change settings automatically during gameplay\nwithout asking for confirmation first."),
                btn -> {
                    config.allowAutoTuning = !config.allowAutoTuning;
                    btn.setMessage(Text.translatable("nozh.option.autotuning", config.allowAutoTuning ? "ON" : "OFF"));
                }));
        y += 25;

        addDrawableChild(new TooltipButton(x, y, 200, 20,
                Text.literal("Rollback System: " + (config.rollbackEnabled ? "ON" : "OFF")),
                Text.literal(
                        "Safety Feature.\nIf an optimization causes FPS to DROP instead of rise,\nNozh will automatically undo it within 45 seconds."),
                btn -> {
                    config.rollbackEnabled = !config.rollbackEnabled;
                    btn.setMessage(Text.literal("Rollback System: " + (config.rollbackEnabled ? "ON" : "OFF")));
                }));
        y += 25;

        addDrawableChild(new SectionLabel(x, y, "Performance Targets"));
        y += 20;

        addSlider(x, y, 200, 20, "Target FPS", config.targetFps, 30, 240,
                val -> config.targetFps = val.intValue(),
                val -> val.intValue() + " FPS",
                "The FPS goal Nozh tries to reach.\nHigher targets = More aggressive degradation of visuals.");
    }

    private void initVisuals(int x, int y) {
        addDrawableChild(new SectionLabel(x, y, "Adaptive Visual Quality"));
        y += 20;

        addDrawableChild(new TooltipButton(x, y, 200, 20,
                Text.literal("Adaptive Quality: " + (config.adaptiveVisualQualityEnabled ? "ON" : "OFF")),
                Text.literal(
                        "Dynamic Resolution scaling for visual effects.\nReduces particles and draw distance when FPS drops.\nRestores them when FPS is high."),
                btn -> {
                    config.adaptiveVisualQualityEnabled = !config.adaptiveVisualQualityEnabled;
                    btn.setMessage(
                            Text.literal("Adaptive Quality: " + (config.adaptiveVisualQualityEnabled ? "ON" : "OFF")));
                }));
        y += 25;

        addSlider(x, y, 200, 20, "Sensitivity", config.adaptiveVisualQualitySensitivityMs, 0.5, 5.0,
                val -> config.adaptiveVisualQualitySensitivityMs = val,
                val -> String.format("%.1f ms", val),
                "Lag Tolerance.\nLower = Reacts faster to small lags.\nHigher = Smoother but reacts slowly.");
        y += 25;

        addDrawableChild(new SectionLabel(x, y, "HUD"));
        y += 20;

        addDrawableChild(new TooltipButton(x, y, 200, 20,
                Text.literal("Show HUD: " + (config.showHud ? "ON" : "OFF")),
                Text.literal("Toggles the in-game information display."),
                btn -> {
                    config.showHud = !config.showHud;
                    btn.setMessage(Text.literal("Show HUD: " + (config.showHud ? "ON" : "OFF")));
                }));
    }

    private void initAdvanced(int x, int y) {
        addDrawableChild(new TooltipButton(x, y, 200, 20,
                Text.literal("Debug Logs: " + (config.debugLogs ? "ON" : "OFF")),
                Text.literal(
                        "Spammy Logs.\nWrites detailed decision data to the log file.\nEnable ONLY if reporting a bug."),
                btn -> {
                    config.debugLogs = !config.debugLogs;
                    btn.setMessage(Text.literal("Debug Logs: " + (config.debugLogs ? "ON" : "OFF")));
                }));
        y += 25;

        addDrawableChild(new TooltipButton(x, y, 200, 20,
                Text.literal("Safe Mode Force: " + (config.safeModeForce ? "ON" : "OFF")),
                Text.literal(
                        "Emergency Switch.\nForces minimal settings regardless of profile.\nUse if crashing repeatedly."),
                btn -> {
                    config.safeModeForce = !config.safeModeForce;
                    btn.setMessage(Text.literal("Safe Mode Force: " + (config.safeModeForce ? "ON" : "OFF")));
                }));
    }

    // ============================================================================================
    // Helper Methods & Classes
    // ============================================================================================

    // Since we are overriding standard methods, we must ensure these helpers exist

    private void addSlider(int x, int y, int w, int h, String name, double current, double min, double max,
            java.util.function.Consumer<Double> onSet,
            java.util.function.Function<Double, String> display, String tooltip) {
        TooltipSlider slider = new TooltipSlider(x, y, w, h, Text.literal(name + ": "), Text.literal(tooltip), min, max,
                current, onSet, display);
        addDrawableChild(slider);
    }

    interface TooltipRenderable {
        void renderTooltip(DrawContext context, int mouseX, int mouseY,
                net.minecraft.client.font.TextRenderer textRenderer);
    }

    static class TooltipButton extends ButtonWidget implements TooltipRenderable {
        private final Text tooltip;

        public TooltipButton(int x, int y, int width, int height, Text message, Text tooltip, PressAction onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.tooltip = tooltip;
        }

        @Override
        public void renderTooltip(DrawContext context, int mouseX, int mouseY,
                net.minecraft.client.font.TextRenderer textRenderer) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    static class TooltipSlider extends net.minecraft.client.gui.widget.SliderWidget implements TooltipRenderable {
        private final Text prefix;
        private final Text tooltip;
        private final double min, max;
        private final java.util.function.Consumer<Double> onSet;
        private final java.util.function.Function<Double, String> display;

        public TooltipSlider(int x, int y, int width, int height, Text prefix, Text tooltip,
                double min, double max, double current,
                java.util.function.Consumer<Double> onSet,
                java.util.function.Function<Double, String> display) {
            super(x, y, width, height, Text.empty(), (Math.max(min, Math.min(max, current)) - min) / (max - min));
            this.prefix = prefix;
            this.tooltip = tooltip;
            this.min = min;
            this.max = max;
            this.onSet = onSet;
            this.display = display;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double val = min + value * (max - min);
            setMessage(prefix.copy().append(display.apply(val)));
        }

        @Override
        protected void applyValue() {
            double val = min + value * (max - min);
            onSet.accept(val);
        }

        @Override
        public void renderTooltip(DrawContext context, int mouseX, int mouseY,
                net.minecraft.client.font.TextRenderer textRenderer) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    static class SectionLabel extends net.minecraft.client.gui.widget.ClickableWidget {
        public SectionLabel(int x, int y, String text) {
            super(x, y, 200, 15, Text.literal(text));
            this.active = false; // Not clickable
        }

        @Override
        protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            context.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                    this.getMessage(), this.getX(), this.getY(), 0x55FF55, false);
        }

        @Override
        protected void appendClickableNarrations(
                net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        }
    }
}
