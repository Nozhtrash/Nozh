package dev.nozh.core.state;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Baseline snapshot of player settings with no-excess rules.
 *
 * Defines the "initial player values" and prevents over-restoring beyond the baseline.
 */
public record BaselineSnapshot(Map<CapabilityId, CapabilityValue> values) {

    public static BaselineSnapshot empty() {
        return new BaselineSnapshot(Map.of());
    }

    public boolean isEmpty() {
        return values == null || values.isEmpty();
    }

    public Optional<CapabilityValue> get(CapabilityId id) {
        if (values == null || id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(id));
    }

    public boolean exceedsBaseline(CapabilityId id, CapabilityValue candidate) {
        if (id == null || candidate == null) {
            return false;
        }
        CapabilityValue baseline = values != null ? values.get(id) : null;
        if (baseline == null) {
            return false;
        }
        return switch (id) {
            case PARTICLES -> compareEnum(candidate, baseline, List.of("MINIMAL", "DECREASED", "ALL"));
            case CLOUDS -> compareEnum(candidate, baseline, List.of("OFF", "FAST", "FANCY"));
            case GRAPHICS_MODE -> compareEnum(candidate, baseline, List.of("FAST", "FANCY", "FABULOUS"));
            case ENTITY_SHADOWS, ARMOR_STANDS, ITEM_FRAMES, BLOCK_ENTITIES, ANIMATIONS, VSYNC, DYNAMIC_LIGHTING ->
                    compareBool(candidate, baseline);
            case RENDER_DISTANCE, SIMULATION_DISTANCE, ENTITY_DISTANCE, BIOME_BLEND, MIPMAP_LEVEL, FOG ->
                    compareInt(candidate, baseline);
            case RESOLUTION_SCALE, DISTORTION_EFFECT_SCALE -> compareFloat(candidate, baseline);
            default -> false;
        };
    }

    public CapabilityValue clampToBaseline(CapabilityId id, CapabilityValue candidate) {
        if (id == null || candidate == null) {
            return candidate;
        }
        CapabilityValue baseline = values != null ? values.get(id) : null;
        if (baseline == null) {
            return candidate;
        }
        return exceedsBaseline(id, candidate) ? baseline : candidate;
    }

    private boolean compareEnum(CapabilityValue candidate, CapabilityValue baseline, List<String> ordering) {
        if (!(candidate instanceof CapabilityValue.EnumValue candidateEnum)
                || !(baseline instanceof CapabilityValue.EnumValue baselineEnum)) {
            return false;
        }
        int candidateIndex = ordering.indexOf(candidateEnum.name());
        int baselineIndex = ordering.indexOf(baselineEnum.name());
        if (candidateIndex < 0 || baselineIndex < 0) {
            return false;
        }
        return candidateIndex > baselineIndex;
    }

    private boolean compareBool(CapabilityValue candidate, CapabilityValue baseline) {
        if (!(candidate instanceof CapabilityValue.BoolValue candidateBool)
                || !(baseline instanceof CapabilityValue.BoolValue baselineBool)) {
            return false;
        }
        return candidateBool.value() && !baselineBool.value();
    }

    private boolean compareInt(CapabilityValue candidate, CapabilityValue baseline) {
        if (!(candidate instanceof CapabilityValue.IntValue candidateInt)
                || !(baseline instanceof CapabilityValue.IntValue baselineInt)) {
            return false;
        }
        return candidateInt.value() > baselineInt.value();
    }

    private boolean compareFloat(CapabilityValue candidate, CapabilityValue baseline) {
        if (!(candidate instanceof CapabilityValue.FloatValue candidateFloat)
                || !(baseline instanceof CapabilityValue.FloatValue baselineFloat)) {
            return false;
        }
        return candidateFloat.value() > baselineFloat.value();
    }
}
