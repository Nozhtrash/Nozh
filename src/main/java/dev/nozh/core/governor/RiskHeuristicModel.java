package dev.nozh.core.governor;

import dev.nozh.core.intelligence.SessionLearning;
import dev.nozh.core.state.RuntimeState;

/**
 * Multi-variable heuristic model for spike risk estimation.
 * Uses recent feature history and dynamic weights.
 */
public final class RiskHeuristicModel {

    private static final int HISTORY_SIZE = 20;
    private static final double BASE_THRESHOLD = 0.6;
    private static final double MIN_THRESHOLD = 0.45;
    private static final double MAX_THRESHOLD = 0.8;

    private final RiskFeatureVector[] history = new RiskFeatureVector[HISTORY_SIZE];
    private int historyIndex = 0;
    private int historyCount = 0;

    private double tickWeight = 0.26;
    private double renderWeight = 0.24;
    private double gcWeight = 0.20;
    private double entityWeight = 0.15;
    private double chunkWeight = 0.15;

    private double riskThreshold = BASE_THRESHOLD;

    public void addSample(RiskFeatureVector vector) {
        if (vector == null || !vector.hasSignal()) {
            return;
        }
        history[historyIndex] = vector;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        if (historyCount < HISTORY_SIZE) {
            historyCount++;
        }
    }

    public RiskScore score(RiskFeatureVector vector) {
        if (vector == null || !vector.hasSignal()) {
            return new RiskScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double tickScore = normalizeTick(vector.tickMs());
        double renderScore = normalizeRender(vector.renderMs());
        double gcScore = normalizeGc(vector.gcMs());
        double entityScore = normalizeEntities(vector.entityCount());
        double chunkScore = normalizeChunk(vector.chunkLoadRate());

        double tickComponent = tickScore * tickWeight;
        double renderComponent = renderScore * renderWeight;
        double gcComponent = gcScore * gcWeight;
        double entityComponent = entityScore * entityWeight;
        double chunkComponent = chunkScore * chunkWeight;

        double total = tickComponent + renderComponent + gcComponent + entityComponent + chunkComponent;
        return new RiskScore(total, tickComponent, renderComponent, gcComponent, entityComponent, chunkComponent);
    }

    public double getRiskThreshold() {
        return riskThreshold;
    }

    public boolean isReady() {
        return historyCount >= 6;
    }

    public void updateWeights(SessionLearning sessionLearning) {
        if (sessionLearning == null) {
            return;
        }
        int count = sessionLearning.getPredictionCount();
        if (count < 5) {
            return;
        }

        double accuracy = sessionLearning.getPredictionAccuracy();
        double avgConfidence = sessionLearning.getPredictionAvgConfidence();
        double accuracyBias = 0.5 - accuracy;
        double confidenceBias = 0.5 - avgConfidence;
        double adjustment = (accuracyBias * 0.2) + (confidenceBias * 0.1);

        riskThreshold = clamp(BASE_THRESHOLD + adjustment, MIN_THRESHOLD, MAX_THRESHOLD);

        double tickVariance = variance(v -> v.tickMs());
        double renderVariance = variance(v -> v.renderMs());
        double gcVariance = variance(v -> v.gcMs());
        double entityVariance = variance(v -> v.entityCount());
        double chunkVariance = variance(v -> v.chunkLoadRate());

        double totalVariance = tickVariance + renderVariance + gcVariance + entityVariance + chunkVariance;
        if (totalVariance <= 0.0) {
            return;
        }

        tickWeight = clamp(tickVariance / totalVariance, 0.15, 0.35);
        renderWeight = clamp(renderVariance / totalVariance, 0.15, 0.35);
        gcWeight = clamp(gcVariance / totalVariance, 0.10, 0.30);
        entityWeight = clamp(entityVariance / totalVariance, 0.10, 0.30);
        chunkWeight = clamp(chunkVariance / totalVariance, 0.10, 0.30);

        normalizeWeights();
    }

    public void recordOutcome(RuntimeState state, boolean predictedSpike) {
        if (state == null) {
            return;
        }
        boolean actualSpike = state.spikeCount() > 0;
        adjustWeightForOutcome(predictedSpike, actualSpike);
    }

    private void adjustWeightForOutcome(boolean predictedSpike, boolean actualSpike) {
        if (predictedSpike == actualSpike) {
            tickWeight += 0.005;
            renderWeight += 0.005;
        } else {
            gcWeight += 0.01;
        }
        normalizeWeights();
    }

    private void normalizeWeights() {
        double sum = tickWeight + renderWeight + gcWeight + entityWeight + chunkWeight;
        if (sum <= 0) {
            tickWeight = 0.26;
            renderWeight = 0.24;
            gcWeight = 0.20;
            entityWeight = 0.15;
            chunkWeight = 0.15;
            return;
        }
        tickWeight /= sum;
        renderWeight /= sum;
        gcWeight /= sum;
        entityWeight /= sum;
        chunkWeight /= sum;
    }

    private double variance(FeatureReader reader) {
        if (historyCount < 2) {
            return 0.0;
        }
        double mean = 0.0;
        for (int i = 0; i < historyCount; i++) {
            mean += reader.read(history[i]);
        }
        mean /= historyCount;

        double sum = 0.0;
        for (int i = 0; i < historyCount; i++) {
            double delta = reader.read(history[i]) - mean;
            sum += delta * delta;
        }
        return sum / (historyCount - 1);
    }

    private double normalizeTick(double tickMs) {
        return clamp(tickMs / 50.0, 0.0, 1.2);
    }

    private double normalizeRender(double renderMs) {
        return clamp(renderMs / 25.0, 0.0, 1.2);
    }

    private double normalizeGc(double gcMs) {
        return clamp(gcMs / 80.0, 0.0, 1.2);
    }

    private double normalizeEntities(int entities) {
        return clamp(entities / 250.0, 0.0, 1.2);
    }

    private double normalizeChunk(int chunkRate) {
        return clamp(chunkRate / 30.0, 0.0, 1.2);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private interface FeatureReader {
        double read(RiskFeatureVector vector);
    }
}
