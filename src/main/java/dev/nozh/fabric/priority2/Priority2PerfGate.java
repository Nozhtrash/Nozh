package dev.nozh.fabric.priority2;

import dev.nozh.core.system.BottleneckKind;
import dev.nozh.core.system.CpuGpuBottleneckClassifier;

/**
 * v0.2/v0.3 hybrid: minimal performance gate for gradual recovery.
 */
final class Priority2PerfGate {

    private Priority2PerfGate() {
    }

    static boolean performanceVeryGood(double tickMs, double renderMs, CpuGpuBottleneckClassifier.Result bottleneck) {
        // Very conservative thresholds: avoid changing quality unless we are clearly stable.
        double total = Math.max(0.0, safe(tickMs) + safe(renderMs));

        // Under ~12ms total suggests >80 FPS potential, stable enough to try recovery.
        if (total <= 12.0) {
            // If classifier thinks we're bottlenecked with high confidence, don't recover.
            if (bottleneck != null && bottleneck.kind != BottleneckKind.UNKNOWN && bottleneck.confidence01 >= 0.75) {
                return false;
            }
            return true;
        }

        return false;
    }

    private static double safe(double ms) {
        if (!Double.isFinite(ms) || ms < 0.0) return 0.0;
        if (ms > 60_000.0) return 60_000.0;
        return ms;
    }
}
