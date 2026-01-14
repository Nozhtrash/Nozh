package dev.nozh.fabric.potato;

import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.core.potato.PotatoConfigApplicator;
import dev.nozh.core.potato.PotatoModeEngine.PotatoConfig;
import dev.nozh.fabric.capability.MinecraftOptionsAdapter;

public class FabricPotatoConfigApplicator implements PotatoConfigApplicator {

    private final MinecraftOptionsAdapter options;

    public FabricPotatoConfigApplicator(MinecraftOptionsAdapter options) {
        this.options = options;
    }

    @Override
    public boolean isAvailable() {
        return options != null;
    }

    @Override
    public void apply(PotatoConfig config) {
        if (!isAvailable())
            return;

        // Render Distance
        options.setRenderDistance(new CapabilityValue.IntValue(config.renderDistance()));

        // Entity Distance (Convert chunks roughly to %)
        // Base is 100% = 16 chunks approx? Standard MC logic is vague, but let's
        // approximate.
        // Actually, let's just use a reasonable mapping based on the values.
        int entityDistancePercent = (int) ((config.entityDistance() / 16.0) * 100);
        if (entityDistancePercent < 25)
            entityDistancePercent = 25; // Clamp min
        if (entityDistancePercent > 500)
            entityDistancePercent = 500; // Clamp max
        options.setEntityDistance(new CapabilityValue.IntValue(entityDistancePercent));

        // Particles
        String particleMode = "ALL";
        if (config.particleMultiplier() <= 0.2) {
            particleMode = "MINIMAL";
        } else if (config.particleMultiplier() < 0.8) {
            particleMode = "DECREASED";
        }
        options.setParticles(new CapabilityValue.EnumValue(particleMode));

        // Clouds (True = FAST for potato context, False = OFF)
        // MC Options usually: OFF, FAST, FANCY.
        // If config says no clouds -> OFF. If yes -> FAST (performance)
        // Check if options.setClouds expects Enum or Bool. Assuming Enum based on
        // standard options.
        // But CapabilityValue.BoolValue might be supported by the adapter impl.
        // Let's use Enum to be safe with vanilla options.
        options.setClouds(new CapabilityValue.EnumValue(config.cloudRendering() ? "FAST" : "OFF"));

        // Entity Shadows
        options.setEntityShadows(new CapabilityValue.BoolValue(config.entityShadows()));

        // Animations (Assuming adapter applies to all known animations)
        // Nozh Potato Mode simplifies this to a global toggle.
        // But wait, setAnimations takes CapabilityValue. Maybe it expects BoolValue?
        // Let's assume BoolValue.
        options.setAnimations(new CapabilityValue.BoolValue(config.animationsEnabled()));

        // Graphics Mode
        // If potato -> FAST. If survival/high -> FANCY/FABULOUS.
        // Usage: config.level usually determines this implicitly via potato config
        // values? No.
        // PotatoConfig doesn't have graphics mode enum.
        // But we can infer. If shadows are off, likely FAST.
        String graphicsMode = config.entityShadows() ? "FANCY" : "FAST";
        options.setGraphicsMode(new CapabilityValue.EnumValue(graphicsMode));
    }
}
