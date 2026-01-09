package dev.nozh.core.scenario;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analyzes player actions within a temporal window.
 * 
 * ROADMAP: Phase 2, Sprint 3 - Action Window Analysis
 * 
 * Tracks last 30 seconds of player activity to determine behavior patterns.
 */
public class ActionWindowAnalyzer {
    
    private static final int WINDOW_SIZE = 600; // 30 seconds at 20 TPS
    private final Deque<PlayerAction> recentActions = new ArrayDeque<>(WINDOW_SIZE);
    
    public static class PlayerAction {
        public final ActionType type;
        public final long timestamp;
        public final BlockPos position;
        public final Entity target;
        
        public PlayerAction(ActionType type, BlockPos position, Entity target) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.position = position;
            this.target = target;
        }
    }
    
    public enum ActionType {
        BLOCK_BREAK,
        BLOCK_PLACE,
        ATTACK_ENTITY,
        USE_ITEM,
        INVENTORY_OPEN,
        JUMP,
        SPRINT,
        SNEAK
    }
    
    /**
     * Record a player action.
     */
    public void recordAction(ActionType type, BlockPos position, Entity target) {
        PlayerAction action = new PlayerAction(type, position, target);
        recentActions.addLast(action);
        
        // Maintain 30-second window
        long cutoffTime = System.currentTimeMillis() - 30000;
        while (!recentActions.isEmpty() && 
               recentActions.peekFirst().timestamp < cutoffTime) {
            recentActions.pollFirst();
        }
        
        // Limit size
        while (recentActions.size() > WINDOW_SIZE) {
            recentActions.pollFirst();
        }
    }
    
    /**
     * Analyze recent actions and determine behavior pattern.
     */
    public ScenarioAnalysis analyze() {
        if (recentActions.isEmpty()) {
            return ScenarioAnalysis.idle();
        }
        
        // Count action types
        Map<ActionType, Long> counts = recentActions.stream()
            .collect(Collectors.groupingBy(a -> a.type, Collectors.counting()));
        
        long attacks = counts.getOrDefault(ActionType.ATTACK_ENTITY, 0L);
        long blocks = counts.getOrDefault(ActionType.BLOCK_PLACE, 0L) + 
                     counts.getOrDefault(ActionType.BLOCK_BREAK, 0L);
        long movement = counts.getOrDefault(ActionType.JUMP, 0L) + 
                       counts.getOrDefault(ActionType.SPRINT, 0L);
        long inventory = counts.getOrDefault(ActionType.INVENTORY_OPEN, 0L);
        
        int totalActions = recentActions.size();
        
        // Pattern detection with confidence scoring
        
        // COMBAT: High attacks + movement
        if (attacks > 10 && movement > 20) {
            double confidence = Math.min(0.95, (attacks + movement) / (double) totalActions);
            return ScenarioAnalysis.combat(confidence, "High combat activity");
        }
        
        // BUILDING: Many block operations, low movement
        if (blocks > 15 && movement < 10) {
            double confidence = blocks / (double) totalActions;
            return ScenarioAnalysis.building(confidence, "Block placement pattern");
        }
        
        // EXPLORING: High movement, low other activity
        if (movement > 30 && attacks < 3 && blocks < 3) {
            double confidence = movement / (double) totalActions;
            return ScenarioAnalysis.exploring(confidence, "High mobility");
        }
        
        // ORGANIZING: Inventory usage
        if (inventory > 5 && movement < 5) {
            double confidence = inventory / (double) totalActions;
            return ScenarioAnalysis.organizing(confidence, "Inventory management");
        }
        
        return ScenarioAnalysis.standard();
    }
    
    /**
     * Get total actions in window.
     */
    public int getTotalActions() {
        return recentActions.size();
    }
    
    /**
     * Clear all recorded actions.
     */
    public void clear() {
        recentActions.clear();
    }
    
    /**
     * Analysis result container.
     */
    public static class ScenarioAnalysis {
        private final Scenario scenario;
        private final double confidence;
        private final String reason;
        
        private ScenarioAnalysis(Scenario scenario, double confidence, String reason) {
            this.scenario = scenario;
            this.confidence = confidence;
            this.reason = reason;
        }
        
        public static ScenarioAnalysis idle() {
            return new ScenarioAnalysis(Scenario.STANDARD, 0.5, "No recent activity");
        }
        
        public static ScenarioAnalysis combat(double confidence, String reason) {
            return new ScenarioAnalysis(Scenario.COMBAT, confidence, reason);
        }
        
        public static ScenarioAnalysis building(double confidence, String reason) {
            return new ScenarioAnalysis(Scenario.BUILDING, confidence, reason);
        }
        
        public static ScenarioAnalysis exploring(double confidence, String reason) {
            return new ScenarioAnalysis(Scenario.EXPLORING, confidence, reason);
        }
        
        public static ScenarioAnalysis organizing(double confidence, String reason) {
            return new ScenarioAnalysis(Scenario.ORGANIZING, confidence, reason);
        }
        
        public static ScenarioAnalysis standard() {
            return new ScenarioAnalysis(Scenario.STANDARD, 0.6, "Standard gameplay");
        }
        
        public Scenario getScenario() { return scenario; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }
        
        public boolean isBuilding() { return scenario == Scenario.BUILDING; }
        public boolean isExploring() { return scenario == Scenario.EXPLORING; }
        public boolean isOrganizing() { return scenario == Scenario.ORGANIZING; }
        public boolean isCombat() { return scenario == Scenario.COMBAT; }
        public int getTotalActions() { return 0; } // Placeholder
    }
}