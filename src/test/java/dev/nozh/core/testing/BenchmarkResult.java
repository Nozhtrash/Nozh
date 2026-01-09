package dev.nozh.core.testing;

public final class BenchmarkResult {
    private final String scenarioId;
    private final String scenarioName;
    private final double averageMs;
    private final double p95Ms;
    private final Double p99Ms;
    private final long spikeCount;
    private final int sampleCount;
    private final String notes;

    public BenchmarkResult(
        String scenarioId,
        String scenarioName,
        double averageMs,
        double p95Ms,
        Double p99Ms,
        long spikeCount,
        int sampleCount,
        String notes
    ) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.averageMs = averageMs;
        this.p95Ms = p95Ms;
        this.p99Ms = p99Ms;
        this.spikeCount = spikeCount;
        this.sampleCount = sampleCount;
        this.notes = notes;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public double getAverageMs() {
        return averageMs;
    }

    public double getP95Ms() {
        return p95Ms;
    }

    public Double getP99Ms() {
        return p99Ms;
    }

    public long getSpikeCount() {
        return spikeCount;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public String getNotes() {
        return notes;
    }
}
