package dev.nozh.core.analytics;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.capability.CapabilityLevel;
import dev.nozh.core.state.GameplayScenario;

import java.util.HashMap;
import java.util.Map;

/**
 * PRIORITY 3: Gain/cost efficiency scoring.
 * 
 * Provides mathematical scoring for each optimization action:
 * - Performance gain (expected FPS improvement)
 * - Quality cost (visual/gameplay impact)
 * - Efficiency ratio (gain/cost)
 * 
 * Higher efficiency = better optimization choice.
 */
public final class CapabilityMetrics {

    private static final Map<CapabilityId, MetricData> METRICS = new HashMap<>();

    static {
        // Format: (avgFpsGain, qualityCost, cpuImpact, gpuImpact)
        
        // High gain, low cost (BEST)
        METRICS.put(CapabilityId.PARTICLES, new MetricData(12.0, 2.0, 0.6, 0.4));
        METRICS.put(CapabilityId.ENTITY_DISTANCE, new MetricData(15.0, 3.0, 0.7, 0.3));
        METRICS.put(CapabilityId.ARMOR_STANDS, new MetricData(8.0, 1.0, 0.5, 0.5));
        METRICS.put(CapabilityId.ITEM_FRAMES, new MetricData(7.0, 1.5, 0.4, 0.6));
        
        // Medium gain, medium cost
        METRICS.put(CapabilityId.RENDER_DISTANCE, new MetricData(20.0, 5.0, 0.3, 0.7));
        METRICS.put(CapabilityId.BLOCK_ENTITIES, new MetricData(10.0, 3.0, 0.5, 0.5));
        METRICS.put(CapabilityId.CHUNK_UPDATES, new MetricData(8.0, 2.0, 0.6, 0.4));
        METRICS.put(CapabilityId.ENTITY_RENDER, new MetricData(12.0, 4.0, 0.4, 0.6));
        
        // Low cost, situational gain
        METRICS.put(CapabilityId.FPS_CAP, new MetricData(5.0, 0.5, 0.8, 0.2));
        METRICS.put(CapabilityId.VSYNC, new MetricData(3.0, 1.0, 0.7, 0.3));
        METRICS.put(CapabilityId.CLOUDS, new MetricData(4.0, 1.0, 0.2, 0.8));
        METRICS.put(CapabilityId.VIGNETTE, new MetricData(2.0, 0.5, 0.1, 0.9));
        
        // High gain, higher cost
        METRICS.put(CapabilityId.SIMULATION_DISTANCE, new MetricData(18.0, 6.0, 0.8, 0.2));
        METRICS.put(CapabilityId.ENTITY_AI, new MetricData(25.0, 7.0, 0.9, 0.1));
        METRICS.put(CapabilityId.LIGHTING_QUALITY, new MetricData(15.0, 5.0, 0.3, 0.7));
        
        // Specialized
        METRICS.put(CapabilityId.FIRE_RENDER, new MetricData(6.0, 2.0, 0.3, 0.7));
        METRICS.put(CapabilityId.BLOCK_ANIMATIONS, new MetricData(5.0, 2.5, 0.4, 0.6));
        METRICS.put(CapabilityId.ENTITY_SHADOWS, new MetricData(8.0, 3.0, 0.2, 0.8));
        METRICS.put(CapabilityId.BIOME_BLEND, new MetricData(7.0, 3.5, 0.3, 0.7));
    }

    /**
     * Calculate efficiency score for a capability.
     * Higher = better optimization choice.
     */
    public static double calculateEfficiency(
            CapabilityId capability,
            GameplayScenario scenario,
            String bottleneck) {
        
        MetricData data = METRICS.get(capability);
        if (data == null) {
            return 0.5; // Default medium efficiency
        }

        // Base efficiency: gain / cost
        double baseEfficiency = data.avgFpsGain / Math.max(data.qualityCost, 0.1);

        // Apply scenario multipliers
        double scenarioMultiplier = getScenarioMultiplier(capability, scenario);
        
        // Apply bottleneck multipliers
        double bottleneckMultiplier = getBottleneckMultiplier(capability, bottleneck, data);

        double finalScore = baseEfficiency * scenarioMultiplier * bottleneckMultiplier;

        // Normalize to 0-10 scale
        return Math.min(finalScore / 2.0, 10.0);
    }

    private static double getScenarioMultiplier(CapabilityId capability, GameplayScenario scenario) {
        return switch (scenario) {
            case COMBAT -> {
                // Prioritize responsiveness over visuals
                if (capability == CapabilityId.PARTICLES) yield 1.5;
                if (capability == CapabilityId.ENTITY_DISTANCE) yield 0.8; // Keep visible
                if (capability == CapabilityId.ENTITY_AI) yield 0.7; // Keep AI
                if (capability == CapabilityId.ENTITY_RENDER) yield 0.9;
                yield 1.0;
            }
            case BUILDING -> {
                // Keep render quality high
                if (capability == CapabilityId.RENDER_DISTANCE) yield 0.6;
                if (capability == CapabilityId.BLOCK_ENTITIES) yield 0.8;
                if (capability == CapabilityId.LIGHTING_QUALITY) yield 0.7;
                if (capability == CapabilityId.ENTITY_AI) yield 1.3; // Can reduce
                if (capability == CapabilityId.PARTICLES) yield 1.2;
                yield 1.0;
            }
            case AFK -> {
                // Aggressive optimizations OK
                if (capability == CapabilityId.FPS_CAP) yield 2.0;
                if (capability == CapabilityId.ENTITY_AI) yield 1.5;
                if (capability == CapabilityId.RENDER_DISTANCE) yield 1.4;
                yield 1.3;
            }
            case EXPLORING -> {
                // Keep render distance, reduce entities
                if (capability == CapabilityId.RENDER_DISTANCE) yield 0.5;
                if (capability == CapabilityId.ENTITY_AI) yield 1.2;
                if (capability == CapabilityId.ENTITY_DISTANCE) yield 1.3;
                yield 1.0;
            }
            case MENU -> {
                // Maximum optimization
                yield 2.0;
            }
            case LOADING -> {
                // Reduce load
                if (capability == CapabilityId.CHUNK_UPDATES) yield 0.8;
                yield 1.1;
            }
        };
    }

    private static double getBottleneckMultiplier(
            CapabilityId capability,
            String bottleneck,
            MetricData data) {
        
        if (bottleneck == null || bottleneck.equals("BALANCED")) {
            return 1.0;
        }

        if (bottleneck.equals("CPU")) {
            // Prioritize CPU-heavy optimizations
            return 0.5 + (data.cpuImpact * 1.5);
        }

        if (bottleneck.equals("GPU")) {
            // Prioritize GPU-heavy optimizations
            return 0.5 + (data.gpuImpact * 1.5);
        }

        return 1.0;
    }

    /**
     * Get expected FPS gain for a capability.
     */
    public static double getExpectedGain(CapabilityId capability) {
        MetricData data = METRICS.get(capability);
        return data != null ? data.avgFpsGain : 5.0;
    }

    /**
     * Get quality cost (1-10 scale, lower = less noticeable).
     */
    public static double getQualityCost(CapabilityId capability) {
        MetricData data = METRICS.get(capability);
        return data != null ? data.qualityCost : 3.0;
    }

    /**
     * Check if capability is primarily CPU-bound.
     */
    public static boolean isCpuBound(CapabilityId capability) {
        MetricData data = METRICS.get(capability);
        return data != null && data.cpuImpact > 0.6;
    }

    /**
     * Check if capability is primarily GPU-bound.
     */
    public static boolean isGpuBound(CapabilityId capability) {
        MetricData data = METRICS.get(capability);
        return data != null && data.gpuImpact > 0.6;
    }

    /**
     * Get top N most efficient capabilities for current context.
     */
    public static java.util.List<CapabilityId> getTopEfficient(
            int count,
            GameplayScenario scenario,
            String bottleneck) {
        
        return METRICS.keySet().stream()
            .sorted((a, b) -> Double.compare(
                calculateEfficiency(b, scenario, bottleneck),
                calculateEfficiency(a, scenario, bottleneck)
            ))
            .limit(count)
            .toList();
    }

    /**
     * Get detailed report for a capability.
     */
    public static String getReport(
            CapabilityId capability,
            GameplayScenario scenario,
            String bottleneck) {
        
        MetricData data = METRICS.get(capability);
        if (data == null) {
            return "No data available";
        }

        double efficiency = calculateEfficiency(capability, scenario, bottleneck);
        
        return String.format(
            "%s: Gain=%.1f FPS, Cost=%.1f, Efficiency=%.2f, CPU=%.0f%%, GPU=%.0f%%",
            capability.name(),
            data.avgFpsGain,
            data.qualityCost,
            efficiency,
            data.cpuImpact * 100,
            data.gpuImpact * 100
        );
    }

    /**
     * Metric data for a capability.
     */
    private record MetricData(
        double avgFpsGain,      // Expected FPS improvement
        double qualityCost,     // Visual/gameplay impact (1-10)
        double cpuImpact,       // 0-1: CPU load reduction
        double gpuImpact        // 0-1: GPU load reduction
    ) {}
}
