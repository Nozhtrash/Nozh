package dev.nozh.core.executor.handlers;

import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.capability.ApplyResult;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

import java.util.Optional;

/**
 * Applies render distance with fog compensation and strong rollback.
 */
final class RenderDistanceApplier {

    private static final double DEFAULT_FOG_RATIO = 0.8;

    private final MinecraftOptionsAdapter options;

    RenderDistanceApplier(MinecraftOptionsAdapter options) {
        this.options = options;
    }

    ApplyResult apply(CapabilityValue.IntValue newValue, CapabilityValue.IntValue previousValue) {
        int newDistance = newValue.value();
        int oldDistance = previousValue.value();
        double fogRatio = calculateFogRatio(oldDistance);

        try {
            boolean distanceSuccess = options.setRenderDistance(newValue);
            if (!distanceSuccess) {
                return new ApplyResult.Failed("setRenderDistance returned false", false, false);
            }

            boolean fogSuccess = adjustFogDistance(newDistance, fogRatio);
            if (!fogSuccess) {
                options.setRenderDistance(previousValue);
                return new ApplyResult.Failed(
                        "Fog distance adjustment failed, rolled back render distance",
                        true,
                        true);
            }

            Optional<CapabilityValue> verifyOpt = options.getRenderDistance();
            if (verifyOpt.isEmpty() || !verifyOpt.get().equals(newValue)) {
                rollbackBoth(previousValue, oldDistance, fogRatio);
                return new ApplyResult.Failed(
                        "Render distance verification failed after apply",
                        true,
                        true);
            }

            return new ApplyResult.Success(previousValue, newValue);
        } catch (Exception e) {
            try {
                rollbackBoth(previousValue, oldDistance, fogRatio);
                return new ApplyResult.Failed(
                        "Exception during apply: " + e.getMessage(),
                        true,
                        true);
            } catch (Exception rollbackException) {
                return new ApplyResult.Failed(
                        "Apply threw + rollback threw: " + e.getMessage(),
                        true,
                        false);
            }
        }
    }

    private double calculateFogRatio(int currentRenderDistance) {
        try {
            Optional<CapabilityValue> fogOpt = options.getFogDistance();
            if (fogOpt.isPresent() && fogOpt.get() instanceof CapabilityValue.IntValue fogValue) {
                int fogDistance = fogValue.value();
                return (double) fogDistance / currentRenderDistance;
            }
        } catch (Exception e) {
            // If can't read fog, use default ratio
        }

        return DEFAULT_FOG_RATIO;
    }

    private boolean adjustFogDistance(int newRenderDistance, double fogRatio) {
        try {
            int newFogDistance = (int) Math.round(newRenderDistance * fogRatio);
            CapabilityValue newFog = new CapabilityValue.IntValue(newFogDistance);
            return options.setFogDistance(newFog);
        } catch (Exception e) {
            // If fog adjustment fails, allow render distance to continue
            return true;
        }
    }

    private void rollbackBoth(CapabilityValue previousDistance, int oldRenderDistance, double fogRatio) {
        options.setRenderDistance(previousDistance);
        adjustFogDistance(oldRenderDistance, fogRatio);
    }
}
