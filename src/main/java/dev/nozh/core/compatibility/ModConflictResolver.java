package dev.nozh.core.compatibility;

import dev.nozh.NozhConstants;
import net.fabricmc.loader.api.FabricLoader;

import java.util.*;

/**
 * Intelligent system to detect and resolve mod conflicts.
 * Prevents "stepping on toes" and reduces log noise from false negatives.
 * 
 * Phase 5: Smart Core
 */
public final class ModConflictResolver {

    public enum ConflictSeverity {
        WARNING, // Redundant but harmless
        ERROR, // Functional break
        FATAL // Crash risk
    }

    public record Conflict(String modA, String modB, ConflictSeverity severity, String message, String recommendation) {
    }

    private static final List<ConflictRule> RULES = new ArrayList<>();

    static {
        // Define knowledge base rules
        RULES.add(new ConflictRule(Set.of("sodium", "optifabric"), ConflictSeverity.FATAL,
                "nozh.conflict.sodium_optifabric.message", "nozh.conflict.sodium_optifabric.rec"));
        RULES.add(new ConflictRule(Set.of("sodium", "canvas"), ConflictSeverity.FATAL,
                "nozh.conflict.sodium_canvas.message", "nozh.conflict.sodium_canvas.rec"));
        RULES.add(new ConflictRule(Set.of("iris", "optifabric"), ConflictSeverity.FATAL,
                "nozh.conflict.iris_optifabric.message", "nozh.conflict.iris_optifabric.rec"));
        RULES.add(new ConflictRule(Set.of("cullleaves", "moreculling"), ConflictSeverity.WARNING,
                "nozh.conflict.redundant_culling.message",
                "nozh.conflict.redundant_culling.rec"));
        RULES.add(new ConflictRule(Set.of("enhancedblockentities", "indium"), ConflictSeverity.WARNING,
                "nozh.conflict.ebe_indium.message",
                "nozh.conflict.ebe_indium.rec"));
    }

    public static List<Conflict> analyze() {
        List<Conflict> conflicts = new ArrayList<>();
        FabricLoader loader = FabricLoader.getInstance();

        for (ConflictRule rule : RULES) {
            List<String> presentMods = rule.mods.stream()
                    .filter(loader::isModLoaded)
                    .toList();

            if (presentMods.size() == rule.mods.size()) {
                // All mods in this conflict rule are present
                String modA = presentMods.get(0);
                String modB = presentMods.size() > 1 ? presentMods.get(1) : "unknown"; // Should always be >1 for
                                                                                       // defined rules

                conflicts.add(new Conflict(modA, modB, rule.severity, rule.message, rule.recommendation));
            }
        }

        // Auto-orchestration logging
        if (!conflicts.isEmpty()) {
            NozhConstants.LOGGER.info("=== capabilities conflict analysis ===");
            for (Conflict c : conflicts) {
                if (c.severity == ConflictSeverity.FATAL) {
                    NozhConstants.LOGGER.error("[Conflict] {} + {}: {}", c.modA, c.modB, c.message);
                } else {
                    NozhConstants.LOGGER.warn("[Redundancy] {} + {}: {}", c.modA, c.modB, c.message);
                }
            }
        }

        return conflicts;
    }

    private record ConflictRule(Set<String> mods, ConflictSeverity severity, String message, String recommendation) {
    }

    private ModConflictResolver() {
    }
}
