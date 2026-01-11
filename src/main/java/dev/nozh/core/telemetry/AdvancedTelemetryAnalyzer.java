package dev.nozh.core.telemetry;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced statistical analysis of telemetry data.
 * 
 * INTEGRATION: Telemetry analysis
 * CONTRACT: Pure functions, no state
 */
public final class AdvancedTelemetryAnalyzer {

    /**
     * Trend direction.
     */
    public enum TrendDirection {
        IMPROVING,
        STABLE,
        DEGRADING,
        UNKNOWN
    }

    /**
     * Calculate standard deviation.
     */
    public double calculateStandardDeviation(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);

        return Math.sqrt(variance);
    }

    /**
     * Calculate percentile.
     */
    public double calculatePercentile(List<Double> values, int percentile) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100");
        }

        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);

        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));

        return sorted.get(index);
    }

    /**
     * Calculate simple moving average.
     */
    public double calculateMovingAverage(List<Double> values, int window) {
        if (values == null || values.isEmpty() || window <= 0) {
            return 0.0;
        }

        int start = Math.max(0, values.size() - window);
        return values.subList(start, values.size()).stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    /**
     * Calculate exponential moving average.
     */
    public double calculateExponentialMovingAverage(List<Double> values, double alpha) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("Alpha must be between 0 and 1");
        }

        double ema = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            ema = alpha * values.get(i) + (1 - alpha) * ema;
        }

        return ema;
    }

    /**
     * Detect trend direction.
     */
    public TrendDirection detectTrend(List<Double> values) {
        if (values == null || values.size() < 2) {
            return TrendDirection.UNKNOWN;
        }

        // Simple linear regression to detect trend
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        // Threshold for considering stable (relative to mean)
        double mean = sumY / n;
        double relativeSlope = slope / mean;

        if (relativeSlope > 0.01) {
            return TrendDirection.IMPROVING;
        } else if (relativeSlope < -0.01) {
            return TrendDirection.DEGRADING;
        } else {
            return TrendDirection.STABLE;
        }
    }

    /**
     * Predict next value using linear regression.
     */
    public double predictNextValue(List<Double> values) {
        if (values == null || values.size() < 2) {
            return values != null && !values.isEmpty() ? values.get(values.size() - 1) : 0.0;
        }

        // Simple linear regression
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Predict for next index
        return slope * n + intercept;
    }

    /**
     * Detect anomalies using standard deviation.
     */
    public List<Integer> detectAnomalies(List<Double> values, double threshold) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(values);

        List<Integer> anomalies = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            double deviation = Math.abs(values.get(i) - mean);
            if (deviation > threshold * stdDev) {
                anomalies.add(i);
            }
        }

        return anomalies;
    }

    /**
     * Check if current value is an anomaly.
     */
    public boolean isCurrentValueAnomaly(double value, List<Double> historicalValues, double threshold) {
        if (historicalValues == null || historicalValues.isEmpty()) {
            return false;
        }

        double mean = historicalValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(historicalValues);

        double deviation = Math.abs(value - mean);
        return deviation > threshold * stdDev;
    }

    /**
     * Calculate correlation between two series.
     * Returns value between -1 (negative correlation) and 1 (positive correlation).
     */
    public double correlate(List<Double> series1, List<Double> series2) {
        if (series1 == null || series2 == null || series1.isEmpty() || series2.isEmpty()) {
            return 0.0;
        }

        int n = Math.min(series1.size(), series2.size());
        if (n < 2) {
            return 0.0;
        }

        double mean1 = series1.stream().limit(n).mapToDouble(Double::doubleValue).average().orElse(0.0);
        double mean2 = series2.stream().limit(n).mapToDouble(Double::doubleValue).average().orElse(0.0);

        double numerator = 0, sumSq1 = 0, sumSq2 = 0;

        for (int i = 0; i < n; i++) {
            double diff1 = series1.get(i) - mean1;
            double diff2 = series2.get(i) - mean2;
            numerator += diff1 * diff2;
            sumSq1 += diff1 * diff1;
            sumSq2 += diff2 * diff2;
        }

        double denominator = Math.sqrt(sumSq1 * sumSq2);
        if (denominator == 0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    /**
     * Calculate coefficient of variation (CV).
     * Useful for comparing variability relative to mean.
     */
    public double calculateCoefficientOfVariation(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean == 0) {
            return 0.0;
        }

        double stdDev = calculateStandardDeviation(values);
        return (stdDev / mean) * 100; // Return as percentage
    }
}
