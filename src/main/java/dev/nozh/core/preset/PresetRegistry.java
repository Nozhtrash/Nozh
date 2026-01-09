package dev.nozh.core.preset;

import dev.nozh.core.capability.CapabilityId;

import java.util.Map;
import java.util.Set;

/**
 * Preset registry (Contract 10).
 * 
 * Static mapping of tiers to constraints.
 * NO reflection, NO dynamic loading.
 */
public final class PresetRegistry {

    private static final Map<HardwareTier, PresetConstraints> PRESETS = Map.of(
            HardwareTier.CAFETERA, new PresetConstraints(
                    4, // maxRenderDistance
                    false, // shadersAllowed
                    30, // maxFpsCap
                    Set.of(CapabilityId.PARTICLES, CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS)),

            HardwareTier.LOW, new PresetConstraints(
                    8,
                    false,
                    60,
                    Set.of(CapabilityId.PARTICLES, CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS)),

            HardwareTier.MEDIUM, new PresetConstraints(
                    12,
                    true,
                    120,
                    Set.of(CapabilityId.PARTICLES, CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS,
                            CapabilityId.FPS_CAP)),

            HardwareTier.HIGH, new PresetConstraints(
                    16,
                    true,
                    144,
                    Set.of(CapabilityId.PARTICLES, CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS,
                            CapabilityId.FPS_CAP)),

            HardwareTier.EXTREME, new PresetConstraints(
                    24,
                    true,
                    240,
                    Set.of(CapabilityId.PARTICLES, CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS,
                            CapabilityId.FPS_CAP)),

            HardwareTier.NASA, new PresetConstraints(
                    32,
                    true,
                    Integer.MAX_VALUE,
                    Set.of(CapabilityId.PARTICLES, CapabilityId.CLOUDS, CapabilityId.ENTITY_SHADOWS,
                            CapabilityId.FPS_CAP)));

    /**
     * Get constraints for a tier.
     * 
     * @param tier Hardware tier
     * @return Constraints for that tier
     * @throws IllegalArgumentException if tier is null or not found
     */
    public static PresetConstraints get(HardwareTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException("Tier cannot be null");
        }

        PresetConstraints constraints = PRESETS.get(tier);
        if (constraints == null) {
            throw new IllegalArgumentException("No preset for tier: " + tier);
        }

        return constraints;
    }

    private PresetRegistry() {
        // Static utility class
    }
}
