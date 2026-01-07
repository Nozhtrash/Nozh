package dev.nozh.core.learning;

import dev.nozh.core.context.Scenario;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Machine learning engine for performance optimization.
 * 
 * Uses reinforcement learning principles:
 * - Reward = FPS improvement
 * - State = (Scenario, Hardware, Current FPS)
 * - Action = Provider adjustments
 * - Policy = Action selection strategy
 * 
 * Learns optimal action selection for different contexts.
 * 
 * TASK 10: Adaptive learning - reinforcement learning
 */
public final class PerformanceLearningEngine {

    private final Map<StateActionPair, QValue> qTable = new ConcurrentHashMap<>();
    private final ActionEffectivenessTracker effectivenessTracker;

    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.95;
    private static final double EXPLORATION_RATE = 0.15;

    public PerformanceLearningEngine(ActionEffectivenessTracker tracker) {
        this.effectivenessTracker = tracker;
    }

    /**
     * Get action value (Q-value) for state-action pair.
     */
    public double getActionValue(GameState state, String actionId) {
        StateActionPair pair = new StateActionPair(state, actionId);
        QValue qValue = qTable.get(pair);
        return qValue != null ? qValue.value : 0.0;
    }

    /**
     * Update Q-value based on observed reward.
     * 
     * Q(s,a) ← Q(s,a) + α[r + γ·max(Q(s',a')) - Q(s,a)]
     */
    public void updateFromExperience(
            GameState previousState,
            String actionTaken,
            double reward,
            GameState newState) {

        StateActionPair pair = new StateActionPair(previousState, actionTaken);
        QValue current = qTable.computeIfAbsent(pair, k -> new QValue());

        // Find max Q-value for new state
        double maxFutureQ = getMaxActionValue(newState);

        // Q-learning update
        double oldQ = current.value;
        double newQ = oldQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxFutureQ - oldQ);

        current.value = newQ;
        current.updateCount++;
    }

    /**
     * Get best action for given state.
     */
    public String getBestAction(GameState state, String[] availableActions) {
        if (availableActions == null || availableActions.length == 0) {
            return null;
        }

        // Epsilon-greedy: explore vs exploit
        if (Math.random() < EXPLORATION_RATE) {
            // Explore: random action
            int randomIndex = (int) (Math.random() * availableActions.length);
            return availableActions[randomIndex];
        }

        // Exploit: best known action
        String bestAction = availableActions[0];
        double bestValue = getActionValue(state, bestAction);

        for (int i = 1; i < availableActions.length; i++) {
            double value = getActionValue(state, availableActions[i]);
            if (value > bestValue) {
                bestValue = value;
                bestAction = availableActions[i];
            }
        }

        return bestAction;
    }

    /**
     * Get maximum Q-value for a state across all actions.
     */
    private double getMaxActionValue(GameState state) {
        double maxQ = 0.0;

        for (Map.Entry<StateActionPair, QValue> entry : qTable.entrySet()) {
            if (entry.getKey().state.matches(state)) {
                maxQ = Math.max(maxQ, entry.getValue().value);
            }
        }

        return maxQ;
    }

    /**
     * Calculate reward from FPS change.
     */
    public static double calculateReward(double fpsBefore, double fpsAfter, double visualImpact, double gameplayImpact) {
        double fpsDelta = fpsAfter - fpsBefore;

        // Reward proportional to FPS gain
        double fpsReward = fpsDelta / 10.0; // +10 FPS = +1.0 reward

        // Penalties for negative impacts
        double visualPenalty = visualImpact * 2.0;
        double gameplayPenalty = gameplayImpact * 3.0;

        return fpsReward - visualPenalty - gameplayPenalty;
    }

    /**
     * Get learning statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("qTableSize", qTable.size());
        stats.put("explorationRate", EXPLORATION_RATE);
        stats.put("learningRate", LEARNING_RATE);
        return stats;
    }

    /**
     * State-action pair for Q-table.
     */
    private static class StateActionPair {
        final GameState state;
        final String actionId;
        final int hashCode;

        StateActionPair(GameState state, String actionId) {
            this.state = state;
            this.actionId = actionId;
            this.hashCode = 31 * state.hashCode() + actionId.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StateActionPair other)) return false;
            return state.equals(other.state) && actionId.equals(other.actionId);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Q-value with metadata.
     */
    private static class QValue {
        double value = 0.0;
        int updateCount = 0;
    }

    /**
     * Game state representation for learning.
     */
    public static class GameState {
        final Scenario scenario;
        final int fpsRange; // Bucketed: 0-30, 30-60, 60-120, 120+
        final String hardwareProfile; // low/medium/high

        public GameState(Scenario scenario, double currentFps, String hardwareProfile) {
            this.scenario = scenario;
            this.fpsRange = bucketFps(currentFps);
            this.hardwareProfile = hardwareProfile;
        }

        private static int bucketFps(double fps) {
            if (fps < 30) return 0;
            if (fps < 60) return 1;
            if (fps < 120) return 2;
            return 3;
        }

        public boolean matches(GameState other) {
            return this.scenario == other.scenario
                    && this.fpsRange == other.fpsRange
                    && this.hardwareProfile.equals(other.hardwareProfile);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GameState other)) return false;
            return matches(other);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * scenario.hashCode() + fpsRange) + hardwareProfile.hashCode();
        }
    }
}
