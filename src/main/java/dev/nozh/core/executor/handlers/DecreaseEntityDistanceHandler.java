package dev.nozh.core.executor.handlers;

import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.executor.ActionHandler;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProductionMinecraftOptionsAdapter;
import net.minecraft.client.MinecraftClient;

import java.util.Optional;

/**
 * Phase 6: Gradually decreases entity distance percentage.
 */
public class DecreaseEntityDistanceHandler implements ActionHandler {

    private static final int MIN_ENTITY_DISTANCE = 50;
    private static final int RAMP_STEP = 25;

    private String oldValue = "";
    private String newValue = "";

    @Override
    public boolean execute(MinecraftClient client) {
        MinecraftOptionsAdapter options = new ProductionMinecraftOptionsAdapter();
        Optional<CapabilityValue> currentOpt = options.getEntityDistance();
        if (currentOpt.isEmpty() || !(currentOpt.get() instanceof CapabilityValue.IntValue currentValue)) {
            return false;
        }

        int current = currentValue.value();
        int next = Math.max(current - RAMP_STEP, MIN_ENTITY_DISTANCE);

        oldValue = Integer.toString(current);
        newValue = Integer.toString(next);

        if (next == current) {
            return false;
        }

        return applyWithVerification(options, new CapabilityValue.IntValue(next), currentValue);
    }

    @Override
    public String getLastChangeDetails() {
        return oldValue + " -> " + newValue;
    }

    @Override
    public String getOldValue() {
        return oldValue;
    }

    @Override
    public String getNewValue() {
        return newValue;
    }

    @Override
    public boolean apply(MinecraftClient client, String value) {
        MinecraftOptionsAdapter options = new ProductionMinecraftOptionsAdapter();
        try {
            int target = Integer.parseInt(value);
            Optional<CapabilityValue> currentOpt = options.getEntityDistance();
            if (currentOpt.isEmpty() || !(currentOpt.get() instanceof CapabilityValue.IntValue currentValue)) {
                return false;
            }
            return applyWithVerification(options, new CapabilityValue.IntValue(target), currentValue);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean applyWithVerification(
            MinecraftOptionsAdapter options,
            CapabilityValue.IntValue target,
            CapabilityValue.IntValue previous) {
        boolean success = options.setEntityDistance(target);
        if (!success) {
            return false;
        }

        Optional<CapabilityValue> verifyOpt = options.getEntityDistance();
        if (verifyOpt.isEmpty() || !verifyOpt.get().equals(target)) {
            options.setEntityDistance(previous);
            return false;
        }

        return true;
    }
}
