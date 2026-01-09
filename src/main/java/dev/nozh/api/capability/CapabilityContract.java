package dev.nozh.api.capability;

import dev.nozh.core.capability.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Public contract describing how a capability behaves and what values it accepts.
 */
public final class CapabilityContract {

    public enum ValueType {
        INT,
        FLOAT,
        BOOL,
        ENUM
    }

    private final CapabilityId capabilityId;
    private final ValueType valueType;
    private final String description;
    private final CapabilityValue defaultValue;
    private final Double minValue;
    private final Double maxValue;
    private final Set<String> allowedValues;
    private final String unit;

    private CapabilityContract(
            CapabilityId capabilityId,
            ValueType valueType,
            String description,
            CapabilityValue defaultValue,
            Double minValue,
            Double maxValue,
            Set<String> allowedValues,
            String unit) {
        this.capabilityId = Objects.requireNonNull(capabilityId, "capabilityId");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.allowedValues = allowedValues == null ? Set.of() : Set.copyOf(allowedValues);
        this.unit = unit == null ? "" : unit;
    }

    public CapabilityId capabilityId() {
        return capabilityId;
    }

    public ValueType valueType() {
        return valueType;
    }

    public String description() {
        return description;
    }

    public CapabilityValue defaultValue() {
        return defaultValue;
    }

    public Double minValue() {
        return minValue;
    }

    public Double maxValue() {
        return maxValue;
    }

    public Set<String> allowedValues() {
        return Collections.unmodifiableSet(allowedValues);
    }

    public String unit() {
        return unit;
    }

    public static Builder builder(CapabilityId capabilityId, ValueType valueType) {
        return new Builder(capabilityId, valueType);
    }

    public static final class Builder {
        private final CapabilityId capabilityId;
        private final ValueType valueType;
        private String description;
        private CapabilityValue defaultValue;
        private Double minValue;
        private Double maxValue;
        private final Set<String> allowedValues = new LinkedHashSet<>();
        private String unit;

        private Builder(CapabilityId capabilityId, ValueType valueType) {
            this.capabilityId = Objects.requireNonNull(capabilityId, "capabilityId");
            this.valueType = Objects.requireNonNull(valueType, "valueType");
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder defaultValue(CapabilityValue defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder range(double minValue, double maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            return this;
        }

        public Builder allowedValues(String... values) {
            if (values != null) {
                Collections.addAll(allowedValues, values);
            }
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public CapabilityContract build() {
            return new CapabilityContract(
                    capabilityId,
                    valueType,
                    description,
                    defaultValue,
                    minValue,
                    maxValue,
                    allowedValues,
                    unit);
        }
    }
}
