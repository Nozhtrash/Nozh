package dev.nozh.client.hud;

import dev.nozh.NozhConstants;

import java.util.*;

/**
 * Professional HUD system with multiple display modes.
 * Clear, informative, and non-intrusive.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class NozhHudSystem {

    /**
     * HUD preset configurations.
     */
    public enum HudPreset {
        OFF("Off", "No HUD displayed"),
        MINIMAL("Minimal", "Just FPS + health indicator"),
        GAMER("Gamer", "FPS, ping, coords, direction"),
        ANALYST("Analyst", "Frametime graph, P95"),
        DEVELOPER("Developer", "Full telemetry, predictions"),
        STREAMER("Streamer", "Clean overlay for streams"),
        CUSTOM("Custom", "User-defined layout");

        public final String displayName;
        public final String description;

        HudPreset(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    /**
     * HUD widget configuration.
     */
    public record HudWidget(
            String id,
            String name,
            boolean visible,
            int x,
            int y,
            float scale,
            int color) {
        /**
         * Creates a default widget.
         */
        public static HudWidget createDefault(String id, String name) {
            return new HudWidget(id, name, true, 0, 0, 1.0f, 0xFFFFFF);
        }

        /**
         * Creates with visibility toggle.
         */
        public HudWidget withVisibility(boolean visible) {
            return new HudWidget(id, name, visible, x, y, scale, color);
        }

        /**
         * Creates with new position.
         */
        public HudWidget withPosition(int x, int y) {
            return new HudWidget(id, name, visible, x, y, scale, color);
        }
    }

    // Available widget IDs
    public static final String WIDGET_FPS_COUNTER = "fps_counter";
    public static final String WIDGET_FRAMETIME_GRAPH = "frametime_graph";
    public static final String WIDGET_P95_INDICATOR = "p95_indicator";
    public static final String WIDGET_SCENARIO_DISPLAY = "scenario_display";
    public static final String WIDGET_MEMORY_BAR = "memory_bar";
    public static final String WIDGET_ACTION_LOG = "action_log";
    public static final String WIDGET_SUGGESTIONS = "suggestions";
    public static final String WIDGET_MOD_STATUS = "mod_status";
    public static final String WIDGET_HARDWARE_MONITOR = "hardware_monitor";
    public static final String WIDGET_NETWORK_STATS = "network_stats";

    private HudPreset currentPreset;
    private final Map<String, HudWidget> widgets;
    private boolean enabled;

    /**
     * Constructs a new NozhHudSystem.
     */
    public NozhHudSystem() {
        this.currentPreset = HudPreset.MINIMAL;
        this.widgets = new LinkedHashMap<>();
        this.enabled = true;

        initializeWidgets();
        applyPreset(currentPreset);
    }

    /**
     * Initializes all available widgets.
     */
    private void initializeWidgets() {
        widgets.put(WIDGET_FPS_COUNTER, new HudWidget(
                WIDGET_FPS_COUNTER, "FPS Counter", true, 5, 5, 1.0f, 0x00FF00));

        widgets.put(WIDGET_FRAMETIME_GRAPH, new HudWidget(
                WIDGET_FRAMETIME_GRAPH, "Frametime Graph", false, 5, 20, 1.0f, 0xFFFFFF));

        widgets.put(WIDGET_P95_INDICATOR, new HudWidget(
                WIDGET_P95_INDICATOR, "P95 Indicator", false, 5, 85, 1.0f, 0xFFFF00));

        widgets.put(WIDGET_SCENARIO_DISPLAY, new HudWidget(
                WIDGET_SCENARIO_DISPLAY, "Scenario", false, 5, 100, 0.8f, 0xAAAAAA));

        widgets.put(WIDGET_MEMORY_BAR, new HudWidget(
                WIDGET_MEMORY_BAR, "Memory Bar", false, 5, 115, 0.8f, 0xFF5500));

        widgets.put(WIDGET_ACTION_LOG, new HudWidget(
                WIDGET_ACTION_LOG, "Action Log", false, 5, 130, 0.7f, 0x888888));

        widgets.put(WIDGET_SUGGESTIONS, new HudWidget(
                WIDGET_SUGGESTIONS, "Suggestions", false, 5, 200, 0.8f, 0x00AAFF));

        widgets.put(WIDGET_MOD_STATUS, new HudWidget(
                WIDGET_MOD_STATUS, "Mod Status", false, 5, 250, 0.7f, 0xAA00AA));

        widgets.put(WIDGET_HARDWARE_MONITOR, new HudWidget(
                WIDGET_HARDWARE_MONITOR, "Hardware Monitor", false, 5, 280, 0.7f, 0xFF0000));

        widgets.put(WIDGET_NETWORK_STATS, new HudWidget(
                WIDGET_NETWORK_STATS, "Network Stats", false, 5, 300, 0.7f, 0x00FFFF));
    }

    /**
     * Sets the HUD preset.
     *
     * @param preset preset to apply
     */
    public void setPreset(HudPreset preset) {
        this.currentPreset = preset;
        applyPreset(preset);
        NozhConstants.LOGGER.info("HUD preset changed to: {}", preset.displayName);
    }

    /**
     * Applies a preset configuration.
     */
    private void applyPreset(HudPreset preset) {
        // First hide all widgets
        widgets.replaceAll((id, widget) -> widget.withVisibility(false));

        switch (preset) {
            case OFF -> enabled = false;
            case MINIMAL -> {
                enabled = true;
                showWidget(WIDGET_FPS_COUNTER);
            }
            case GAMER -> {
                enabled = true;
                showWidget(WIDGET_FPS_COUNTER);
                showWidget(WIDGET_P95_INDICATOR);
                showWidget(WIDGET_NETWORK_STATS);
            }
            case ANALYST -> {
                enabled = true;
                showWidget(WIDGET_FPS_COUNTER);
                showWidget(WIDGET_FRAMETIME_GRAPH);
                showWidget(WIDGET_P95_INDICATOR);
                showWidget(WIDGET_SCENARIO_DISPLAY);
            }
            case DEVELOPER -> {
                enabled = true;
                // Show everything for developers
                widgets.replaceAll((id, widget) -> widget.withVisibility(true));
            }
            case STREAMER -> {
                enabled = true;
                showWidget(WIDGET_FPS_COUNTER);
                // Minimal, clean overlay
            }
            case CUSTOM -> enabled = true; // Keep current settings
        }
    }

    /**
     * Toggles a widget's visibility.
     *
     * @param widgetId widget to toggle
     */
    public void toggleWidget(String widgetId) {
        HudWidget widget = widgets.get(widgetId);
        if (widget != null) {
            widgets.put(widgetId, widget.withVisibility(!widget.visible()));
            currentPreset = HudPreset.CUSTOM;
        }
    }

    /**
     * Shows a specific widget.
     */
    private void showWidget(String widgetId) {
        HudWidget widget = widgets.get(widgetId);
        if (widget != null) {
            widgets.put(widgetId, widget.withVisibility(true));
        }
    }

    /**
     * Moves a widget to a new position.
     *
     * @param widgetId widget to move
     * @param x        new X position
     * @param y        new Y position
     */
    public void moveWidget(String widgetId, int x, int y) {
        HudWidget widget = widgets.get(widgetId);
        if (widget != null) {
            widgets.put(widgetId, widget.withPosition(x, y));
            currentPreset = HudPreset.CUSTOM;
        }
    }

    /**
     * Gets all widgets.
     *
     * @return list of all widgets
     */
    public List<HudWidget> getWidgets() {
        return new ArrayList<>(widgets.values());
    }

    /**
     * Gets visible widgets.
     *
     * @return list of visible widgets
     */
    public List<HudWidget> getVisibleWidgets() {
        return widgets.values().stream()
                .filter(HudWidget::visible)
                .toList();
    }

    /**
     * Checks if HUD is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled && currentPreset != HudPreset.OFF;
    }

    /**
     * Gets current preset.
     *
     * @return current preset
     */
    public HudPreset getCurrentPreset() {
        return currentPreset;
    }

    /**
     * Gets all available presets.
     *
     * @return array of presets
     */
    public static HudPreset[] getAvailablePresets() {
        return HudPreset.values();
    }
}
