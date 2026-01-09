package dev.nozh.core.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling average calculator for performance metrics.
 * 
 * @author Nozh Team
 * @since 0.5.0
 */
public final class RollingAverage {
    private final Deque<Double> values;
    private final int maxSize;
    private double sum = 0.0;
    
    public RollingAverage(int maxSize) {
        this.maxSize = maxSize;
        this.values = new ArrayDeque<>(maxSize);
    }
    
    public void add(double value) {
        if (values.size() >= maxSize) {
            double removed = values.removeFirst();
            sum -= removed;
        }
        values.addLast(value);
        sum += value;
    }
    
    public double getAverage() {
        return values.isEmpty() ? 0.0 : sum / values.size();
    }
    
    public boolean hasEnoughSamples() {
        return values.size() >= 20;
    }
    
    public void clear() {
        values.clear();
        sum = 0.0;
    }
    
    public int size() {
        return values.size();
    }
}
