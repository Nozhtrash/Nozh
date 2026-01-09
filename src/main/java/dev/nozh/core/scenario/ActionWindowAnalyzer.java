package dev.nozh.core.scenario;

import dev.nozh.NozhConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes player actions over a temporal window (30 seconds).
 * Provides context for scenario detection.
 */
public class ActionWindowAnalyzer {
    
    private static final int WINDOW_SIZE_TICKS = 600; // 30 seconds at 20 TPS
    private final Deque<PlayerAction> recentActions = new ArrayDeque<>(WINDOW_SIZE_TICKS);
    
    public enum ActionType {
        BLOCK_BREAK,
        BLOCK_PLACE,
        ATTACK_ENTITY,
        USE_ITEM,
        INVENTORY_OPEN,
        JUMP,
        SPRINT,
        SNEAK,
        CAMERA_MOVE
    }
    
    public static class PlayerAction {
        public final ActionType type;
        public final long timestamp;
        public final BlockPos position;
        public final Entity target;
        
        public PlayerAction(ActionType type) {
            this(type, null, null);
        }
        
        public PlayerAction(ActionType type, BlockPos position, Entity target) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.position = position;
            this.target = target;
        }
    }
    
    /**
     * Record a player action.
     */
    public void recordAction(ActionType type, Object... data) {
        BlockPos pos = null;
        Entity entity = null;
        
        for (Object obj : data) {
            if (obj instanceof BlockPos) pos = (BlockPos) obj;
            if (obj instanceof Entity) entity = (Entity) obj;
        }
        
        PlayerAction action = new PlayerAction(type, pos, entity);
        recentActions.addLast(action);
        
        // Maintain window size
        cleanOldActions();
    }
    
    /**
     * Analyze recent actions and determine scenario.
     */
    public ScenarioAnalysis analyze() {
        if (recentActions.isEmpty()) {
            return ScenarioAnalysis.idle();
        }
        
        cleanOldActions();
        
        // Count action types
        Map<ActionType, Long> counts = recentActions.stream()
            .collect(Collectors.groupingBy(a -> a.type, Collectors.counting()));
        
        long attacks = counts.getOrDefault(ActionType.ATTACK_ENTITY, 0L);
        long blocks = counts.getOrDefault(ActionType.BLOCK_PLACE, 0L) + 
                     counts.getOrDefault(ActionType.BLOCK_BREAK, 0L);
        long movement = counts.getOrDefault(ActionType.JUMP, 0L) + 
                       counts.getOrDefault(ActionType.SPRINT, 0L);
        long inventory = counts.getOrDefault(ActionType.INVENTORY_OPEN, 0L);
        long cameraMovement = counts.getOrDefault(ActionType.CAMERA_MOVE, 0L);
        
        int totalActions = recentActions.size();
        
        // COMBAT: Lots of attacks + movement
        if (attacks > 10 && movement > 20) {
            double confidence = Math.min(0.95, (attacks + movement) / (double) totalActions);
            return ScenarioAnalysis.combat(confidence, attacks, movement);
        }
        
        // BUILDING: Many blocks, low movement
        if (blocks > 15 && movement < 10) {
            double confidence = Math.min(0.90, blocks / (double) totalActions);
            return ScenarioAnalysis.building(confidence, blocks);
        }
        
        // EXPLORING: High movement, low everything else
        if (movement > 30 && attacks < 3 && blocks < 3) {
            double confidence = Math.min(0.85, movement / (double) totalActions);
            return ScenarioAnalysis.exploring(confidence, movement);
        }
        
        // ORGANIZING: Inventory management
        if (inventory > 5 && movement < 5) {
            double confidence = Math.min(0.80, inventory / (double) totalActions);
            return ScenarioAnalysis.organizing(confidence, inventory);
        }
        
        // AFK: Very few actions
        if (totalActions < 5 && cameraMovement < 2) {
            long idleTime = System.currentTimeMillis() - 
                           (recentActions.isEmpty() ? 0 : recentActions.peekLast().timestamp);
            if (idleTime > 30000) { // 30 seconds
                double confidence = Math.min(0.95, idleTime / 60000.0); // grows with time
                return ScenarioAnalysis.afk(confidence, idleTime);
            }
        }
        
        // STANDARD: No clear pattern
        return ScenarioAnalysis.standard();
    }
    
    /**
     * Remove actions older than window size.
     */
    private void cleanOldActions() {
        long cutoffTime = System.currentTimeMillis() - 30000; // 30 seconds
        
        while (!recentActions.isEmpty() && 
               recentActions.peekFirst().timestamp < cutoffTime) {
            recentActions.pollFirst();
        }
    }
    
    /**
     * Get total action count in window.
     */
    public int getActionCount() {
        return recentActions.size();
    }
    
    /**
     * Clear all tracked actions.
     */
    public void clear() {
        recentActions.clear();
    }
    
    public static class ScenarioAnalysis {
        public final String scenarioType;
        public final double confidence;
        public final Map<String, Object> metrics;
        
        private ScenarioAnalysis(String type, double confidence, Map<String, Object> metrics) {
            this.scenarioType = type;
            this.confidence = confidence;
            this.metrics = metrics;
        }
        
        public static ScenarioAnalysis combat(double confidence, long attacks, long movement) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("attacks", attacks);
            metrics.put("movement", movement);
            return new ScenarioAnalysis("COMBAT", confidence, metrics);
        }
        
        public static ScenarioAnalysis building(double confidence, long blocks) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("blocks", blocks);
            return new ScenarioAnalysis("BUILDING", confidence, metrics);
        }
        
        public static ScenarioAnalysis exploring(double confidence, long movement) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("movement", movement);
            return new ScenarioAnalysis("EXPLORING", confidence, metrics);
        }
        
        public static ScenarioAnalysis organizing(double confidence, long inventory) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("inventory_actions", inventory);
            return new ScenarioAnalysis("ORGANIZING", confidence, metrics);
        }
        
        public static ScenarioAnalysis afk(double confidence, long idleTimeMs) {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("idle_time_ms", idleTimeMs);
            return new ScenarioAnalysis("AFK", confidence, metrics);
        }
        
        public static ScenarioAnalysis standard() {
            return new ScenarioAnalysis("STANDARD", 0.5, new HashMap<>());
        }
        
        public static ScenarioAnalysis idle() {
            return new ScenarioAnalysis("IDLE", 1.0, new HashMap<>());
        }
        
        public boolean isBuilding() {
            return "BUILDING".equals(scenarioType);
        }
        
        public boolean isExploring() {
            return "EXPLORING".equals(scenarioType);
        }
        
        public boolean isOrganizing() {
            return "ORGANIZING".equals(scenarioType);
        }
        
        public boolean isCombat() {
            return "COMBAT".equals(scenarioType);
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public int getTotalActions() {
            return metrics.values().stream()
                .filter(v -> v instanceof Long)
                .mapToInt(v -> ((Long) v).intValue())
                .sum();
        }
    }
}
