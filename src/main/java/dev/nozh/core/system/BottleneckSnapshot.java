package dev.nozh.core.system;

import java.util.Objects;

/**
 * Snapshot used for CPU/GPU bottleneck inference.
 */
public final class BottleneckSnapshot {

    public final double tickMs;
    public final double renderMs;

    public final double processCpuLoad01; // -1 if unavailable
    public final double systemCpuLoad01;  // -1 if unavailable

    public final boolean shadersEnabled;
    public final int visibleEntityCount;

    public BottleneckSnapshot(
            double tickMs,
            double renderMs,
            double processCpuLoad01,
            double systemCpuLoad01,
            boolean shadersEnabled,
            int visibleEntityCount
    ) {
        this.tickMs = tickMs;
        this.renderMs = renderMs;
        this.processCpuLoad01 = processCpuLoad01;
        this.systemCpuLoad01 = systemCpuLoad01;
        this.shadersEnabled = shadersEnabled;
        this.visibleEntityCount = visibleEntityCount;
    }

    @Override
    public String toString() {
        return "BottleneckSnapshot{" +
                "tickMs=" + tickMs +
                ", renderMs=" + renderMs +
                ", processCpuLoad01=" + processCpuLoad01 +
                ", systemCpuLoad01=" + systemCpuLoad01 +
                ", shadersEnabled=" + shadersEnabled +
                ", visibleEntityCount=" + visibleEntityCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BottleneckSnapshot that)) return false;
        return Double.compare(tickMs, that.tickMs) == 0
                && Double.compare(renderMs, that.renderMs) == 0
                && Double.compare(processCpuLoad01, that.processCpuLoad01) == 0
                && Double.compare(systemCpuLoad01, that.systemCpuLoad01) == 0
                && shadersEnabled == that.shadersEnabled
                && visibleEntityCount == that.visibleEntityCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tickMs, renderMs, processCpuLoad01, systemCpuLoad01, shadersEnabled, visibleEntityCount);
    }
}
