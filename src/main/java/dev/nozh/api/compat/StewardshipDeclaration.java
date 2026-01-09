package dev.nozh.api.compat;

import dev.nozh.core.capability.CapabilityId;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

public final class StewardshipDeclaration {

    private final String modId;
    private final String displayName;
    private final EnumSet<CapabilityId> exclusiveCapabilities;
    private final EnumSet<CapabilityId> sharedCapabilities;
    private final String reason;

    private StewardshipDeclaration(
            String modId,
            String displayName,
            EnumSet<CapabilityId> exclusiveCapabilities,
            EnumSet<CapabilityId> sharedCapabilities,
            String reason) {
        this.modId = modId;
        this.displayName = displayName;
        this.exclusiveCapabilities = EnumSet.copyOf(exclusiveCapabilities);
        this.sharedCapabilities = EnumSet.copyOf(sharedCapabilities);
        this.reason = reason == null ? "" : reason;
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }

    public EnumSet<CapabilityId> exclusiveCapabilities() {
        return EnumSet.copyOf(exclusiveCapabilities);
    }

    public EnumSet<CapabilityId> sharedCapabilities() {
        return EnumSet.copyOf(sharedCapabilities);
    }

    public String reason() {
        return reason;
    }

    public StewardshipMode modeFor(CapabilityId capability) {
        if (sharedCapabilities.contains(capability)) {
            return StewardshipMode.SHARED;
        }
        if (exclusiveCapabilities.contains(capability)) {
            return StewardshipMode.EXCLUSIVE;
        }
        return StewardshipMode.NONE;
    }

    public static Builder builder(String modId, String displayName) {
        return new Builder(modId, displayName);
    }

    public static final class Builder {
        private final String modId;
        private final String displayName;
        private final EnumSet<CapabilityId> exclusive = EnumSet.noneOf(CapabilityId.class);
        private final EnumSet<CapabilityId> shared = EnumSet.noneOf(CapabilityId.class);
        private String reason = "";

        private Builder(String modId, String displayName) {
            this.modId = Objects.requireNonNull(modId, "modId");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        public Builder exclusive(CapabilityId... capabilities) {
            if (capabilities != null) {
                exclusive.addAll(Arrays.asList(capabilities));
            }
            return this;
        }

        public Builder shared(CapabilityId... capabilities) {
            if (capabilities != null) {
                shared.addAll(Arrays.asList(capabilities));
            }
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason == null ? "" : reason;
            return this;
        }

        public StewardshipDeclaration build() {
            return new StewardshipDeclaration(modId, displayName, exclusive, shared, reason);
        }
    }
}
