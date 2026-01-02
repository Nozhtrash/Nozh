package dev.nozh.fabric.capability;

import dev.nozh.core.bus.CapabilityValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;

import java.util.Optional;

/**
 * Production implementation of MinecraftOptionsAdapter.
 * 
 * Accesses real Minecraft GameOptions to read/write settings.
 * 
 * CONTRACT:
 * - Never throws exceptions (returns Optional.empty() on errors)
 * - Thread-safe (all access via MC client thread)
 * - Atomic operations (options.write() called after each change)
 */
public final class ProductionMinecraftOptionsAdapter implements MinecraftOptionsAdapter {

    @Override
    public Optional<CapabilityValue> getParticles() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            ParticlesMode mode = options.getParticles().getValue();

            // Map MC enum to CapabilityValue
            String valueName = switch (mode) {
                case ALL -> "ALL";
                case DECREASED -> "DECREASED";
                case MINIMAL -> "MINIMAL";
            };

            return Optional.of(new CapabilityValue.EnumValue(valueName));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setParticles(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.EnumValue enumValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;

            // Map CapabilityValue to MC enum
            ParticlesMode mode = switch (enumValue.name()) {
                case "ALL" -> ParticlesMode.ALL;
                case "DECREASED" -> ParticlesMode.DECREASED;
                case "MINIMAL" -> ParticlesMode.MINIMAL;
                default -> null;
            };

            if (mode == null) {
                return false;
            }

            // Apply
            options.getParticles().setValue(mode);
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getClouds() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            CloudRenderMode mode = options.getCloudRenderMode().getValue();

            String valueName = switch (mode) {
                case OFF -> "OFF";
                case FAST -> "FAST";
                case FANCY -> "FANCY";
            };

            return Optional.of(new CapabilityValue.EnumValue(valueName));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setClouds(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.EnumValue enumValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;

            CloudRenderMode mode = switch (enumValue.name()) {
                case "OFF" -> CloudRenderMode.OFF;
                case "FAST" -> CloudRenderMode.FAST;
                case "FANCY" -> CloudRenderMode.FANCY;
                default -> null;
            };

            if (mode == null) {
                return false;
            }

            options.getCloudRenderMode().setValue(mode);
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getEntityShadows() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            boolean enabled = options.getEntityShadows().getValue();

            return Optional.of(new CapabilityValue.BoolValue(enabled));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setEntityShadows(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.BoolValue boolValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;
            options.getEntityShadows().setValue(boolValue.value());
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getFpsCap() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            int maxFps = options.getMaxFps().getValue();

            return Optional.of(new CapabilityValue.IntValue(maxFps));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setFpsCap(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.IntValue intValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;
            int fps = intValue.value();

            // Validate range (MC supports 10-260 FPS)
            if (fps < 10 || fps > 260) {
                return false;
            }

            options.getMaxFps().setValue(fps);
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getRenderDistance() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            int renderDistance = options.getViewDistance().getValue();

            return Optional.of(new CapabilityValue.IntValue(renderDistance));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setRenderDistance(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.IntValue intValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;
            options.getViewDistance().setValue(intValue.value());
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getSimulationDistance() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            int simulationDistance = options.getSimulationDistance().getValue();

            return Optional.of(new CapabilityValue.IntValue(simulationDistance));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setSimulationDistance(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.IntValue intValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;
            options.getSimulationDistance().setValue(intValue.value());
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getBiomeBlendRadius() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            int biomeBlend = options.getBiomeBlendRadius().getValue();

            return Optional.of(new CapabilityValue.IntValue(biomeBlend));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setBiomeBlendRadius(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.IntValue intValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;
            options.getBiomeBlendRadius().setValue(intValue.value());
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getEntityDistance() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            // Entity distance is stored as percentage (50-500)
            double entityDistance = options.getEntityDistanceScaling().getValue();
            int percentage = (int) (entityDistance * 100);

            return Optional.of(new CapabilityValue.IntValue(percentage));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setEntityDistance(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.IntValue intValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;
            // Convert percentage to decimal (100% = 1.0)
            double decimal = intValue.value() / 100.0;
            options.getEntityDistanceScaling().setValue(decimal);
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getMipmapLevels() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return Optional.empty();
            }

            GameOptions options = client.options;
            int mipmapLevels = options.getMipmapLevels().getValue();

            return Optional.of(new CapabilityValue.IntValue(mipmapLevels));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setMipmapLevels(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.IntValue intValue)) {
                return false;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }

            GameOptions options = client.options;
            options.getMipmapLevels().setValue(intValue.value());
            options.write();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getFogDistance() {
        // Fog distance is typically calculated based on render distance
        // In modern Minecraft, it's not directly accessible
        // Return empty - providers will use default ratio
        return Optional.empty();
    }

    @Override
    public boolean setFogDistance(CapabilityValue value) {
        // Fog distance is calculated automatically
        return true;
    }

    @Override
    public Optional<CapabilityValue> getVsync() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null)
                return Optional.empty();
            boolean vsync = client.options.getEnableVsync().getValue();
            return Optional.of(new CapabilityValue.BoolValue(vsync));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setVsync(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.BoolValue boolValue))
                return false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null)
                return false;
            client.options.getEnableVsync().setValue(boolValue.value());
            client.options.write();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getGraphicsMode() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null)
                return Optional.empty();
            net.minecraft.client.option.GraphicsMode mode = client.options.getGraphicsMode().getValue();
            return Optional.of(new CapabilityValue.EnumValue(mode.name()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setGraphicsMode(CapabilityValue value) {
        try {
            if (!(value instanceof CapabilityValue.EnumValue enumValue))
                return false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null)
                return false;

            net.minecraft.client.option.GraphicsMode mode = switch (enumValue.name()) {
                case "FAST" -> net.minecraft.client.option.GraphicsMode.FAST;
                case "FANCY" -> net.minecraft.client.option.GraphicsMode.FANCY;
                case "FABULOUS" -> net.minecraft.client.option.GraphicsMode.FABULOUS;
                default -> null;
            };
            if (mode == null)
                return false;

            client.options.getGraphicsMode().setValue(mode);
            client.options.write();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // SmoothLighting removed temporarily due to symbol resolution issues

    @Override
    public Optional<CapabilityValue> getDistortionEffectScale() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null)
                return Optional.empty();
            double val = client.options.getDistortionEffectScale().getValue();
            return Optional.of(new CapabilityValue.FloatValue((float) val));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setDistortionEffectScale(CapabilityValue value) {
        try {
            // Support both Float and Int (0 or 1)
            double val;
            if (value instanceof CapabilityValue.FloatValue f)
                val = f.value();
            else if (value instanceof CapabilityValue.IntValue i)
                val = i.value(); // 0 or 1
            else
                return false;

            if (val < 0.0 || val > 1.0)
                return false;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null)
                return false;
            client.options.getDistortionEffectScale().setValue(val);
            client.options.write();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<CapabilityValue> getArmorStands() {
        return Optional
                .of(new CapabilityValue.BoolValue(dev.nozh.core.settings.NozhRenderSettings.isArmorStandsVisible()));
    }

    @Override
    public boolean setArmorStands(CapabilityValue value) {
        if (value instanceof CapabilityValue.BoolValue b) {
            dev.nozh.core.settings.NozhRenderSettings.setArmorStandsVisible(b.value());
            return true;
        }
        return false;
    }

    @Override
    public Optional<CapabilityValue> getItemFrames() {
        return Optional
                .of(new CapabilityValue.BoolValue(dev.nozh.core.settings.NozhRenderSettings.isItemFramesVisible()));
    }

    @Override
    public boolean setItemFrames(CapabilityValue value) {
        if (value instanceof CapabilityValue.BoolValue b) {
            dev.nozh.core.settings.NozhRenderSettings.setItemFramesVisible(b.value());
            return true;
        }
        return false;
    }

    @Override
    public Optional<CapabilityValue> getBlockEntities() {
        return Optional
                .of(new CapabilityValue.BoolValue(dev.nozh.core.settings.NozhRenderSettings.isBlockEntitiesVisible()));
    }

    @Override
    public boolean setBlockEntities(CapabilityValue value) {
        if (value instanceof CapabilityValue.BoolValue b) {
            dev.nozh.core.settings.NozhRenderSettings.setBlockEntitiesVisible(b.value());
            return true;
        }
        return false;
    }

    @Override
    public Optional<CapabilityValue> getAnimations() {
        return Optional
                .of(new CapabilityValue.BoolValue(dev.nozh.core.settings.NozhRenderSettings.isAllAnimationsVisible()));
    }

    @Override
    public boolean setAnimations(CapabilityValue value) {
        if (value instanceof CapabilityValue.BoolValue b) {
            dev.nozh.core.settings.NozhRenderSettings.setAllAnimationsVisible(b.value());
            return true;
        }
        return false;
    }
}
