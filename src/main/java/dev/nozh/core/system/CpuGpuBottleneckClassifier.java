package dev.nozh.core.system;

import java.util.Objects;

/**
 * Heuristic bottleneck classifier aimed at the v0.2 roadmap.
 *
 * <p>Design goals:
 * <ul>
 *   <li>Deterministic, testable, and cheap to run.</li>
 *   <li>Works even when OS load metrics are unavailable.</li>
 *   <li>Exposes confidence to support governor decisions.</li>
 * </ul>
 */
public final class CpuGpuBottleneckClassifier {

    public static final class Result {
        public final BottleneckKind kind;
        public final double confidence01;

        public Result(BottleneckKind kind, double confidence01) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.confidence01 = clamp01(confidence01);
        }

        private static double clamp01(double v) {
            if (!Double.isFinite(v)) return 0.0;
            if (v < 0.0) return 0.0;
            if (v > 1.0) return 1.0;
            return v;
        }
    }

    /**
     * Primary heuristic: compare tick vs render time, corrected by CPU load.
     */
    public Result classify(BottleneckSnapshot s) {
        if (s == null) {
            return new Result(BottleneckKind.UNKNOWN, 0.0);
        }

        double tick = saneMs(s.tickMs);
        double render = saneMs(s.renderMs);

        if (tick <= 0.0 && render <= 0.0) {
            return new Result(BottleneckKind.UNKNOWN, 0.0);
        }

        // Ratio-based inference.
        double total = Math.max(0.001, tick + render);
        double tickShare = tick / total;
        double renderShare = render / total;

        // CPU load helps reduce false positives: if CPU load is clearly low, avoid calling CPU bottleneck.
        double cpuLoad = pickCpuLoad01(s);

        // Basic thresholds tuned for stability rather than aggressiveness.
        if (tickShare >= 0.62) {
            double conf = 0.55 + (tickShare - 0.62) * 1.3;
            if (cpuLoad >= 0.0) {
                conf *= (0.75 + 0.25 * cpuLoad);
            }
            return new Result(BottleneckKind.CPU, conf);
        }

        if (renderShare >= 0.62) {
            double conf = 0.55 + (renderShare - 0.62) * 1.3;
            // If shaders are enabled, slightly increase GPU confidence.
            if (s.shadersEnabled) {
                conf = Math.min(1.0, conf + 0.08);
            }
            // Many entities often correlates with render bottlenecks.
            if (s.visibleEntityCount >= 250) {
                conf = Math.min(1.0, conf + 0.06);
            }
            return new Result(BottleneckKind.GPU, conf);
        }

        // Mixed when neither dominates.
        double conf = 0.45 + Math.abs(tickShare - 0.5) * 0.6;
        return new Result(BottleneckKind.MIXED, conf);
    }

    private static double saneMs(double ms) {
        if (!Double.isFinite(ms) || ms < 0.0) return 0.0;
        if (ms > 60_000.0) return 60_000.0;
        return ms;
    }

    private static double pickCpuLoad01(BottleneckSnapshot s) {
        if (s.processCpuLoad01 >= 0.0 && s.processCpuLoad01 <= 1.0) return s.processCpuLoad01;
        if (s.systemCpuLoad01 >= 0.0 && s.systemCpuLoad01 <= 1.0) return s.systemCpuLoad01;
        return -1.0;
    }
}
