package dev.nozh.core.util;

/**
 * Circular buffer for calculating rolling averages.
 * 
 * <p>Thread-safe implementation for performance metric smoothing.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 2 Sprint 4)
 */
public final class RollingAverage {
    private final double[] values;
    private int index = 0;
    private int count = 0;
    private double sum = 0.0;
    
    /**
     * Create rolling average with specified window size.
     * 
     * @param size window size (number of samples)
     */
    public RollingAverage(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.values = new double[size];
    }
    
    /**
     * Add a new value to the rolling average.
     */
    public synchronized void add(double value) {
        if (!Double.isFinite(value)) {
            return; // Ignore invalid values
        }
        
        // Remove old value from sum if buffer is full
        if (count == values.length) {
            sum -= values[index];
        } else {
            count++;
        }
        
        // Add new value
        values[index] = value;
        sum += value;
        
        // Move to next index (circular)
        index = (index + 1) % values.length;
    }
    
    /**
     * Get current rolling average.
     */
    public synchronized double getAverage() {
        if (count == 0) {
            return 0.0;
        }
        return sum / count;
    }
    
    /**
     * Get number of samples in buffer.
     */
    public synchronized int getCount() {
        return count;
    }
    
    /**
     * Check if buffer is full.
     */
    public synchronized boolean isFull() {
        return count == values.length;
    }
    
    /**
     * Clear all values.
     */
    public synchronized void clear() {
        index = 0;
        count = 0;
        sum = 0.0;
        for (int i = 0; i < values.length; i++) {
            values[i] = 0.0;
        }
    }
    
    /**
     * Get maximum value in buffer.
     */
    public synchronized double getMax() {
        if (count == 0) {
            return 0.0;
        }
        
        double max = values[0];
        for (int i = 1; i < count; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }
    
    /**
     * Get minimum value in buffer.
     */
    public synchronized double getMin() {
        if (count == 0) {
            return 0.0;
        }
        
        double min = values[0];
        for (int i = 1; i < count; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }
}
