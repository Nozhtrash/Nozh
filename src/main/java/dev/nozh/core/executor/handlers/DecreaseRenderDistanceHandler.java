package dev.nozh.core.executor.handlers;

import dev.nozh.core.capability.CapabilityValue;
import dev.nozh.core.executor.ActionHandler;
import dev.nozh.fabric.capability.CompatAwareMinecraftOptionsAdapter;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;
import dev.nozh.fabric.capability.ProductionMinecraftOptionsAdapter;
import dev.nozh.fabric.compat.CompatRegistry;
import net.minecraft.client.MinecraftClient;

import java.util.Optional;

/**
 * Phase 6: Gradually decreases render distance with fog compensation.
 */
public class DecreaseRenderDistanceHandler implements ActionHandler {

    private static final int MIN_RENDER_DISTANCE = 2;
    private static final int RAMP_STEP = 2;

    private String oldValue = "";
    private String newValue = "";

    @Override
    public boolean execute(MinecraftClient client) {
        MinecraftOptionsAdapter options = new CompatAwareMinecraftOptionsAdapter(
                new ProductionMinecraftOptionsAdapter(),
                new CompatRegistry());
        Optional<CapabilityValue> currentOpt = options.getRenderDistance();
        if (currentOpt.isEmpty() || !(currentOpt.get() instanceof CapabilityValue.IntValue currentValue)) {
            return false;
        }

        int current = currentValue.value();
        int next = Math.max(current - RAMP_STEP, MIN_RENDER_DISTANCE);

        oldValue = Integer.toString(current);
        newValue = Integer.toString(next);

        if (next == current) {
            return false;
        }

        RenderDistanceApplier applier = new RenderDistanceApplier(options);
        return applier.apply(new CapabilityValue.IntValue(next), currentValue).succeeded();
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
        MinecraftOptionsAdapter options = new CompatAwareMinecraftOptionsAdapter(
                new ProductionMinecraftOptionsAdapter(),
                new CompatRegistry());
        try {
            int target = Integer.parseInt(value);
            Optional<CapabilityValue> currentOpt = options.getRenderDistance();
            if (currentOpt.isEmpty() || !(currentOpt.get() instanceof CapabilityValue.IntValue currentValue)) {
                return false;
            }
            RenderDistanceApplier applier = new RenderDistanceApplier(options);
            return applier.apply(new CapabilityValue.IntValue(target), currentValue).succeeded();
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
