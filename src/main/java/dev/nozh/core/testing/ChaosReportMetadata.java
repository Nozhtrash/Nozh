package dev.nozh.core.testing;

/**
 * Chaos test report metadata for correlating benchmark results.
 */
public record ChaosReportMetadata(
        int renderDistance,
        int simulationDistance,
        String shaderPack) {
}
