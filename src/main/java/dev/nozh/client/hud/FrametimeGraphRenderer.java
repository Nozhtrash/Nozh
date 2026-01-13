package dev.nozh.client.hud;

import dev.nozh.NozhConstants;

import java.util.*;

/**
 * Renders real-time frametime graph like MSI Afterburner.
 * Shows frametime history and spikes visually.
 * 
 * @since 0.3.0
 * @author NOZH Team
 */
public final class FrametimeGraphRenderer {

    /**
     * Graph display style.
     */
    public enum GraphStyle {
        LINE("Line Graph", "Traditional line graph"),
        BAR("Bar Graph", "Vertical bars"),
        FILL("Filled Area", "Filled area graph");

        public final String displayName;
        public final String description;

        GraphStyle(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    /**
     * Graph point data.
     */
    public record GraphPoint(
            long timestamp,
            double frametime,
            boolean isSpike) {
    }

    // Optimized circular buffer
    private final double[] frametimeHistory;
    private final boolean[] spikeFlags;
    private int historyIndex = 0;
    private int historySize = 0;

    private GraphStyle style;
    private int width;
    private int height;
    private final int maxDataPoints;

    // Display settings
    private int backgroundColor;
    private int lineColor;
    private int spikeColor;
    private int targetLineColor;
    private double targetFrametime;

    // Scale
    private double minFrametime;
    private double maxFrametime;
    private boolean autoScale;

    /**
     * Constructs a new FrametimeGraphRenderer.
     */
    public FrametimeGraphRenderer() {
        this.maxDataPoints = 240; // 4 seconds at 60fps
        this.frametimeHistory = new double[maxDataPoints];
        this.spikeFlags = new boolean[maxDataPoints];

        this.style = GraphStyle.LINE;
        this.width = 200;
        this.height = 60;

        // Colors
        this.backgroundColor = 0x80000000; // Semi-transparent black
        this.lineColor = 0xFF00FF00; // Green
        this.spikeColor = 0xFFFF0000; // Red
        this.targetLineColor = 0xFFFFFF00; // Yellow

        // Scale
        this.minFrametime = 0;
        this.maxFrametime = 50; // 50ms = 20fps
        this.autoScale = true;
        this.targetFrametime = 16.67; // 60fps target
    }

    /**
     * Adds a new frametime sample.
     * 
     * @param frametimeMs frametime in milliseconds
     */
    public void addSample(double frametimeMs) {
        boolean isSpike = frametimeMs > targetFrametime * 1.5;

        frametimeHistory[historyIndex] = frametimeMs;
        spikeFlags[historyIndex] = isSpike;

        historyIndex = (historyIndex + 1) % maxDataPoints;
        if (historySize < maxDataPoints) {
            historySize++;
        }

        // Auto-scale if enabled (every 60 frames to be cheap)
        if (autoScale && historyIndex % 60 == 0) {
            updateAutoScale();
        }
    }

    /**
     * Updates auto-scale based on recent data.
     */
    private void updateAutoScale() {
        if (historySize == 0)
            return;

        double max = 0;
        for (int i = 0; i < historySize; i++) {
            if (frametimeHistory[i] > max) {
                max = frametimeHistory[i];
            }
        }

        // Add 20% headroom
        maxFrametime = Math.max(20, max * 1.2);
    }

    /**
     * Renders the graph directly to the context.
     * Zero-allocation in hot path.
     */
    public void render(net.minecraft.client.gui.DrawContext context, int x, int y) {
        if (historySize < 2)
            return;

        // Background
        context.fill(x, y, x + width, y + height, backgroundColor);

        // Target Line
        int targetY = y + height - (int) ((targetFrametime / maxFrametime) * height);
        if (targetY >= y && targetY <= y + height) {
            context.fill(x, targetY, x + width, targetY + 1, targetLineColor);
        }

        // Draw graph
        // We iterate backwards from current index to draw newest at right

        int spacing = 1; // 1 pixel per point? or stretch?
        // Let's stretch to width
        double stepX = (double) width / (maxDataPoints - 1);

        // Draw points
        for (int i = 0; i < historySize - 1; i++) {
            // Calculate circular indices
            // Visual index 0 is oldest, historySize-1 is newest
            // Actual newest is at historyIndex - 1

            int actualIdx = (historyIndex - 1 - i);
            if (actualIdx < 0)
                actualIdx += maxDataPoints;

            int nextActualIdx = (actualIdx - 1);
            if (nextActualIdx < 0)
                nextActualIdx += maxDataPoints;

            // X goes from right to left
            double currentX = x + width - (i * stepX);
            double nextX = x + width - ((i + 1) * stepX);

            if (nextX < x)
                break; // Out of bounds

            double val = frametimeHistory[actualIdx];
            double nextVal = frametimeHistory[nextActualIdx];

            int currentY = y + height - (int) ((val / maxFrametime) * height);
            int nextY = y + height - (int) ((nextVal / maxFrametime) * height);

            // Clamp
            currentY = Math.max(y, Math.min(y + height, currentY));
            nextY = Math.max(y, Math.min(y + height, nextY));

            int color = spikeFlags[actualIdx] ? spikeColor : lineColor;

            if (style == GraphStyle.BAR) {
                context.fill((int) nextX, nextY, (int) currentX, y + height, color);
            } else {
                // Simple line drawing (Bresenham or just fillRect for segments)
                // DrawContext doesn't have drawLine, so we iterate or use fill
                drawLine(context, (int) currentX, currentY, (int) nextX, nextY, color);
            }
        }
    }

    private void drawLine(net.minecraft.client.gui.DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // Very basic line implementation using vertical strips if needed,
        // but for now let's just draw points or small rects to save perf
        // Real line drawing in MC requires BufferBuilder which is complex here without
        // direct access
        // We will approximate with a 1px wide fill

        int w = x2 - x1;
        int h = y2 - y1;

        // If mostly horizontal
        if (Math.abs(w) > Math.abs(h)) {
            context.fill(x2, y2, x2 + 1, y2 + 1, color);
        } else {
            context.fill(x2, Math.min(y1, y2), x2 + 1, Math.max(y1, y2), color);
        }
    }

    /**
     * Sets graph style.
     * 
     * @param style new style
     */
    public void setStyle(GraphStyle style) {
        this.style = style;
    }

    /**
     * Sets graph dimensions.
     * 
     * @param width  width in pixels
     * @param height height in pixels
     */
    public void setDimensions(int width, int height) {
        this.width = Math.max(50, width);
        this.height = Math.max(20, height);
    }

    /**
     * Sets target frametime.
     * 
     * @param targetMs target in milliseconds
     */
    public void setTargetFrametime(double targetMs) {
        this.targetFrametime = Math.max(1, targetMs);
    }

    /**
     * Clears all data.
     */
    public void clear() {
        historyIndex = 0;
        historySize = 0;
    }

    /**
     * Gets data point count.
     * 
     * @return number of data points
     */
    public int getDataPointCount() {
        return historySize;
    }

    /**
     * Gets current style.
     * 
     * @return current style
     */
    public GraphStyle getStyle() {
        return style;
    }

    /**
     * Gets graph width.
     * 
     * @return width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets graph height.
     * 
     * @return height in pixels
     */
    public int getHeight() {
        return height;
    }
}
