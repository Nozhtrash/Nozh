package dev.nozh.core.capability;

/**
 * Type-safe capability values (Contract 2, Rule 2.3).
 * 
 * Sealed union: IntValue | EnumValue | BoolValue | FloatValue
 * NO raw Strings in commands.
 */
public sealed interface CapabilityValue {

    /**
     * Integer value (e.g., render distance, biome blend radius).
     */
    record IntValue(int value) implements CapabilityValue {
    }

    /**
     * Enum value as string (e.g., "FANCY", "FAST", "OFF").
     */
    record EnumValue(String name) implements CapabilityValue {
    }

    /**
     * Boolean value (e.g., entity shadows ON/OFF).
     */
    record BoolValue(boolean value) implements CapabilityValue {
    }

    /**
     * Float value (e.g., resolution scale 0.5-1.0).
     */
    record FloatValue(float value) implements CapabilityValue {
    }
}
