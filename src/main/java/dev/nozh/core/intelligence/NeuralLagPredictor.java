package dev.nozh.core.intelligence;

import dev.nozh.core.math.Perceptron;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Predicts frame drops using a lightweight neural network (Perceptron).
 * <p>
 * This model learns in real-time from the user's specific hardware capabilities.
 * It takes various game state metrics as input and outputs the probability of a lag spike.
 */
public class NeuralLagPredictor {
    private static final int INPUT_FEATURES = 4;
    private static final double LEARNING_RATE = 0.05;
    
    // Feature indices
    private static final int IDX_ENTITY_COUNT = 0;
    private static final int IDX_PARTICLE_COUNT = 1;
    private static final int IDX_CHUNK_UPDATES = 2;
    private static final int IDX_PLAYER_SPEED = 3;

    private final Perceptron perceptron;
    private final AtomicReference<PredictionResult> lastPrediction = new AtomicReference<>(new PredictionResult(0.0, false));

    public NeuralLagPredictor() {
        this.perceptron = new Perceptron(INPUT_FEATURES, LEARNING_RATE);
    }

    /**
     * Helper record to store prediction state
     */
    public record PredictionResult(double probability, boolean isSpikePredicted) {}

    /**
     * Run inference to predict if a lag spike is imminent.
     * 
     * @param entityCount Number of loaded entities
     * @param particleCount Number of active particles
     * @param chunkUpdates Number of chunks updated this tick
     * @param playerSpeed Player velocity magnitude
     * @return PredictionResult containing probability
     */
    public PredictionResult predict(int entityCount, int particleCount, int chunkUpdates, double playerSpeed) {
        double[] inputs = normalizeInputs(entityCount, particleCount, chunkUpdates, playerSpeed);
        double probability = perceptron.predict(inputs);
        
        // Threshold of 0.7 usually indicates high confidence
        boolean isSpike = probability > 0.7;
        
        PredictionResult result = new PredictionResult(probability, isSpike);
        lastPrediction.set(result);
        return result;
    }

    /**
     * Learn from what actually happened.
     * Call this AFTER the frame is rendered and we know if it lagged or not.
     * 
     * @param actuallyLagged True if the frame time exceeded the target (e.g. >33ms)
     * @param entityCount Snapshot of state during prediction
     * @param particleCount Snapshot of state during prediction
     * @param chunkUpdates Snapshot of state during prediction
     * @param playerSpeed Snapshot of state during prediction
     */
    public void train(boolean actuallyLagged, int entityCount, int particleCount, int chunkUpdates, double playerSpeed) {
        double[] inputs = normalizeInputs(entityCount, particleCount, chunkUpdates, playerSpeed);
        double target = actuallyLagged ? 1.0 : 0.0;
        perceptron.train(inputs, target);
    }

    /**
     * Normalize inputs to roughly [0.0, 1.0] range for better convergence.
     * Rough max values based on vanilla limits.
     */
    private double[] normalizeInputs(int entities, int particles, int chunks, double speed) {
        return new double[] {
            Math.min(entities / 500.0, 1.0),   // Max 500 entities
            Math.min(particles / 2000.0, 1.0), // Max 2000 particles
            Math.min(chunks / 50.0, 1.0),      // Max 50 chunk updates
            Math.min(speed / 2.0, 1.0)         // Max 2.0 blocks/tick usually
        };
    }
    
    public double[] getWeights() {
        return perceptron.getWeights();
    }
}
