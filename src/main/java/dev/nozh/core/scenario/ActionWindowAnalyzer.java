package dev.nozh.core.scenario;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analyzes player actions within a time window to detect activity patterns.
 * 
 * <p>Tracks recent player actions (30 seconds) and analyzes patterns
 * to determine current activity type (combat, building, exploring, etc.).
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 2 Sprint 3)
 */
public final class ActionWindowAnalyzer {
    private static final int WINDOW_SIZE = 600; // 30 seconds at 20 TPS
    private static final long WINDOW_DURATION_MS = 30000; // 30 seconds
    
    private final Deque<PlayerAction> recentActions = new ArrayDeque<>(WINDOW_SIZE);
    
    /**
     * Player action types for pattern detection.
     */
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
     * Record of a single player action.
     */
    public static class PlayerAction {
        public ActionType type;
        public long timestamp;
        public BlockPos position;
        public Entity target;
        
        public PlayerAction(ActionType type) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Result of action window analysis.
     */
    public static class ScenarioAnalysis {
        private final ScenarioType type;
        private final double confidence;
        
        private ScenarioAnalysis(ScenarioType type, double confidence) {
            this.type = type;
            this.confidence = confidence;
        }
        
        public static ScenarioAnalysis combat(double confidence) {
            return new ScenarioAnalysis(ScenarioType.COMBAT, confidence);
        }
        
        public static ScenarioAnalysis building(double confidence) {
            return new ScenarioAnalysis(ScenarioType.BUILDING, confidence);
        }
        
        public static ScenarioAnalysis exploring(double confidence) {
            return new ScenarioAnalysis(ScenarioType.EXPLORING, confidence);
        }
        
        public static ScenarioAnalysis organizing(double confidence) {
            return new ScenarioAnalysis(ScenarioType.ORGANIZING, confidence);
        }
        
        public static ScenarioAnalysis idle() {
            return new ScenarioAnalysis(ScenarioType.IDLE, 1.0);
        }
        
        public static ScenarioAnalysis standard() {
            return new ScenarioAnalysis(ScenarioType.STANDARD, 0.5);
        }
        
        public boolean isCombat() { return type == ScenarioType.COMBAT; }
        public boolean isBuilding() { return type == ScenarioType.BUILDING; }
        public boolean isExploring() { return type == ScenarioType.EXPLORING; }
        public boolean isOrganizing() { return type == ScenarioType.ORGANIZING; }
        
        public double getConfidence() { return confidence; }
        public int getTotalActions() { return 0; } // Placeholder
    }
    
    private enum ScenarioType {
        COMBAT, BUILDING, EXPLORING, ORGANIZING, IDLE, STANDARD
    }
    
    /**
     * Record a player action.
     */
    public void recordAction(ActionType type, Object... data) {
        PlayerAction action = new PlayerAction(type);
        
        // Populate additional data if provided
        if (data.length > 0 && data[0] instanceof BlockPos) {
            action.position = (BlockPos) data[0];
        }
        if (data.length > 1 && data[1] instanceof Entity) {
            action.target = (Entity) data[1];
        }
        
        recentActions.addLast(action);
        
        // Maintain window size (remove old actions)
        cleanupOldActions();
    }
    
    /**
     * Remove actions older than window duration.
     */
    private void cleanupOldActions() {
        long cutoffTime = System.currentTimeMillis() - WINDOW_DURATION_MS;
        
        while (!recentActions.isEmpty() && 
               recentActions.peekFirst().timestamp < cutoffTime) {
            recentActions.pollFirst();
        }
        
        // Also enforce max size
        while (recentActions.size() > WINDOW_SIZE) {
            recentActions.pollFirst();
        }
    }
    
    /**
     * Analyze recent actions to determine activity pattern.
     */
    public ScenarioAnalysis analyze() {
        cleanupOldActions();
        
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
        
        // Pattern analysis with confidence scoring
        if (attacks > 10 && movement > 20) {
            double confidence = Math.min(0.95, attacks / 20.0);
            return ScenarioAnalysis.combat(confidence);
        }
        
        if (blocks > 15 && movement < 10) {
            double confidence = Math.min(0.90, blocks / 30.0);
            return ScenarioAnalysis.building(confidence);
        }
        
        if (movement > 30 && attacks < 3 && blocks < 3) {
            double confidence = Math.min(0.85, movement / 50.0);
            return ScenarioAnalysis.exploring(confidence);
        }
        
        if (inventory > 5 && movement < 5) {
            double confidence = Math.min(0.80, inventory / 10.0);
            return ScenarioAnalysis.organizing(confidence);
        }
        
        return ScenarioAnalysis.standard();
    }
    
    /**
     * Get total action count in current window.
     */
    public int getActionCount() {
        cleanupOldActions();
        return recentActions.size();
    }
    
    /**
     * Clear all recorded actions.
     */
    public void clear() {
        recentActions.clear();
    }
}
