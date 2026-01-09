package dev.nozh.core.learning;

import dev.nozh.core.scenario.Scenario;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Improved Q-Learning with exploration.
 * 
 * ROADMAP: Phase 2, Sprint 4 - Q-Learning with Exploration
 * 
 * Implements epsilon-greedy strategy for balancing exploration and exploitation.
 */
public class ImprovedQLearning {
    
    private static final double INITIAL_EPSILON = 0.3;  // 30% exploration
    private static final double MIN_EPSILON = 0.05;     // Minimum 5%
    private static final double EPSILON_DECAY = 0.995;  // Decay per episode
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    
    private double epsilon = INITIAL_EPSILON;
    private final Map<StateActionPair, Double> qTable = new ConcurrentHashMap<>();
    
    /**
     * Select action using epsilon-greedy strategy.
     * 
     * @param state current game state
     * @param availableActions array of possible actions
     * @return selected action ID
     */
    public String selectAction(GameState state, String[] availableActions) {
        if (availableActions == null || availableActions.length == 0) {
            return null;
        }
        
        // Exploration: random action
        if (Math.random() < epsilon) {
            int index = ThreadLocalRandom.current().nextInt(availableActions.length);
            return availableActions[index];
        }
        
        // Exploitation: best known action
        return getBestKnownAction(state, availableActions);
    }
    
    /**
     * Get best action based on Q-values.
     */
    private String getBestKnownAction(GameState state, String[] actions) {
        String bestAction = actions[0];
        double bestValue = getQValue(state, bestAction);
        
        for (int i = 1; i < actions.length; i++) {
            double value = getQValue(state, actions[i]);
            if (value > bestValue) {
                bestValue = value;
                bestAction = actions[i];
            }
        }
        
        return bestAction;
    }
    
    /**
     * Update Q-value based on experience.
     * 
     * Q(s,a) = Q(s,a) + α * [reward + γ * max(Q(s',a')) - Q(s,a)]
     */
    public void updateQValue(GameState state, String action, 
                            double reward, GameState nextState) {
        StateActionPair pair = new StateActionPair(state, action);
        double oldQ = qTable.getOrDefault(pair, 0.0);
        double maxNextQ = getMaxQValue(nextState);
        
        double newQ = oldQ + LEARNING_RATE * (
            reward + DISCOUNT_FACTOR * maxNextQ - oldQ
        );
        
        qTable.put(pair, newQ);
    }
    
    /**
     * Get Q-value for state-action pair.
     */
    public double getQValue(GameState state, String action) {
        StateActionPair pair = new StateActionPair(state, action);
        return qTable.getOrDefault(pair, 0.0);
    }
    
    /**
     * Get maximum Q-value for next state.
     */
    private double getMaxQValue(GameState state) {
        return qTable.entrySet().stream()
            .filter(e -> e.getKey().state.equals(state))
            .mapToDouble(Map.Entry::getValue)
            .max()
            .orElse(0.0);
    }
    
    /**
     * Update epsilon after episode completion.
     * 
     * @param success whether the episode was successful
     */
    public void updateAfterEpisode(boolean success) {
        if (success) {
            // Decay epsilon on success - exploit more
            epsilon = Math.max(MIN_EPSILON, epsilon * EPSILON_DECAY);
        } else {
            // Increase epsilon on failure - explore more
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
     * State-action pair for Q-table key.
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
     * Game state representation.
     */
    public static class GameState {
        final Scenario scenario;
        final double fps;
        final String hardwareProfile;
        
        public GameState(Scenario scenario, double fps, String profile) {
            this.scenario = scenario;
            this.fps = fps;
            this.hardwareProfile = profile;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GameState)) return false;
            GameState state = (GameState) o;
            return scenario == state.scenario && 
                   Math.abs(fps - state.fps) < 10 && // FPS within 10
                   hardwareProfile.equals(state.hardwareProfile);
        }
        
        @Override
        public int hashCode() {
            return 31 * scenario.hashCode() + hardwareProfile.hashCode();
        }
    }
}