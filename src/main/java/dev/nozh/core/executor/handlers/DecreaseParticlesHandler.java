package dev.nozh.core.executor.handlers;

import dev.nozh.core.executor.ActionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;

/**
 * Phase 6: Strictly decreases particles.
 * ALL -> DECREASED -> MINIMAL
 */
public class DecreaseParticlesHandler implements ActionHandler {

    private String oldValue = "";
    private String newValue = "";

    @Override
    public boolean execute(MinecraftClient client) {
        if (client == null || client.options == null)
            return false;

        ParticlesMode current = client.options.getParticles().getValue();

        if (current == ParticlesMode.ALL) {
            oldValue = "ALL";
            newValue = "DECREASED";
            client.options.getParticles().setValue(ParticlesMode.DECREASED);
            return true;
        } else if (current == ParticlesMode.DECREASED) {
            oldValue = "DECREASED";
            newValue = "MINIMAL";
            client.options.getParticles().setValue(ParticlesMode.MINIMAL);
            return true;
        }

        // Already MINIMAL
        oldValue = "MINIMAL";
        newValue = "MINIMAL";
        return false;
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
        if (client == null || client.options == null)
            return false;
        try {
            ParticlesMode mode = ParticlesMode.valueOf(value);
            client.options.getParticles().setValue(mode);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
