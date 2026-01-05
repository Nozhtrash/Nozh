package dev.nozh.fabric.compat;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;

import java.util.EnumSet;
import java.util.Optional;

public final class LambDynamicLightsAdapter implements CompatAdapter {

    private static final EnumSet<CapabilityId> SUPPORTED = EnumSet.of(CapabilityId.DYNAMIC_LIGHTING);
    private final DynamicLightingBridge bridge = new DynamicLightingBridge();

    @Override
    public String modId() {
        return "lambdynlights";
    }

    @Override
    public EnumSet<CapabilityId> supportedCapabilities() {
        return SUPPORTED;
    }

    @Override
    public boolean isAvailable() {
        return bridge.isAvailable();
    }

    @Override
    public Optional<CapabilityValue> getCurrentValue(CapabilityId capability) {
        if (capability != CapabilityId.DYNAMIC_LIGHTING) {
            return Optional.empty();
        }
        return bridge.getCurrentValue();
    }

    @Override
    public boolean apply(CapabilityId capability, CapabilityValue value) {
        if (capability != CapabilityId.DYNAMIC_LIGHTING) {
            return false;
        }
        return bridge.apply(value);
    }
}
