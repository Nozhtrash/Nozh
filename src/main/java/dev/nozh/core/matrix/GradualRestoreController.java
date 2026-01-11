package dev.nozh.core.matrix;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.List;

/**
 * Controller for gradual quality restoration.
 * 
 * When performance improves significantly (P95 below target),
 * this controller gradually restores quality settings instead
 * of jumping directly to maximum quality.
 * 
 * Example progression:
 * - RENDER_DISTANCE: 4 → 6 → 8 → 12 (not 4 → 12 directly)
 * - PARTICLES: OFF → MINIMAL → DECREASED → ALL
 * - ENTITY_DISTANCE: 60 → 75 → 100
 * 
 * PRIORITY 3 - Gradual Restoration (Task 7)
 */
public final class GradualRestoreController {

    /**
     * Particle quality progression order.
     */
    private static final List<String> PARTICLE_ORDER = List.of("OFF", "MINIMAL", "DECREASED", "ALL");

    /**
     * Cloud quality progression order.
     */
    private static final List<String> CLOUD_ORDER = List.of("OFF", "FAST", "FANCY");

    /**
     * Graphics mode progression order.
     */
    private static final List<String> GRAPHICS_ORDER = List.of("FAST", "FANCY", "FABULOUS");

    /**
     * Render distance progression steps.
     */
    private static final List<Integer> RENDER_DISTANCE_STEPS = List.of(2, 4, 6, 8, 10, 12, 16, 20, 24, 32);

    /**
     * Entity distance progression steps.
     */
    private static final List<Integer> ENTITY_DISTANCE_STEPS = List.of(50, 60, 75, 100, 125, 150, 200, 250);

    /**
     * Simulation distance progression steps.
     */
    private static final List<Integer> SIMULATION_DISTANCE_STEPS = List.of(2, 4, 6, 8, 10, 12, 16);

    /**
     * Get the next step value when restoring quality gradually.
     * 
     * @param capability The capability being restored
     * @param currentValue Current value
     * @return Next higher quality value, or current if at maximum
     */
    public static CapabilityValue getGradualRestoreValue(
            CapabilityId capability,
            CapabilityValue currentValue) {
        
        return switch (capability) {
            case PARTICLES -> getNextInProgression(currentValue, PARTICLE_ORDER);
            case CLOUDS -> getNextInProgression(currentValue, CLOUD_ORDER);
            case GRAPHICS_MODE -> getNextInProgression(currentValue, GRAPHICS_ORDER);
            case RENDER_DISTANCE -> getNextIntValue(currentValue, RENDER_DISTANCE_STEPS);
            case ENTITY_DISTANCE -> getNextIntValue(currentValue, ENTITY_DISTANCE_STEPS);
            case SIMULATION_DISTANCE -> getNextIntValue(currentValue, SIMULATION_DISTANCE_STEPS);
            default -> currentValue; // No gradual progression defined
        };
    }

    /**
     * Get the next step value when reducing quality gradually.
     * 
     * @param capability The capability being reduced
     * @param currentValue Current value
     * @return Next lower quality value, or current if at minimum
     */
    public static CapabilityValue getGradualReduceValue(
            CapabilityId capability,
            CapabilityValue currentValue) {
        
        return switch (capability) {
            case PARTICLES -> getPrevInProgression(currentValue, PARTICLE_ORDER);
            case CLOUDS -> getPrevInProgression(currentValue, CLOUD_ORDER);
            case GRAPHICS_MODE -> getPrevInProgression(currentValue, GRAPHICS_ORDER);
            case RENDER_DISTANCE -> getPrevIntValue(currentValue, RENDER_DISTANCE_STEPS);
            case ENTITY_DISTANCE -> getPrevIntValue(currentValue, ENTITY_DISTANCE_STEPS);
            case SIMULATION_DISTANCE -> getPrevIntValue(currentValue, SIMULATION_DISTANCE_STEPS);
            default -> currentValue; // No gradual progression defined
        };
    }

    /**
     * Check if a capability supports gradual progression.
     * 
     * @param capability The capability to check
     * @return true if gradual progression is available
     */
    public static boolean supportsGradualProgression(CapabilityId capability) {
        return switch (capability) {
            case PARTICLES, CLOUDS, GRAPHICS_MODE,
                 RENDER_DISTANCE, ENTITY_DISTANCE, SIMULATION_DISTANCE -> true;
            default -> false;
        };
    }

    /**
     * Get next value in an ordered progression.
     */
    private static CapabilityValue getNextInProgression(
            CapabilityValue current,
            List<String> progression) {
        
        String currentName = current.name();
        int currentIndex = progression.indexOf(currentName);
        
        if (currentIndex < 0 || currentIndex >= progression.size() - 1) {
            return current; // Already at max or not found
        }
        
        String nextName = progression.get(currentIndex + 1);
        return CapabilityValue.valueOf(nextName);
    }

    /**
     * Get previous value in an ordered progression.
     */
    private static CapabilityValue getPrevInProgression(
            CapabilityValue current,
            List<String> progression) {
        
        String currentName = current.name();
        int currentIndex = progression.indexOf(currentName);
        
        if (currentIndex <= 0) {
            return current; // Already at min or not found
        }
        
        String prevName = progression.get(currentIndex - 1);
        return CapabilityValue.valueOf(prevName);
    }

    /**
     * Get next higher integer value from progression steps.
     */
    private static CapabilityValue getNextIntValue(
            CapabilityValue current,
            List<Integer> steps) {
        
        try {
            int currentInt = Integer.parseInt(current.name());
            
            // Find next higher step
            for (int step : steps) {
                if (step > currentInt) {
                    return CapabilityValue.of(String.valueOf(step));
                }
            }
            
            return current; // Already at or above max
        } catch (NumberFormatException e) {
            return current; // Not a number
        }
    }

    /**
     * Get next lower integer value from progression steps.
     */
    private static CapabilityValue getPrevIntValue(
            CapabilityValue current,
            List<Integer> steps) {
        
        try {
            int currentInt = Integer.parseInt(current.name());
            
            // Find previous lower step (iterate in reverse)
            for (int i = steps.size() - 1; i >= 0; i--) {
                int step = steps.get(i);
                if (step < currentInt) {
                    return CapabilityValue.of(String.valueOf(step));
                }
            }
            
            return current; // Already at or below min
        } catch (NumberFormatException e) {
            return current; // Not a number
        }
    }

    /**
     * Get number of steps available for a capability.
     * 
     * @param capability The capability to check
     * @return Number of progression steps, or 0 if not supported
     */
    public static int getProgressionSteps(CapabilityId capability) {
        return switch (capability) {
            case PARTICLES -> PARTICLE_ORDER.size();
            case CLOUDS -> CLOUD_ORDER.size();
            case GRAPHICS_MODE -> GRAPHICS_ORDER.size();
            case RENDER_DISTANCE -> RENDER_DISTANCE_STEPS.size();
            case ENTITY_DISTANCE -> ENTITY_DISTANCE_STEPS.size();
            case SIMULATION_DISTANCE -> SIMULATION_DISTANCE_STEPS.size();
            default -> 0;
        };
    }
}
