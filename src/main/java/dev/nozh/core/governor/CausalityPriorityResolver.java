package dev.nozh.core.governor;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.matrix.ActionCandidate;
import dev.nozh.core.profiler.SpikeCauseType;
import dev.nozh.core.profiler.SpikeCausalityReport;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class CausalityPriorityResolver {
    private static final EnumSet<CapabilityId> CPU_PRIORITY = EnumSet.of(
            CapabilityId.SIMULATION_DISTANCE,
            CapabilityId.ENTITY_DISTANCE,
            CapabilityId.ARMOR_STANDS,
            CapabilityId.ITEM_FRAMES,
            CapabilityId.BLOCK_ENTITIES,
            CapabilityId.ANIMATIONS,
            CapabilityId.PARTICLES,
            CapabilityId.CHUNK_LOADING);
    private static final EnumSet<CapabilityId> GPU_PRIORITY = EnumSet.of(
            CapabilityId.RENDER_DISTANCE,
            CapabilityId.RESOLUTION_SCALE,
            CapabilityId.DISTORTION_EFFECT_SCALE,
            CapabilityId.GRAPHICS_MODE,
            CapabilityId.SMOOTH_LIGHTING,
            CapabilityId.MIPMAP_LEVEL,
            CapabilityId.BIOME_BLEND,
            CapabilityId.FOG,
            CapabilityId.ENTITY_SHADOWS,
            CapabilityId.CLOUDS,
            CapabilityId.DYNAMIC_LIGHTING);
    private static final EnumSet<CapabilityId> GC_PRIORITY = EnumSet.of(
            CapabilityId.RENDER_DISTANCE,
            CapabilityId.SIMULATION_DISTANCE,
            CapabilityId.ENTITY_DISTANCE,
            CapabilityId.BIOME_BLEND,
            CapabilityId.MIPMAP_LEVEL,
            CapabilityId.BLOCK_ENTITIES);

    private CausalityPriorityResolver() {
    }

    public static String applyBound(String detectedBound, SpikeCausalityReport report) {
        if (report == null || report.cause() == null) {
            return detectedBound != null ? detectedBound : "BALANCED";
        }
        return switch (report.cause()) {
            case TICK, GC -> "CPU";
            case RENDER -> "GPU";
            case CRITICAL_EVENT -> "BALANCED";
            case FRAME, UNKNOWN -> detectedBound != null ? detectedBound : "BALANCED";
        };
    }

    public static void prioritizeCandidates(List<ActionCandidate> candidates, SpikeCausalityReport report) {
        if (candidates == null || candidates.size() < 2 || report == null || report.cause() == null) {
            return;
        }
        EnumSet<CapabilityId> prioritySet = resolvePrioritySet(report.cause());
        if (prioritySet.isEmpty()) {
            return;
        }
        candidates.sort((ActionCandidate left, ActionCandidate right) -> {
            boolean leftPreferred = prioritySet.contains(left.capabilityId());
            boolean rightPreferred = prioritySet.contains(right.capabilityId());
            return Boolean.compare(rightPreferred, leftPreferred);
        });
    }

    private static EnumSet<CapabilityId> resolvePrioritySet(SpikeCauseType cause) {
        if (cause == null) {
            return EnumSet.noneOf(CapabilityId.class);
        }
        return switch (cause) {
            case TICK -> CPU_PRIORITY;
            case RENDER -> GPU_PRIORITY;
            case GC -> GC_PRIORITY;
            case CRITICAL_EVENT, FRAME, UNKNOWN -> EnumSet.noneOf(CapabilityId.class);
        };
    }
}
