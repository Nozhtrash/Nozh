package dev.nozh.core.compat;

import dev.nozh.core.capability.CapabilityId;
import java.util.*;

/**
 * Resolves compatibility conflicts using intelligent strategies.
 */
public final class CompatConflictResolver {
    
    public ResolutionStrategy resolveConflict(
        CapabilityId capability,
        String primarySteward,
        String secondarySteward
    ) {
        // Priority order for known mods
        var priorityMap = buildPriorityMap();
        
        int primaryPriority = priorityMap.getOrDefault(primarySteward, 50);
        int secondarPriority = priorityMap.getOrDefault(secondarySteward, 50);
        
        if (primaryPriority > secondarPriority) {
            return new ResolutionStrategy(
                ResolutionType.DEFER_TO_PRIMARY,
                primarySteward,
                "Higher priority mod (" + primarySteward + ") takes precedence"
            );
        } else if (secondarPriority > primaryPriority) {
            return new ResolutionStrategy(
                ResolutionType.DEFER_TO_SECONDARY,
                secondarySteward,
                "Higher priority mod (" + secondarySteward + ") takes precedence"
            );
        } else {
            return new ResolutionStrategy(
                ResolutionType.COORDINATE,
                null,
                "Both mods have equal priority, NOZH will coordinate changes"
            );
        }
    }
    
    private Map<String, Integer> buildPriorityMap() {
        var map = new HashMap<String, Integer>();
        
        // Higher number = higher priority
        // Performance mods (highest priority)
        map.put("sodium", 100);
        map.put("iris", 95);
        map.put("lithium", 90);
        map.put("starlight", 85);
        
        // Visual mods (medium priority)
        map.put("lambdynamiclights", 60);
        map.put("lambdabettergrass", 55);
        
        // OptiFine-based (lower priority, deprecated)
        map.put("optifabric", 40);
        
        // NOZH itself (can override if needed)
        map.put("nozh", 50);
        
        return map;
    }
    
    public List<RecommendedAction> generateRecommendations(CompatMatrix.CompatAnalysis analysis) {
        var actions = new ArrayList<RecommendedAction>();
        
        for (var conflict : analysis.conflicts()) {
            switch (conflict.severity()) {
                case CRITICAL -> actions.add(new RecommendedAction(
                    ActionPriority.CRITICAL,
                    "Remove " + conflict.mod2() + " (conflicts with " + conflict.mod1() + ")",
                    conflict.resolution()
                ));
                case HIGH -> actions.add(new RecommendedAction(
                    ActionPriority.HIGH,
                    "Review conflict: " + conflict.description(),
                    conflict.resolution()
                ));
                case MEDIUM -> actions.add(new RecommendedAction(
                    ActionPriority.MEDIUM,
                    "Monitor: " + conflict.description(),
                    conflict.resolution()
                ));
                case LOW -> actions.add(new RecommendedAction(
                    ActionPriority.LOW,
                    "Note: " + conflict.description(),
                    "No action required"
                ));
            }
        }
        
        return actions;
    }
    
    public record ResolutionStrategy(
        ResolutionType type,
        String preferredSteward,
        String rationale
    ) {}
    
    public record RecommendedAction(
        ActionPriority priority,
        String description,
        String resolution
    ) {}
    
    public enum ResolutionType {
        DEFER_TO_PRIMARY,
        DEFER_TO_SECONDARY,
        COORDINATE,
        DISABLE_CAPABILITY
    }
    
    public enum ActionPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}