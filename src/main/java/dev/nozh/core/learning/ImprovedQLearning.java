package dev.nozh.core.learning;

import dev.nozh.NozhConstants;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Improved Q-Learning implementation with exploration/exploitation balance.
 * 
 * <p>Uses epsilon-greedy strategy with decay for adaptive learning.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 2 Sprint 4)
 */
public final class ImprovedQLearning {
    private static final double INITIAL_EPSILON = 0.3; // 30% exploration
    private static final double MIN_EPSILON = 0.05; // minimum 5%
    private static final double EPSILON_DECAY = 0.995;
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    
    private double epsilon = INITIAL_EPSILON;
    private final Map<StateActionPair, Double> qTable = new ConcurrentHashMap<>();
    
    /**
     * State-Action pair for Q-table lookup.
     */
    public static class StateActionPair {
        private final String stateKey;
        private final String action;
        
        public StateActionPair(String stateKey, String action) {
            this.stateKey = stateKey;
            this.action = action;
        }
        
        public String getStateKey() { return stateKey; }
        public String getAction() { return action; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StateActionPair pair)) return false;
            return Objects.equals(stateKey, pair.stateKey) && 
                   Objects.equals(action, pair.action);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(stateKey, action);
        }
        
        @Override
        public String toString() {
            return stateKey + ":" + action;
        }
    }
    
    /**
     * Select action using epsilon-greedy strategy.
     * 
     * @param stateKey current state identifier
     * @param availableActions array of available actions
     * @return selected action
     */
    public String selectAction(String stateKey, String[] availableActions) {
        if (availableActions == null || availableActions.length == 0) {
            throw new IllegalArgumentException("No available actions");
        }
        
        // Exploration vs Exploitation
        if (ThreadLocalRandom.current().nextDouble() < epsilon) {
            // EXPLORE: random action
            int randomIndex = ThreadLocalRandom.current().nextInt(availableActions.length);
            String action = availableActions[randomIndex];
            NozhConstants.LOGGER.debug("Exploring: selected random action {}", action);
            return action;
        } else {
            // EXPLOIT: best known action
            String bestAction = getBestKnownAction(stateKey, availableActions);
            NozhConstants.LOGGER.debug("Exploiting: selected best action {}", bestAction);
            return bestAction;
        }
    }
    
    /**
     * Get best known action for state based on Q-values.
     */
    private String getBestKnownAction(String stateKey, String[] availableActions) {
        String bestAction = availableActions[0];
        double bestQValue = getQValue(stateKey, bestAction);
        
        for (int i = 1; i < availableActions.length; i++) {
            double qValue = getQValue(stateKey, availableActions[i]);
            if (qValue > bestQValue) {
                bestQValue = qValue;
                bestAction = availableActions[i];
            }
        }
        
        return bestAction;
    }
    
    /**
     * Get Q-value for state-action pair.
     */
    public double getQValue(String stateKey, String action) {
        StateActionPair pair = new StateActionPair(stateKey, action);
        return qTable.getOrDefault(pair, 0.0);
    }
    
    /**
     * Update Q-value based on experience.
     * 
     * @param stateKey current state
     * @param action action taken
     * @param reward reward received
     * @param nextStateKey resulting state
     */
    public void updateQValue(String stateKey, String action, 
                            double reward, String nextStateKey, 
                            String[] nextAvailableActions) {
        StateActionPair pair = new StateActionPair(stateKey, action);
        double oldQ = qTable.getOrDefault(pair, 0.0);
        double maxNextQ = getMaxQValue(nextStateKey, nextAvailableActions);
        
        // Q-learning update rule
        double newQ = oldQ + LEARNING_RATE * (
            reward + DISCOUNT_FACTOR * maxNextQ - oldQ
        );
        
        qTable.put(pair, newQ);
        
        NozhConstants.LOGGER.debug(
            "Q-update: {} | old={:.3f}, new={:.3f}, reward={:.3f}",
            pair, oldQ, newQ, reward
        );
    }
    
    /**
     * Get maximum Q-value for next state.
     */
    private double getMaxQValue(String stateKey, String[] availableActions) {
        if (availableActions == null || availableActions.length == 0) {
            return 0.0;
        }
        
        double maxQ = getQValue(stateKey, availableActions[0]);
        for (int i = 1; i < availableActions.length; i++) {
            maxQ = Math.max(maxQ, getQValue(stateKey, availableActions[i]));
        }
        return maxQ;
    }
    
    /**
     * Update epsilon after episode (decay or increase).
     * 
     * @param success whether the episode was successful
     */
    public void updateAfterEpisode(boolean success) {
        if (success) {
            // Decay epsilon on success
            epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
        } else {
            // Temporarily increase exploration on failure
            epsilon = Math.min(0.5, epsilon * 1.1);
        }
    }
    
    /**
     * Get current epsilon value.
     */
    public double getEpsilon() {
        return epsilon;
    }
    
    /**
     * Get Q-table size.
     */
    public int getQTableSize() {
        return qTable.size();
    }
    
    /**
     * Clear all learned Q-values.
     */
    public void clear() {
        qTable.clear();
        epsilon = INITIAL_EPSILON;
        NozhConstants.LOGGER.info("Q-Learning data cleared, epsilon reset to {}", INITIAL_EPSILON);
    }
}
