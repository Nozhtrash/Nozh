package dev.nozh.client.gui;

import dev.nozh.NozhMod;
import dev.nozh.client.ConfigPresets;
import dev.nozh.client.gui.widget.VitalsGraphWidget;
import dev.nozh.core.config.ConfigManager;
import dev.nozh.core.config.NozhConfig;
import dev.nozh.core.governor.IntegratedGovernor;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
                DASHBOARD("nozh.config.tab.dashboard"),
                GENERAL("nozh.config.tab.general"),
                VISUALS("nozh.config.tab.visuals"),
                SYSTEM("nozh.config.tab.system"),
                ADVANCED("nozh.config.tab.advanced");

                final String key;

                Tab(String key) {
                        this.key = key;
                }
        }

        public NozhConfigScreen(Screen parent) {
                super(Text.translatable("nozh.config.title"));
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
                        addDrawableChild(ButtonWidget.builder(Text.translatable(tab.key), button -> {
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
                } else if (currentTab == Tab.SYSTEM) {
                        initSystem(contentX, contentY);
                }

                // Save Button (just save, don't close)
                addDrawableChild(ButtonWidget.builder(Text.translatable("nozh.config.button.save"), button -> {
                        ConfigManager.saveAndNotify();
                        button.setMessage(Text.translatable("nozh.config.button.saved"));
                })
                                .dimensions(this.width - 220, this.height - 30, 100, 20)
                                .build());

                // Close Button
                addDrawableChild(ButtonWidget.builder(Text.translatable("nozh.config.button.save_close"), button -> {
                        ConfigManager.saveAndNotify();
                        this.client.setScreen(parent);
                })
                                .dimensions(this.width - 110, this.height - 30, 100, 20)
                                .build());
        }

        private void initDashboard(int x, int y) {
                // Quick Profiles
                addDrawableChild(ButtonWidget.builder(Text.translatable("nozh.config.profile.low"), btn -> {
                        ConfigPresets.applyLowEnd();
                        this.config = ConfigManager.getConfig();
                        this.clearAndInit();
                }).dimensions(x, y, 150, 20).build());

                addDrawableChild(ButtonWidget.builder(Text.translatable("nozh.config.profile.mid"), btn -> {
                        ConfigPresets.applyMidRange();
                        this.config = ConfigManager.getConfig();
                        this.clearAndInit();
                }).dimensions(x + 160, y, 150, 20).build());

                addDrawableChild(ButtonWidget.builder(Text.translatable("nozh.config.profile.high"), btn -> {
                        ConfigPresets.applyHighEnd();
                        this.config = ConfigManager.getConfig();
                        this.clearAndInit();
                }).dimensions(x + 320, y, 150, 20).build());

                // EXTREME Mode
                y += 30;
                addDrawableChild(ButtonWidget.builder(Text.translatable("nozh.config.profile.extreme"), btn -> {
                        // Manually set config to EXTREME profile
                        ConfigPresets.applyExtreme();
                        this.config = ConfigManager.getConfig();
                        this.clearAndInit();
                }).dimensions(x, y, 310, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                this.renderBackground(context);

                // Render Header
                context.fill(0, 0, this.width, HEADER_HEIGHT, 0xFF111111);
                context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("nozh.config.dashboard_title"),
                                this.width / 2, 16, 0xFFFFFF);

                // Render Sidebar Background
                context.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, this.height, 0x80000000);

                // Render Graph
                if (graphWidget != null) {
                        graphWidget.render(context, mouseX, mouseY, delta);
                        // Graph Tooltip
                        if (mouseX >= graphWidget.getX() && mouseX <= graphWidget.getX() + graphWidget.getWidth()
                                        && mouseY >= graphWidget.getY()
                                        && mouseY <= graphWidget.getY() + graphWidget.getHeight()) {
                                context.drawTooltip(textRenderer, Text.translatable("nozh.config.tooltip.graph"),
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
                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.core"));
                y += 20;

                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.option.enabled",
                                                config.enabled ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.enabled"),
                                btn -> {
                                        config.enabled = !config.enabled;
                                        btn.setMessage(Text.translatable("nozh.option.enabled",
                                                        config.enabled ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                }));
                y += 25;

                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.option.autotuning",
                                                config.allowAutoTuning ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.autotuning"),
                                btn -> {
                                        config.allowAutoTuning = !config.allowAutoTuning;
                                        btn.setMessage(Text.translatable("nozh.option.autotuning",
                                                        config.allowAutoTuning ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                }));
                y += 25;

                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.option.rollback",
                                                config.rollbackEnabled ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.rollback"),
                                btn -> {
                                        config.rollbackEnabled = !config.rollbackEnabled;
                                        btn.setMessage(Text.translatable("nozh.config.option.rollback",
                                                        config.rollbackEnabled ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                }));
                y += 25;

                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.targets"));
                y += 20;

                addSlider(x, y, 200, 20, "nozh.config.targetfps", config.targetFps, 30, 240,
                                val -> config.targetFps = val.intValue(),
                                val -> val.intValue() + " FPS",
                                "nozh.config.tooltip.targetfps");
        }

        private void initVisuals(int x, int y) {
                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.adaptive"));
                y += 20;

                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.option.adaptive_quality",
                                                config.adaptiveVisualQualityEnabled ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.adaptive_quality"),
                                btn -> {
                                        config.adaptiveVisualQualityEnabled = !config.adaptiveVisualQualityEnabled;
                                        btn.setMessage(
                                                        Text.translatable("nozh.config.option.adaptive_quality",
                                                                        config.adaptiveVisualQualityEnabled
                                                                                        ? Text.translatable(
                                                                                                        "options.on")
                                                                                        : Text.translatable(
                                                                                                        "options.off")));
                                }));
                y += 25;

                addSlider(x, y, 200, 20, "nozh.config.sensitivity", config.adaptiveVisualQualitySensitivityMs, 0.5, 5.0,
                                val -> config.adaptiveVisualQualitySensitivityMs = val,
                                val -> String.format("%.1f ms", val),
                                "nozh.config.tooltip.sensitivity");
                y += 25;

                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.hud"));
                y += 20;

                // HUD: Enable
                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.option.hud",
                                                config.showHud ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.hud"),
                                btn -> {
                                        config.showHud = !config.showHud;
                                        btn.setMessage(Text.translatable("nozh.config.option.hud",
                                                        config.showHud ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                        this.clearAndInit();
                                }));
                y += 25;

                if (config.showHud) {
                        // HUD: Mode Cycle
                        addDrawableChild(new TooltipButton(x, y, 200, 20,
                                        Text.translatable("nozh.config.hud.mode",
                                                        Text.translatable("nozh.config.hud.mode."
                                                                        + config.hudMode.toLowerCase())),
                                        Text.translatable("nozh.config.hud.mode.tooltip"),
                                        btn -> {
                                                switch (config.hudMode) {
                                                        case "ANALYST" -> config.hudMode = "EXPERT";
                                                        case "EXPERT" -> config.hudMode = "COMPACT";
                                                        case "COMPACT" -> config.hudMode = "ANALYST";
                                                        default -> config.hudMode = "ANALYST";
                                                }
                                                btn.setMessage(Text.translatable("nozh.config.hud.mode",
                                                                Text.translatable("nozh.config.hud.mode."
                                                                                + config.hudMode.toLowerCase())));
                                        }));
                        y += 25;

                        // HUD: Suggestions Toggle
                        addDrawableChild(new TooltipButton(x, y, 200, 20,
                                        Text.translatable("nozh.config.hud.suggestions",
                                                        config.showHudSuggestions ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")),
                                        Text.translatable("nozh.config.hud.suggestions.tooltip"),
                                        btn -> {
                                                config.showHudSuggestions = !config.showHudSuggestions;
                                                btn.setMessage(Text.translatable("nozh.config.hud.suggestions",
                                                                config.showHudSuggestions
                                                                                ? Text.translatable("options.on")
                                                                                : Text.translatable("options.off")));
                                        }));
                        y += 25;

                        // HUD: Debug Overlay Toggle
                        addDrawableChild(new TooltipButton(x, y, 200, 20,
                                        Text.translatable("nozh.config.hud.debug_overlay",
                                                        config.showDebugOverlay ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")),
                                        Text.translatable("nozh.config.hud.debug_overlay.tooltip"),
                                        btn -> {
                                                config.showDebugOverlay = !config.showDebugOverlay;
                                                btn.setMessage(Text.translatable("nozh.config.hud.debug_overlay",
                                                                config.showDebugOverlay
                                                                                ? Text.translatable("options.on")
                                                                                : Text.translatable("options.off")));
                                        }));
                        y += 25;

                        // HUD: Anchor Cycle
                        addDrawableChild(new TooltipButton(x, y, 200, 20,
                                        Text.translatable("nozh.config.hud.anchor",
                                                        Text.translatable("nozh.config.hud.anchor."
                                                                        + config.hudAnchor.toLowerCase())),
                                        Text.translatable("nozh.config.hud.anchor.tooltip"),
                                        btn -> {
                                                switch (config.hudAnchor) {
                                                        case "TOP_LEFT" -> config.hudAnchor = "TOP_RIGHT";
                                                        case "TOP_RIGHT" -> config.hudAnchor = "BOTTOM_RIGHT";
                                                        case "BOTTOM_RIGHT" -> config.hudAnchor = "BOTTOM_LEFT";
                                                        case "BOTTOM_LEFT" -> config.hudAnchor = "TOP_LEFT";
                                                        default -> config.hudAnchor = "TOP_LEFT";
                                                }
                                                btn.setMessage(Text.translatable("nozh.config.hud.anchor",
                                                                Text.translatable("nozh.config.hud.anchor."
                                                                                + config.hudAnchor.toLowerCase())));
                                        }));
                        y += 25;

                        // HUD: Scale Slider
                        addSlider(x, y, 200, 20, "nozh.config.hud.scale", config.hudScale, 0.5, 2.0,
                                        val -> config.hudScale = val,
                                        val -> String.format("%.0f%%", val * 100),
                                        "nozh.config.hud.scale.tooltip");
                        y += 25;

                        // HUD: Offsets (X / Y)
                        addSlider(x, y, 95, 20, "nozh.config.hud.offset_x", (double) config.hudOffsetX, -200, 200,
                                        val -> config.hudOffsetX = val.intValue(),
                                        val -> val.intValue() + "px",
                                        "nozh.config.hud.offset_x.tooltip");

                        addSlider(x + 105, y, 95, 20, "nozh.config.hud.offset_y", (double) config.hudOffsetY, -200, 200,
                                        val -> config.hudOffsetY = val.intValue(),
                                        val -> val.intValue() + "px",
                                        "nozh.config.hud.offset_y.tooltip");
                        y += 25;
                }
        }

        private void initAdvanced(int x, int y) {
                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.calibration"));
                y += 20;

                // Rollback in General tab, removed here for redundancy

                // Hybrid Model Toggle
                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.option.hybrid",
                                                config.hybridModelEnabled ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.hybrid"),
                                btn -> {
                                        config.hybridModelEnabled = !config.hybridModelEnabled;
                                        btn.setMessage(Text.translatable("nozh.config.option.hybrid",
                                                        config.hybridModelEnabled ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                }));
                y += 25;

                // Benchmark Mode Toggle
                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.option.benchmark",
                                                config.benchmarkModeEnabled ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.benchmark"),
                                btn -> {
                                        config.benchmarkModeEnabled = !config.benchmarkModeEnabled;
                                        btn.setMessage(Text.translatable("nozh.config.option.benchmark",
                                                        config.benchmarkModeEnabled ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                }));
                y += 25;

                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.history"));
                y += 20;

                // History Limit Slider
                addSlider(x, y, 200, 20, "nozh.config.option.history_limit", (double) config.historyMaxEntries, 10, 200,
                                val -> config.historyMaxEntries = val.intValue(),
                                val -> String.valueOf(val.intValue()),
                                "nozh.config.tooltip.history_limit");
                y += 25;

                // Decision Budget Slider
                addSlider(x, y, 200, 20, "nozh.config.option.decision_budget", (double) config.governorDecisionBudgetMs,
                                1, 50,
                                val -> config.governorDecisionBudgetMs = val.intValue(),
                                val -> val.intValue() + " ms",
                                "nozh.config.tooltip.decision_budget");
                y += 25;

                // Debug & Safe Mode
                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.option.debug",
                                                config.debugLogs ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.debug"),
                                btn -> {
                                        config.debugLogs = !config.debugLogs;
                                        btn.setMessage(Text.translatable("nozh.config.option.debug",
                                                        config.debugLogs ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                }));
                y += 25;

                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.option.safemode_force",
                                                config.safeModeForce ? Text.translatable("options.on")
                                                                : Text.translatable("options.off")),
                                Text.translatable("nozh.config.tooltip.safemode_force"),
                                btn -> {
                                        config.safeModeForce = !config.safeModeForce;
                                        btn.setMessage(Text.translatable("nozh.config.option.safemode_force",
                                                        config.safeModeForce ? Text.translatable("options.on")
                                                                        : Text.translatable("options.off")));
                                }));
        }

        private void initSystem(int x, int y) {
                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.system.data"));
                y += 20;

                // Export Config
                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.button.export_config"),
                                Text.translatable("nozh.config.tooltip.export_config"),
                                btn -> {
                                        net.minecraft.client.MinecraftClient.getInstance().keyboard
                                                        .setClipboard(dev.nozh.core.config.ConfigManager.serialize());
                                        btn.setMessage(Text.literal("Copied!"));
                                }));
                y += 25;

                // Reload Config
                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.button.reload_config"),
                                Text.translatable("nozh.config.tooltip.reload_config"),
                                btn -> {
                                        dev.nozh.core.config.ConfigManager.load();
                                        // Config object instance might change, so we refresh our reference and UI
                                        this.config = dev.nozh.core.config.ConfigManager.getConfig();
                                        this.clearAndInit();
                                }));
                y += 25;

                addDrawableChild(new SectionLabel(x, y, "nozh.config.section.system.actions"));
                y += 20;

                // Factory Reset
                addDrawableChild(new TooltipButton(x, y, 200, 20,
                                Text.translatable("nozh.config.button.reset_full"),
                                Text.translatable("nozh.config.tooltip.reset_full"),
                                btn -> {
                                        net.minecraft.client.MinecraftClient.getInstance()
                                                        .setScreen(new net.minecraft.client.gui.screen.ConfirmScreen(
                                                                        (result) -> {
                                                                                if (result) {
                                                                                        // Reset logic
                                                                                        dev.nozh.core.config.ConfigManager
                                                                                                        .resetToDefaults();
                                                                                        this.config = dev.nozh.core.config.ConfigManager
                                                                                                        .getConfig();
                                                                                }
                                                                                net.minecraft.client.MinecraftClient
                                                                                                .getInstance()
                                                                                                .setScreen(this);
                                                                        },
                                                                        Text.translatable("nozh.config.confirm.title"),
                                                                        Text.translatable(
                                                                                        "nozh.config.confirm.message"),
                                                                        Text.translatable("nozh.config.confirm.yes"),
                                                                        Text.translatable("nozh.config.confirm.no")));
                                }));
        }

        // ============================================================================================
        // Helper Methods & Classes
        // ============================================================================================

        // Since we are overriding standard methods, we must ensure these helpers exist

        private void addSlider(int x, int y, int w, int h, String name, double current, double min, double max,
                        java.util.function.Consumer<Double> onSet,
                        java.util.function.Function<Double, String> display, String tooltip) {
                TooltipSlider slider = new TooltipSlider(x, y, w, h, Text.translatable(name).append(": "),
                                Text.translatable(tooltip), min, max,
                                current, onSet, display);
                addDrawableChild(slider);
        }

        interface TooltipRenderable {
                void renderTooltip(DrawContext context, int mouseX, int mouseY,
                                net.minecraft.client.font.TextRenderer textRenderer);
        }

        static class TooltipButton extends ButtonWidget implements TooltipRenderable {
                private final Text tooltip;

                public TooltipButton(int x, int y, int width, int height, Text message, Text tooltip,
                                PressAction onPress) {
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
                        super(x, y, width, height, Text.empty(),
                                        (Math.max(min, Math.min(max, current)) - min) / (max - min));
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
                        super(x, y, 200, 15, Text.translatable(text));
                        this.active = false; // Not clickable
                }

                @Override
                protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
                        // Draw just the text, centered, with a distinct color (Gold/Yellow)
                        context.drawCenteredTextWithShadow(
                                        net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                                        this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + 3,
                                        0xFFAA00);
                        // Do NOT call super.renderButton() to avoid drawing the button texture
                        // Draw a small underline to emphasize it's a section
                        context.fill(this.getX(), this.getY() + 14, this.getX() + this.getWidth(), this.getY() + 15,
                                        0x40FFAA00);
                }

                @Override
                protected void appendClickableNarrations(
                                net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
                }
        }
}
