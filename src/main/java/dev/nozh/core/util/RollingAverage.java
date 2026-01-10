package dev.nozh.core.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Calculadora de promedio móvil (rolling average) para métricas de rendimiento.
 * 
 * <p>Mantiene una ventana deslizante de valores y calcula el promedio eficientemente.
 * Thread-safe para operaciones de lectura concurrente.
 * 
 * @author Nozh Team
 * @since 0.5.0
 */
public final class RollingAverage {
    private final Deque<Double> values;
    private final int maxSize;
    private double sum = 0.0;
    
    /**
     * Crea un nuevo RollingAverage con tamaño de ventana especificado.
     * 
     * @param maxSize tamaño máximo de la ventana
     */
    public RollingAverage(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize debe ser positivo: " + maxSize);
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
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return; // Ignorar valores inválidos
        }
        
        if (values.size() >= maxSize) {
            double removed = values.removeFirst();
            sum -= removed;
        }
        values.addLast(value);
        sum += value;
    }
    
    /**
     * Obtiene el promedio actual de valores en la ventana.
     * 
     * @return promedio, o 0.0 si no hay valores
     */
    public double getAverage() {
        return values.isEmpty() ? 0.0 : sum / values.size();
    }
    
    /**
     * Verifica si hay suficientes muestras para un promedio confiable.
     * 
     * @return true si hay al menos 20% del tamaño máximo de muestras
     */
    public boolean hasEnoughSamples() {
        return values.size() >= Math.max(1, maxSize / 5);
    }
    
    /**
     * Obtiene el número actual de valores en la ventana.
     * 
     * @return cantidad de valores
     */
    public int size() {
        return values.size();
    }
    
    /**
     * Limpia todos los valores de la ventana.
     */
    public void clear() {
        values.clear();
        sum = 0.0;
    }
    
    /**
     * Obtiene el valor mínimo en la ventana actual.
     * 
     * @return valor mínimo, o Double.MAX_VALUE si no hay valores
     */
    public double getMin() {
        return values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.MAX_VALUE);
    }
    
    /**
     * Obtiene el valor máximo en la ventana actual.
     * 
     * @return valor máximo, o Double.MIN_VALUE si no hay valores
     */
    public double getMax() {
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.MIN_VALUE);
    }
    
    @Override
    public String toString() {
        return String.format("RollingAverage[size=%d/%d, avg=%.2f]", 
                           values.size(), maxSize, getAverage());
    }
}