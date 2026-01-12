package dev.nozh.core.priority3;

import dev.nozh.core.priority2.Priority2Suggestion;

/**
 * v0.3: Gain/Cost scoring.
 */
public final class EfficiencyScorer {

    public static final class Score {
        public final double expectedGainMs;
        public final double visualCost;
        public final double gameplayCost;
        public final double efficiency;
        public final double confidence01;
        public final double finalScore;

        public Score(double expectedGainMs, double visualCost, double gameplayCost, double confidence01) {
            this.expectedGainMs = sanitize(expectedGainMs);
            this.visualCost = sanitize(visualCost);
            this.gameplayCost = sanitize(gameplayCost);
            this.confidence01 = clamp01(confidence01);
            this.efficiency = this.expectedGainMs / (this.visualCost + this.gameplayCost + 0.1);
            this.finalScore = this.efficiency * this.confidence01;
        }

        private static double sanitize(double v) {
            if (!Double.isFinite(v) || v < 0.0) return 0.0;
            return v;
        }

        private static double clamp01(double v) {
            if (!Double.isFinite(v)) return 0.0;
            if (v < 0.0) return 0.0;
            if (v > 1.0) return 1.0;
            return v;
        }
    }

    /**
     * Best-effort scoring based on suggestion ID.
     */
    public Score score(Priority2Suggestion s, double confidence01) {
        if (s == null || s.id == null) {
            return new Score(0.0, 10.0, 10.0, confidence01);
        }

        return switch (s.id) {
            // Reducing particles is usually cheap visually/gameplay.
            case "gpu.reduce_particles" -> new Score(6.0, 1.5, 0.5, confidence01);
            // Generic shader mitigation is more visually costly.
            case "gpu.reduce_shaders" -> new Score(10.0, 4.0, 0.5, confidence01);
            // CPU entity reductions can affect gameplay/visibility.
            case "cpu.reduce_entities" -> new Score(9.0, 2.0, 3.0, confidence01);
            // Combat stabilize tries to keep playability, moderate cost.
            case "scenario.combat_stabilize" -> new Score(7.0, 2.5, 1.0, confidence01);
            default -> new Score(4.0, 3.0, 3.0, confidence01);
        };
    }
}
