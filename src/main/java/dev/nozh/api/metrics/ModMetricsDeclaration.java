package dev.nozh.api.metrics;

import dev.nozh.core.bus.CapabilityId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Declares internal metrics exposed by a mod to NOZH.
 */
public final class ModMetricsDeclaration {

    public enum MetricType {
        COUNTER,
        GAUGE,
        HISTOGRAM,
        TIMER,
        EVENT
    }

    public record MetricDefinition(
            String name,
            MetricType type,
            String description,
            String unit) {
        public MetricDefinition {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
        }
    }

    private final String modId;
    private final String displayName;
    private final EnumSet<CapabilityId> capabilities;
    private final List<MetricDefinition> metrics;
    private final String notes;

    private ModMetricsDeclaration(
            String modId,
            String displayName,
            EnumSet<CapabilityId> capabilities,
            List<MetricDefinition> metrics,
            String notes) {
        this.modId = modId;
        this.displayName = displayName;
        this.capabilities = EnumSet.copyOf(capabilities);
        this.metrics = List.copyOf(metrics);
        this.notes = notes == null ? "" : notes;
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }

    public EnumSet<CapabilityId> capabilities() {
        return EnumSet.copyOf(capabilities);
    }

    public List<MetricDefinition> metrics() {
        return Collections.unmodifiableList(metrics);
    }

    public String notes() {
        return notes;
    }

    public static Builder builder(String modId, String displayName) {
        return new Builder(modId, displayName);
    }

    public static final class Builder {
        private final String modId;
        private final String displayName;
        private final EnumSet<CapabilityId> capabilities = EnumSet.noneOf(CapabilityId.class);
        private final List<MetricDefinition> metrics = new ArrayList<>();
        private String notes;

        private Builder(String modId, String displayName) {
            this.modId = Objects.requireNonNull(modId, "modId");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        public Builder capability(CapabilityId capability) {
            if (capability != null) {
                capabilities.add(capability);
            }
            return this;
        }

        public Builder capabilities(CapabilityId... capabilities) {
            if (capabilities != null) {
                Collections.addAll(this.capabilities, capabilities);
            }
            return this;
        }

        public Builder metric(MetricDefinition metric) {
            if (metric != null) {
                metrics.add(metric);
            }
            return this;
        }

        public Builder metric(String name, MetricType type, String description, String unit) {
            return metric(new MetricDefinition(name, type, description, unit));
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public ModMetricsDeclaration build() {
            return new ModMetricsDeclaration(modId, displayName, capabilities, metrics, notes);
        }
    }
}
