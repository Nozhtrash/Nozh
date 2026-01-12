package dev.nozh.core.priority2;

import dev.nozh.core.director.DirectorBiasHints;
import dev.nozh.core.scenario.DeepScenarioSnapshot;
import dev.nozh.core.system.BottleneckKind;
import dev.nozh.core.system.CpuGpuBottleneckClassifier;

/**
 * v0.2: Big-step suggestion engine.
 *
 * <p>This does not apply changes directly. It produces suggestions that can be:
 * - queued in manual mode
 * - displayed in HUD
 * - later mapped to real capabilities/actions by the governor
 */
public final class Priority2SuggestionEngine {

    // Suggestion IDs are stable strings so they can be mapped later to real capabilities.
    public static final String SUGGEST_CPU_REDUCE_ENTITIES = "cpu.reduce_entities";
    public static final String SUGGEST_GPU_REDUCE_SHADERS = "gpu.reduce_shaders";
    public static final String SUGGEST_GPU_REDUCE_PARTICLES = "gpu.reduce_particles";
    public static final String SUGGEST_COMBAT_STABILIZE = "scenario.combat_stabilize";

    public Priority2Suggestion compute(
            DeepScenarioSnapshot scenario,
            CpuGpuBottleneckClassifier.Result bottleneck,
            DirectorBiasHints directorHints
    ) {
        if (bottleneck == null || bottleneck.kind == BottleneckKind.UNKNOWN) {
            return null;
        }

        double conf = clamp01(bottleneck.confidence01);
        if (conf < 0.55) {
            return null;
        }

        // Combat heuristic: many hostiles nearby.
        if (scenario != null && scenario.hostileMobsNearby >= 12) {
            return new Priority2Suggestion(
                    SUGGEST_COMBAT_STABILIZE,
                    "Many hostiles nearby (" + scenario.hostileMobsNearby + "). Prefer stability actions (particles/shadows/entities).",
                    Priority2Suggestion.Severity.URGENT
            );
        }

        // Bias adjustment: directorHints slightly nudges the decision.
        double bias = directorHints == null ? 0.0 : directorHints.cpuGpuBias;

        if (bottleneck.kind == BottleneckKind.CPU && bias <= 0.5) {
            return new Priority2Suggestion(
                    SUGGEST_CPU_REDUCE_ENTITIES,
                    "CPU-bound signal (conf=" + fmt(conf) + "). Consider reducing entity-related load (distance, AI-heavy mobs).",
                    Priority2Suggestion.Severity.RECOMMENDED
            );
        }

        if (bottleneck.kind == BottleneckKind.GPU || (bottleneck.kind == BottleneckKind.MIXED && bias > 0.1)) {
            return new Priority2Suggestion(
                    SUGGEST_GPU_REDUCE_PARTICLES,
                    "GPU-bound signal (conf=" + fmt(conf) + "). Consider reducing particles/shader cost.",
                    Priority2Suggestion.Severity.RECOMMENDED
            );
        }

        return null;
    }

    private static double clamp01(double v) {
        if (!Double.isFinite(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}
