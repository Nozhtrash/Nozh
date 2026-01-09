package dev.nozh.core.learning;

import dev.nozh.NozhConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Enhanced Q-Learning with exploration/exploitation balance.
 * Features epsilon-greedy strategy with adaptive decay.
 */
public class ImprovedQLearning {
    
    // Learning parameters
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.95;
    
    // Exploration parameters
    private static final double INITIAL_EPSILON = 0.3;  // 30% initial exploration
    private static final double MIN_EPSILON = 0.05;      // Minimum 5% exploration
    private static final double EPSILON_DECAY = 0.995;   // Decay rate
    
    private double epsilon = INITIAL_EPSILON;
    private final Map<StateActionPair, Double> qTable = new ConcurrentHashMap<>();
    
    private int totalEpisodes = 0;
    private int successfulEpisodes = 0;
    
    /**
     * Select action using epsilon-greedy strategy.
     * 
     * @param state current game state
     * @param availableActions actions that can be taken
     * @return selected action
     */
    public String selectAction(GameState state, String[] availableActions) {
        if (availableActions == null || availableActions.length == 0) {
            throw new IllegalArgumentException("No available actions");
        }
        
        // Epsilon-greedy: explore vs exploit
        if (Math.random() < epsilon) {
            // EXPLORE: Random action
            String randomAction = availableActions[
                ThreadLocalRandom.current().nextInt(availableActions.length)
            ];
            NozhConstants.LOGGER.debug("[Q-Learning] EXPLORING: selected random action {}", randomAction);
            return randomAction;
        } else {
            // EXPLOIT: Best known action
            String bestAction = getBestKnownAction(state, availableActions);
            NozhConstants.LOGGER.debug("[Q-Learning] EXPLOITING: selected best action {}", bestAction);
            return bestAction;
        }
    }
    
    /**
     * Get the best known action for a state.
     */
    private String getBestKnownAction(GameState state, String[] availableActions) {
        String bestAction = null;
        double bestQValue = Double.NEGATIVE_INFINITY;
        
        for (String action : availableActions) {
            StateActionPair pair = new StateActionPair(state, action);
            double qValue = qTable.getOrDefault(pair, 0.0);
            
            if (qValue > bestQValue) {
                bestQValue = qValue;
                bestAction = action;
            }
        }
        
        // If no action has been tried, return first one
        return bestAction != null ? bestAction : availableActions[0];
    }
    
    /**
     * Update Q-value after action execution.
     * 
     * @param state state before action
     * @param action action taken
     * @param reward reward received
     * @param nextState state after action
     */
    public void updateQValue(GameState state, String action, double reward, GameState nextState) {
        StateActionPair pair = new StateActionPair(state, action);
        
        double oldQ = qTable.getOrDefault(pair, 0.0);
        double maxNextQ = getMaxQValue(nextState);
        
        // Q-learning update rule
        double newQ = oldQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - oldQ);
        
        qTable.put(pair, newQ);
        
        NozhConstants.LOGGER.debug("[Q-Learning] Updated Q({}, {}) : {} -> {} (reward={})",
                                 state, action, oldQ, newQ, reward);
    }
    
    /**
     * Get maximum Q-value for a state.
     */
    private double getMaxQValue(GameState state) {
        return qTable.entrySet().stream()
            .filter(e -> e.getKey().state.equals(state))
            .mapToDouble(Map.Entry::getValue)
            .max()
            .orElse(0.0);
    }
    
    /**
     * Update after episode completion.
     * Adjusts epsilon based on success.
     * 
     * @param success whether the episode was successful
     */
    public void updateAfterEpisode(boolean success) {
        totalEpisodes++;
        if (success) {
            successfulEpisodes++;
        }
        
        if (success) {
            // Decay epsilon after successful episodes
            epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
            NozhConstants.LOGGER.debug("[Q-Learning] Success! Epsilon decayed to {}", epsilon);
        } else {
            // Increase exploration after failures
            epsilon = Math.min(0.5, epsilon * 1.1);
            NozhConstants.LOGGER.debug("[Q-Learning] Failure! Epsilon increased to {}", epsilon);
        }
    }
    
    /**
     * Get current exploration rate.
     */
    public double getEpsilon() {
        return epsilon;
    }
    
    /**
     * Get success rate.
     */
    public double getSuccessRate() {
        return totalEpisodes > 0 ? (double) successfulEpisodes / totalEpisodes : 0.0;
    }
    
    /**
     * Get Q-value for specific state-action pair.
     */
    public double getQValue(GameState state, String action) {
        return qTable.getOrDefault(new StateActionPair(state, action), 0.0);
    }
    
    /**
     * Get total number of learned state-action pairs.
     */
    public int getQTableSize() {
        return qTable.size();
    }
    
    /**
     * Reset epsilon to initial value (for testing or reset).
     */
    public void resetEpsilon() {
        epsilon = INITIAL_EPSILON;
        totalEpisodes = 0;
        successfulEpisodes = 0;
    }
    
    /**
     * Clear all learned Q-values.
     */
    public void clearQTable() {
        qTable.clear();
        resetEpsilon();
        NozhConstants.LOGGER.info("[Q-Learning] Q-table cleared");
    }
    
    /**
     * State-action pair key for Q-table.
     */
    private static class StateActionPair {
        final GameState state;
        final String action;
        
        StateActionPair(GameState state, String action) {
            this.state = state;
            this.action = action;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StateActionPair)) return false;
            StateActionPair that = (StateActionPair) o;
            return state.equals(that.state) && action.equals(that.action);
        }
        
        @Override
        public int hashCode() {
            return 31 * state.hashCode() + action.hashCode();
        }
    }
    
    /**
     * Simplified game state representation.
     */
    public static class GameState {
        final String scenario;
        final int fpsRange;  // 0-30, 30-60, 60-90, 90+
        final String bottleneck; // CPU, GPU, BALANCED, NONE
        
        public GameState(String scenario, int fps, String bottleneck) {
            this.scenario = scenario;
            this.fpsRange = fps < 30 ? 0 : (fps < 60 ? 1 : (fps < 90 ? 2 : 3));
            this.bottleneck = bottleneck;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GameState)) return false;
            GameState that = (GameState) o;
            return fpsRange == that.fpsRange && 
                   scenario.equals(that.scenario) && 
                   bottleneck.equals(that.bottleneck);
        }
        
        @Override
        public int hashCode() {
            return 31 * (31 * scenario.hashCode() + fpsRange) + bottleneck.hashCode();
        }
        
        @Override
        public String toString() {
            return String.format("State{%s,FPS:%d,BN:%s}", scenario, fpsRange, bottleneck);
        }
    }
}
