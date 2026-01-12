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

    private final Deque<GraphPoint> dataPoints;
    private GraphStyle style;
    private int width;
    private int height;
    private int maxDataPoints;

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
        this.dataPoints = new ArrayDeque<>();
        this.style = GraphStyle.LINE;
        this.width = 200;
        this.height = 60;
        this.maxDataPoints = 120; // 2 seconds at 60fps

        // Colors
        this.backgroundColor = 0x80000000; // Semi-transparent black
        this.lineColor = 0x00FF00; // Green
        this.spikeColor = 0xFF0000; // Red
        this.targetLineColor = 0xFFFF00; // Yellow

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

        GraphPoint point = new GraphPoint(
                System.currentTimeMillis(),
                frametimeMs,
                isSpike);

        dataPoints.addLast(point);

        // Trim old data
        while (dataPoints.size() > maxDataPoints) {
            dataPoints.removeFirst();
        }

        // Auto-scale if enabled
        if (autoScale) {
            updateAutoScale();
        }
    }

    /**
     * Updates auto-scale based on recent data.
     */
    private void updateAutoScale() {
        if (dataPoints.isEmpty())
            return;

        double max = 0;
        for (GraphPoint point : dataPoints) {
            if (point.frametime() > max) {
                max = point.frametime();
            }
        }

        // Add 20% headroom
        maxFrametime = Math.max(20, max * 1.2);
    }

    /**
     * Gets normalized Y position for a frametime value.
     * 
     * @param frametime frametime in ms
     * @return normalized Y (0.0 to 1.0)
     */
    public double getNormalizedY(double frametime) {
        if (maxFrametime <= minFrametime)
            return 0.5;
        return 1.0 - (frametime - minFrametime) / (maxFrametime - minFrametime);
    }

    /**
     * Calculates graph positions for rendering.
     * 
     * @return list of X,Y pairs (relative to graph origin)
     */
    public List<double[]> getGraphPoints() {
        List<double[]> points = new ArrayList<>();

        int i = 0;
        int total = dataPoints.size();

        for (GraphPoint point : dataPoints) {
            double x = (double) i / Math.max(1, total - 1) * width;
            double y = getNormalizedY(point.frametime()) * height;
            points.add(new double[] { x, y });
            i++;
        }

        return points;
    }

    /**
     * Gets spike positions for rendering warnings.
     * 
     * @return list of spike X positions
     */
    public List<Double> getSpikePositions() {
        List<Double> spikes = new ArrayList<>();

        int i = 0;
        int total = dataPoints.size();

        for (GraphPoint point : dataPoints) {
            if (point.isSpike()) {
                double x = (double) i / Math.max(1, total - 1) * width;
                spikes.add(x);
            }
            i++;
        }

        return spikes;
    }

    /**
     * Gets target frametime Y position.
     * 
     * @return Y position for target line
     */
    public double getTargetLineY() {
        return getNormalizedY(targetFrametime) * height;
    }

    /**
     * Gets current average frametime.
     * 
     * @return average frametime in ms
     */
    public double getAverageFrametime() {
        if (dataPoints.isEmpty())
            return 0;
        return dataPoints.stream()
                .mapToDouble(GraphPoint::frametime)
                .average()
                .orElse(0);
    }

    /**
     * Gets current P95 frametime.
     * 
     * @return P95 frametime in ms
     */
    public double getP95Frametime() {
        if (dataPoints.isEmpty())
            return 0;

        List<Double> sorted = dataPoints.stream()
                .map(GraphPoint::frametime)
                .sorted(Comparator.reverseOrder())
                .toList();

        int p95Index = (int) (sorted.size() * 0.05);
        return sorted.get(Math.min(p95Index, sorted.size() - 1));
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
        dataPoints.clear();
    }

    /**
     * Gets data point count.
     * 
     * @return number of data points
     */
    public int getDataPointCount() {
        return dataPoints.size();
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
