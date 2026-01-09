package dev.nozh.fabric.compat;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.capability.CapabilityValue;

import java.util.EnumSet;
import java.util.Optional;

public interface CompatAdapter {

    String modId();

    EnumSet<CapabilityId> supportedCapabilities();

    boolean isAvailable();

    Optional<CapabilityValue> getCurrentValue(CapabilityId capability);

    boolean apply(CapabilityId capability, CapabilityValue value);
}
