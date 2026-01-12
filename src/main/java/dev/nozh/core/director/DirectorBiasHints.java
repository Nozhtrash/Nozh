package dev.nozh.core.director;

public final class DirectorBiasHints {

    public final double cpuGpuBias;
    public final ModEcosystem ecosystem;

    public DirectorBiasHints(double cpuGpuBias, ModEcosystem ecosystem) {
        this.cpuGpuBias = clamp(cpuGpuBias);
        this.ecosystem = ecosystem == null ? ModEcosystem.UNKNOWN : ecosystem;
    }

    private static double clamp(double v) {
        if (!Double.isFinite(v)) return 0.0;
        if (v < -1.0) return -1.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
