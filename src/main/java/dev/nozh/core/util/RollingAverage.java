package dev.nozh.core.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling average calculator.
 * 
 * Thread-safe implementation for performance metrics.
 */
public class RollingAverage {
    
    private final int maxSize;
    private final Deque<Double> values;
    private double sum;
    
    public RollingAverage(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
        this.values = new ArrayDeque<>(maxSize);
        this.sum = 0.0;
    }
    
    /**
     * Add value to rolling window.
     */
    public synchronized void add(double value) {
        values.addLast(value);
        sum += value;
        
        if (values.size() > maxSize) {
            double removed = values.pollFirst();
            sum -= removed;
        }
    }
    
    /**
     * Get current average.
     */
    public synchronized double getAverage() {
        if (values.isEmpty()) {
            return 0.0;
        }
        return sum / values.size();
    }
    
    /**
     * Get sample count.
     */
    public synchronized int size() {
        return values.size();
    }
    
    /**
     * Clear all values.
     */
    public synchronized void clear() {
        values.clear();
        sum = 0.0;
    }
}