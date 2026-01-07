package dev.nozh.core.profiler;

public record SpikeCausalityReport(
        SpikeCauseType cause,
        double confidence,
        String detail) {

    public static SpikeCausalityReport unknown() {
        return new SpikeCausalityReport(SpikeCauseType.UNKNOWN, 0.0, "");
    }
}
