package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.Arrays;
import java.util.List;

/**
 * Controller for gradual restoration of capabilities.
 * 
 * Instead of instantly restoring settings to maximum, this controller
 * provides progressive restoration paths for smoother user experience.
 * 
 * PRIORITY 3: Gradual restoration for visual quality settings.
 */
public final class GradualRestoreController {

    // Particle progression: MINIMAL → DECREASED → ALL
    private static final List<String> PARTICLES_PROGRESSION = Arrays.asList(
        "MINIMAL", "DECREASED", "ALL"
    );
    
    // Clouds progression: false → true
    private static final List<Boolean> CLOUDS_PROGRESSION = Arrays.asList(
        false, true
    );
    
    // Graphics mode progression: FAST → FANCY → FABULOUS
    private static final List<String> GRAPHICS_MODE_PROGRESSION = Arrays.asList(
        "FAST", "FANCY", "FABULOUS"
    );

    /**
     * Get gradual restore value for a capability.
     * 
     * @param capability The capability to restore
     * @param currentValue Current value
     * @param targetValue Target (maximum) value
     * @return Next value in progression, or targetValue if no progression defined
     */
    public CapabilityValue getGradualRestoreValue(
            CapabilityId capability, 
            CapabilityValue currentValue, 
            CapabilityValue targetValue) {
        
        return switch (capability) {
            case PARTICLES -> getNextEnumValue(PARTICLES_PROGRESSION, currentValue, targetValue);
            case CLOUDS -> getNextBoolValue(CLOUDS_PROGRESSION, currentValue, targetValue);
            case GRAPHICS_MODE -> getNextEnumValue(GRAPHICS_MODE_PROGRESSION, currentValue, targetValue);
            case RENDER_DISTANCE -> getNextIntValue(currentValue, targetValue, 2);
            case ENTITY_DISTANCE -> getNextIntValue(currentValue, targetValue, 1);
            case SIMULATION_DISTANCE -> getNextIntValue(currentValue, targetValue, 1);
            default -> targetValue; // No gradual progression
        };
    }

    /**
     * Get gradual reduce value for a capability.
     * 
     * @param capability The capability to reduce
     * @param currentValue Current value
     * @param targetValue Target (minimum) value
     * @return Next value in reduction, or targetValue if no progression defined
     */
    public CapabilityValue getGradualReduceValue(
            CapabilityId capability,
            CapabilityValue currentValue,
            CapabilityValue targetValue) {
        
        return switch (capability) {
            case PARTICLES -> getPrevEnumValue(PARTICLES_PROGRESSION, currentValue, targetValue);
            case CLOUDS -> getPrevBoolValue(CLOUDS_PROGRESSION, currentValue, targetValue);
            case GRAPHICS_MODE -> getPrevEnumValue(GRAPHICS_MODE_PROGRESSION, currentValue, targetValue);
            case RENDER_DISTANCE -> getPrevIntValue(currentValue, targetValue, 2);
            case ENTITY_DISTANCE -> getPrevIntValue(currentValue, targetValue, 1);
            case SIMULATION_DISTANCE -> getPrevIntValue(currentValue, targetValue, 1);
            default -> targetValue; // No gradual progression
        };
    }

    /**
     * Check if a capability supports gradual progression.
     */
    public boolean supportsGradualProgression(CapabilityId capability) {
        return switch (capability) {
            case PARTICLES, CLOUDS, GRAPHICS_MODE, RENDER_DISTANCE, 
                 ENTITY_DISTANCE, SIMULATION_DISTANCE -> true;
            default -> false;
        };
    }

    // Helper methods for enum progression
    private CapabilityValue getNextEnumValue(
            List<String> progression, 
            CapabilityValue current, 
            CapabilityValue target) {
        
        if (!(current instanceof CapabilityValue.EnumValue currentEnum) ||
            !(target instanceof CapabilityValue.EnumValue targetEnum)) {
            return target;
        }
        
        int currentIndex = progression.indexOf(currentEnum.name());
        int targetIndex = progression.indexOf(targetEnum.name());
        
        if (currentIndex < 0 || targetIndex < 0) {
            return target;
        }
        
        if (currentIndex >= targetIndex) {
            return target;
        }
        
        // Move one step towards target
        int nextIndex = Math.min(currentIndex + 1, targetIndex);
        return new CapabilityValue.EnumValue(progression.get(nextIndex));
    }

    private CapabilityValue getPrevEnumValue(
            List<String> progression,
            CapabilityValue current,
            CapabilityValue target) {
        
        if (!(current instanceof CapabilityValue.EnumValue currentEnum) ||
            !(target instanceof CapabilityValue.EnumValue targetEnum)) {
            return target;
        }
        
        int currentIndex = progression.indexOf(currentEnum.name());
        int targetIndex = progression.indexOf(targetEnum.name());
        
        if (currentIndex < 0 || targetIndex < 0) {
            return target;
        }
        
        if (currentIndex <= targetIndex) {
            return target;
        }
        
        // Move one step towards target
        int nextIndex = Math.max(currentIndex - 1, targetIndex);
        return new CapabilityValue.EnumValue(progression.get(nextIndex));
    }

    // Helper methods for boolean progression
    private CapabilityValue getNextBoolValue(
            List<Boolean> progression,
            CapabilityValue current,
            CapabilityValue target) {
        
        if (!(current instanceof CapabilityValue.BoolValue currentBool) ||
            !(target instanceof CapabilityValue.BoolValue targetBool)) {
            return target;
        }
        
        int currentIndex = progression.indexOf(currentBool.value());
        int targetIndex = progression.indexOf(targetBool.value());
        
        if (currentIndex < 0 || targetIndex < 0) {
            return target;
        }
        
        if (currentIndex >= targetIndex) {
            return target;
        }
        
        int nextIndex = Math.min(currentIndex + 1, targetIndex);
        return new CapabilityValue.BoolValue(progression.get(nextIndex));
    }

    private CapabilityValue getPrevBoolValue(
            List<Boolean> progression,
            CapabilityValue current,
            CapabilityValue target) {
        
        if (!(current instanceof CapabilityValue.BoolValue currentBool) ||
            !(target instanceof CapabilityValue.BoolValue targetBool)) {
            return target;
        }
        
        int currentIndex = progression.indexOf(currentBool.value());
        int targetIndex = progression.indexOf(targetBool.value());
        
        if (currentIndex < 0 || targetIndex < 0) {
            return target;
        }
        
        if (currentIndex <= targetIndex) {
            return target;
        }
        
        int nextIndex = Math.max(currentIndex - 1, targetIndex);
        return new CapabilityValue.BoolValue(progression.get(nextIndex));
    }

    // Helper methods for integer progression
    private CapabilityValue getNextIntValue(
            CapabilityValue current,
            CapabilityValue target,
            int step) {
        
        if (!(current instanceof CapabilityValue.IntValue currentInt) ||
            !(target instanceof CapabilityValue.IntValue targetInt)) {
            return target;
        }
        
        int currentVal = currentInt.value();
        int targetVal = targetInt.value();
        
        if (currentVal >= targetVal) {
            return target;
        }
        
        int nextVal = Math.min(currentVal + step, targetVal);
        return new CapabilityValue.IntValue(nextVal);
    }

    private CapabilityValue getPrevIntValue(
            CapabilityValue current,
            CapabilityValue target,
            int step) {
        
        if (!(current instanceof CapabilityValue.IntValue currentInt) ||
            !(target instanceof CapabilityValue.IntValue targetInt)) {
            return target;
        }
        
        int currentVal = currentInt.value();
        int targetVal = targetInt.value();
        
        if (currentVal <= targetVal) {
            return target;
        }
        
        int nextVal = Math.max(currentVal - step, targetVal);
        return new CapabilityValue.IntValue(nextVal);
    }
}
