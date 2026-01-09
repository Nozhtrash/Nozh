package dev.nozh.core.learning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages learning data with time-based decay.
 * 
 * ROADMAP: Phase 2, Sprint 4 - Learning Data Decay
 * 
 * Implements TTL and time-weighted scoring for action effectiveness.
 */
public class LearningDataDecay {
    
    private static final long ENTRY_TTL_MS = 7L * 24 * 60 * 60 * 1000; // 7 days
    private static final double TIME_DECAY_FACTOR = 0.95; // Per day
    private static final int MAX_ENTRIES = 1000;
    
    private final List<ActionRecord> actionHistory = new ArrayList<>();
    
    /**
     * Action execution record.
     */
    public static class ActionRecord {
        public final String actionId;
        public final double fpsImprovement;
        public final boolean success;
        public final long timestamp;
        public final String hardwareFingerprint;
        
        public ActionRecord(String id, double fps, boolean success, String hw) {
            this.actionId = id;
            this.fpsImprovement = fps;
            this.success = success;
            this.timestamp = System.currentTimeMillis();
            this.hardwareFingerprint = hw;
        }
        
        /**
         * Get weighted score with time decay.
         */
        public double getWeightedScore() {
            long age = System.currentTimeMillis() - timestamp;
            long daysOld = age / (24 * 60 * 60 * 1000);
            
            // Exponential decay
            double decayMultiplier = Math.pow(TIME_DECAY_FACTOR, daysOld);
            
            return fpsImprovement * decayMultiplier;
        }
        
        /**
         * Check if record has expired.
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ENTRY_TTL_MS;
        }
    }
    
    /**
     * Record action result.
     */
    public void recordAction(String actionId, double fpsImprovement, 
                            boolean success, String hardwareProfile) {
        ActionRecord record = new ActionRecord(
            actionId, fpsImprovement, success, hardwareProfile
        );
        actionHistory.add(record);
    }
    
    /**
     * Clean up old entries.
     */
    public void cleanup() {
        // Remove expired
        actionHistory.removeIf(ActionRecord::isExpired);
        
        // Limit size
        if (actionHistory.size() > MAX_ENTRIES) {
            // Keep most recent
            actionHistory.sort(Comparator.comparing(r -> -r.timestamp));
            while (actionHistory.size() > MAX_ENTRIES) {
                actionHistory.remove(actionHistory.size() - 1);
            }
        }
    }
    
    /**
     * Get effectiveness score for action.
     */
    public double getEffectivenessScore(String actionId) {
        return actionHistory.stream()
            .filter(r -> r.actionId.equals(actionId))
            .mapToDouble(ActionRecord::getWeightedScore)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Get history size.
     */
    public int getHistorySize() {
        return actionHistory.size();
    }
    
    /**
     * Clear all history.
     */
    public void clear() {
        actionHistory.clear();
    }
}