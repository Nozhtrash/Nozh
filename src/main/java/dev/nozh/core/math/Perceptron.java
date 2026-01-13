package dev.nozh.core.math;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A lightweight, zero-dependency Perceptron implementation for on-device learning.
 * <p>
 * Designed for micro-second predictions in the render loop.
 * Uses a sigmoid activation function for [0.0, 1.0] output probabilities.
 */
public class Perceptron {
    private final double[] weights;
    private double bias;
    private final double learningRate;

    /**
     * @param inputSize Number of input features
     * @param learningRate Adjustment step size (0.01 - 0.1 recommended)
     */
    public Perceptron(int inputSize, double learningRate) {
        this.weights = new double[inputSize];
        this.learningRate = learningRate;
        this.bias = 0.0;
        
        // Initialize with small random weights to break symmetry
        for (int i = 0; i < inputSize; i++) {
            this.weights[i] = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
        }
    }

    /**
     * Forward pass: Compute the output probability.
     * 
     * @param inputs Feature vector (must match inputSize)
     * @return Probability between 0.0 and 1.0
     */
    public double predict(double[] inputs) {
        if (inputs.length != weights.length) {
            throw new IllegalArgumentException("Input size mismatch");
        }

        double weightedSum = bias;
        for (int i = 0; i < inputs.length; i++) {
            weightedSum += inputs[i] * weights[i];
        }

        return sigmoid(weightedSum);
    }

    /**
     * Backward pass: Adjust weights based on error.
     * 
     * @param inputs The input features that generated the prediction
     * @param target The actual observed outcome (0.0 = fine, 1.0 = lag spike)
     */
    public void train(double[] inputs, double target) {
        double prediction = predict(inputs);
        double error = target - prediction;

        // Gradient descent step
        // d/dw = error * input * sigmoid_derivative
        double derivative = prediction * (1.0 - prediction); // Derivative of sigmoid

        for (int i = 0; i < weights.length; i++) {
            weights[i] += error * inputs[i] * derivative * learningRate;
        }

        bias += error * derivative * learningRate;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    public double[] getWeights() {
        return Arrays.copyOf(weights, weights.length);
    }
}
