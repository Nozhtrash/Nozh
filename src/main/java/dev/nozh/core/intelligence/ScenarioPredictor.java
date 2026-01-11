package dev.nozh.core.intelligence;

import dev.nozh.api.Scenario;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Predicts upcoming scenarios based on player behavior patterns.
 * Uses sliding window analysis to anticipate combat, exploration, building.
 * 
 * INTEGRATION: Core intelligence system
 * CONTRACT: Thread-safe, minimal allocation
 */
public final class ScenarioPredictor {

    private static final int WINDOW_SIZE = 20;
    private static final long PREDICTION_HORIZON_MS = 5000; // 5 seconds lookahead
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    /**
     * Scenario prediction with confidence and timing.
     */
    public record ScenarioPrediction(
        Scenario predictedScenario,
        double confidence,
        long expectedInMs,
        String reasoning
    ) {}

    /**
     * Tracked event for pattern analysis.
     */
    private static class Event {
        final Scenario scenario;
        final long timestamp;
        final double velocity;
        final int actionCount;

        Event(Scenario scenario, long timestamp, double velocity, int actionCount) {
            this.scenario = scenario;
            this.timestamp = timestamp;
            this.velocity = velocity;
            this.actionCount = actionCount;
        }
    }

    private final Deque<Event> eventWindow = new ArrayDeque<>(WINDOW_SIZE);
    private volatile Scenario currentScenario = Scenario.UNKNOWN;
    private volatile Scenario lastPrediction = null;
    private final AtomicInteger correctPredictions = new AtomicInteger(0);
    private final AtomicInteger totalPredictions = new AtomicInteger(0);
    private final AtomicLong lastPredictionTime = new AtomicLong(0);

    /**
     * Record current scenario and context.
     */
    public void recordScenario(Scenario scenario, double playerVelocity, int recentActions) {
        long now = System.currentTimeMillis();
        
        synchronized (eventWindow) {
            if (eventWindow.size() >= WINDOW_SIZE) {
                eventWindow.removeFirst();
            }
            eventWindow.addLast(new Event(scenario, now, playerVelocity, recentActions));
        }

        // Validate last prediction if exists
        if (lastPrediction != null && lastPrediction == scenario) {
            correctPredictions.incrementAndGet();
        }
        
        this.currentScenario = scenario;
    }

    /**
     * Predict next scenario based on recent patterns.
     */
    public ScenarioPrediction predictNextScenario() {
        Event[] events;
        synchronized (eventWindow) {
            if (eventWindow.size() < 3) {
                return new ScenarioPrediction(
                    currentScenario,
                    0.5,
                    PREDICTION_HORIZON_MS,
                    "Insufficient data for prediction"
                );
            }
            events = eventWindow.toArray(new Event[0]);
        }

        long now = System.currentTimeMillis();
        totalPredictions.incrementAndGet();
        lastPredictionTime.set(now);

        // Analyze patterns
        double avgVelocity = 0;
        int combatCount = 0;
        int explorationCount = 0;
        int buildingCount = 0;
        int idleCount = 0;

        for (Event event : events) {
            avgVelocity += event.velocity;
            switch (event.scenario) {
                case COMBAT -> combatCount++;
                case EXPLORATION -> explorationCount++;
                case BUILDING -> buildingCount++;
                case IDLE, AFK -> idleCount++;
            }
        }
        avgVelocity /= events.length;

        // Recent trend (last 5 events more weight)
        Scenario recentTrend = events[events.length - 1].scenario;
        int trendCount = 0;
        for (int i = Math.max(0, events.length - 5); i < events.length; i++) {
            if (events[i].scenario == recentTrend) {
                trendCount++;
            }
        }

        // Make prediction
        Scenario predicted;
        double confidence;
        String reasoning;

        // High velocity -> likely combat or exploration
        if (avgVelocity > 0.3) {
            if (combatCount > explorationCount) {
                predicted = Scenario.COMBAT;
                confidence = Math.min(0.95, 0.6 + (combatCount * 0.05));
                reasoning = String.format("High velocity (%.2f) + combat pattern (%d/%d events)",
                    avgVelocity, combatCount, events.length);
            } else {
                predicted = Scenario.EXPLORATION;
                confidence = Math.min(0.9, 0.6 + (explorationCount * 0.05));
                reasoning = String.format("High velocity (%.2f) + exploration pattern",
                    avgVelocity);
            }
        }
        // Low velocity -> building or idle
        else if (avgVelocity < 0.1) {
            if (buildingCount > idleCount) {
                predicted = Scenario.BUILDING;
                confidence = Math.min(0.85, 0.5 + (buildingCount * 0.05));
                reasoning = "Low velocity + building pattern";
            } else {
                predicted = Scenario.IDLE;
                confidence = Math.min(0.8, 0.5 + (idleCount * 0.05));
                reasoning = "Low velocity + idle pattern";
            }
        }
        // Trend continuation
        else {
            predicted = recentTrend;
            confidence = Math.min(0.75, 0.5 + (trendCount * 0.05));
            reasoning = String.format("Trend continuation (%s, %d/5 recent)",
                recentTrend, trendCount);
        }

        // Boost confidence if strong trend
        if (trendCount >= 4) {
            confidence = Math.min(0.95, confidence + 0.15);
        }

        lastPrediction = predicted;
        return new ScenarioPrediction(predicted, confidence, PREDICTION_HORIZON_MS, reasoning);
    }

    /**
     * Pre-warm settings for predicted scenario (stub).
     */
    public void preWarmForScenario(Scenario scenario) {
        // TODO: Integration with governor to pre-adjust settings
        // This would prepare settings before scenario actually changes
    }

    /**
     * Get prediction accuracy percentage.
     */
    public double getPredictionAccuracy() {
        int total = totalPredictions.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) correctPredictions.get() / total;
    }

    /**
     * Get current scenario.
     */
    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    /**
     * Get statistics summary.
     */
    public String getStatsSummary() {
        int total = totalPredictions.get();
        int correct = correctPredictions.get();
        double accuracy = total > 0 ? (double) correct / total * 100 : 0;
        
        return String.format("Predictions: %d | Accurate: %d | Accuracy: %.1f%%",
            total, correct, accuracy);
    }

    /**
     * Reset prediction statistics.
     */
    public void reset() {
        synchronized (eventWindow) {
            eventWindow.clear();
        }
        correctPredictions.set(0);
        totalPredictions.set(0);
        lastPrediction = null;
    }
}
