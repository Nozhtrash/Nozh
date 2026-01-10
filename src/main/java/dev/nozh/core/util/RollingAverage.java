package dev.nozh.core.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Calculadora de promedio móvil con ventana deslizante.
 * Thread-safe y optimizada para métricas de rendimiento.
 * 
 * @author Nozh Team
 * @since 0.5.0
 */
public final class RollingAverage {
    private final Deque<Double> values;
    private final int maxSize;
    private double sum = 0.0;
    private final Object lock = new Object();
    
    /**
     * Constructor con tamaño de ventana.
     * 
     * @param maxSize tamaño máximo de la ventana (debe ser > 0)
     * @throws IllegalArgumentException si maxSize <= 0
     */
    public RollingAverage(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize debe ser mayor a 0");
        }
        this.maxSize = maxSize;
        this.values = new ArrayDeque<>(maxSize);
    }
    
    /**
     * Agrega un nuevo valor a la ventana.
     * Si la ventana está llena, elimina el valor más antiguo.
     * 
     * @param value valor a agregar
     */
    public void add(double value) {
        synchronized (lock) {
            if (values.size() >= maxSize) {
                double removed = values.removeFirst();
                sum -= removed;
            }
            values.addLast(value);
            sum += value;
        }
    }
    
    /**
     * Obtiene el promedio actual de la ventana.
     * 
     * @return promedio, o 0.0 si no hay valores
     */
    public double getAverage() {
        synchronized (lock) {
            return values.isEmpty() ? 0.0 : sum / values.size();
        }
    }
    
    /**
     * Verifica si hay suficientes muestras para cálculos confiables.
     * 
     * @return true si hay al menos 20% de la capacidad llena
     */
    public boolean hasEnoughSamples() {
        synchronized (lock) {
            return values.size() >= Math.max(1, maxSize / 5);
        }
    }
    
    /**
     * Obtiene el número de valores actuales en la ventana.
     * 
     * @return cantidad de valores
     */
    public int size() {
        synchronized (lock) {
            return values.size();
        }
    }
    
    /**
     * Limpia todos los valores de la ventana.
     */
    public void clear() {
        synchronized (lock) {
            values.clear();
            sum = 0.0;
        }
    }
    
    /**
     * Obtiene el valor mínimo en la ventana actual.
     * 
     * @return valor mínimo, o Double.MAX_VALUE si está vacía
     */
    public double getMin() {
        synchronized (lock) {
            return values.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(Double.MAX_VALUE);
        }
    }
    
    /**
     * Obtiene el valor máximo en la ventana actual.
     * 
     * @return valor máximo, o Double.MIN_VALUE si está vacía
     */
    public double getMax() {
        synchronized (lock) {
            return values.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(Double.MIN_VALUE);
        }
    }
    
    @Override
    public String toString() {
        synchronized (lock) {
            return String.format("RollingAverage[size=%d/%d, avg=%.2f, min=%.2f, max=%.2f]",
                values.size(), maxSize, getAverage(), getMin(), getMax());
        }
    }
}