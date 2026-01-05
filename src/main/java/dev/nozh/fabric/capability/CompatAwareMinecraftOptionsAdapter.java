package dev.nozh.fabric.capability;

import dev.nozh.core.bus.CapabilityId;
import dev.nozh.core.bus.CapabilityValue;
import dev.nozh.fabric.compat.CompatAdapter;
import dev.nozh.fabric.compat.CompatRegistry;

import java.util.Optional;
import java.util.function.Supplier;

public final class CompatAwareMinecraftOptionsAdapter implements MinecraftOptionsAdapter {

    private final MinecraftOptionsAdapter fallback;
    private final CompatRegistry compatRegistry;

    public CompatAwareMinecraftOptionsAdapter(MinecraftOptionsAdapter fallback, CompatRegistry compatRegistry) {
        this.fallback = fallback;
        this.compatRegistry = compatRegistry;
    }

    @Override
    public Optional<CapabilityValue> getParticles() {
        return getValue(CapabilityId.PARTICLES, fallback::getParticles);
    }

    @Override
    public boolean setParticles(CapabilityValue value) {
        return setValue(CapabilityId.PARTICLES, value, () -> fallback.setParticles(value));
    }

    @Override
    public Optional<CapabilityValue> getClouds() {
        return getValue(CapabilityId.CLOUDS, fallback::getClouds);
    }

    @Override
    public boolean setClouds(CapabilityValue value) {
        return setValue(CapabilityId.CLOUDS, value, () -> fallback.setClouds(value));
    }

    @Override
    public Optional<CapabilityValue> getEntityShadows() {
        return getValue(CapabilityId.ENTITY_SHADOWS, fallback::getEntityShadows);
    }

    @Override
    public boolean setEntityShadows(CapabilityValue value) {
        return setValue(CapabilityId.ENTITY_SHADOWS, value, () -> fallback.setEntityShadows(value));
    }

    @Override
    public Optional<CapabilityValue> getFpsCap() {
        return getValue(CapabilityId.FPS_CAP, fallback::getFpsCap);
    }

    @Override
    public boolean setFpsCap(CapabilityValue value) {
        return setValue(CapabilityId.FPS_CAP, value, () -> fallback.setFpsCap(value));
    }

    @Override
    public Optional<CapabilityValue> getRenderDistance() {
        return getValue(CapabilityId.RENDER_DISTANCE, fallback::getRenderDistance);
    }

    @Override
    public boolean setRenderDistance(CapabilityValue value) {
        return setValue(CapabilityId.RENDER_DISTANCE, value, () -> fallback.setRenderDistance(value));
    }

    @Override
    public Optional<CapabilityValue> getSimulationDistance() {
        return getValue(CapabilityId.SIMULATION_DISTANCE, fallback::getSimulationDistance);
    }

    @Override
    public boolean setSimulationDistance(CapabilityValue value) {
        return setValue(CapabilityId.SIMULATION_DISTANCE, value, () -> fallback.setSimulationDistance(value));
    }

    @Override
    public Optional<CapabilityValue> getBiomeBlendRadius() {
        return getValue(CapabilityId.BIOME_BLEND, fallback::getBiomeBlendRadius);
    }

    @Override
    public boolean setBiomeBlendRadius(CapabilityValue value) {
        return setValue(CapabilityId.BIOME_BLEND, value, () -> fallback.setBiomeBlendRadius(value));
    }

    @Override
    public Optional<CapabilityValue> getEntityDistance() {
        return getValue(CapabilityId.ENTITY_DISTANCE, fallback::getEntityDistance);
    }

    @Override
    public boolean setEntityDistance(CapabilityValue value) {
        return setValue(CapabilityId.ENTITY_DISTANCE, value, () -> fallback.setEntityDistance(value));
    }

    @Override
    public Optional<CapabilityValue> getMipmapLevels() {
        return getValue(CapabilityId.MIPMAP_LEVEL, fallback::getMipmapLevels);
    }

    @Override
    public boolean setMipmapLevels(CapabilityValue value) {
        return setValue(CapabilityId.MIPMAP_LEVEL, value, () -> fallback.setMipmapLevels(value));
    }

    @Override
    public Optional<CapabilityValue> getFogDistance() {
        return getValue(CapabilityId.FOG, fallback::getFogDistance);
    }

    @Override
    public boolean setFogDistance(CapabilityValue value) {
        return setValue(CapabilityId.FOG, value, () -> fallback.setFogDistance(value));
    }

    @Override
    public Optional<CapabilityValue> getVsync() {
        return getValue(CapabilityId.VSYNC, fallback::getVsync);
    }

    @Override
    public boolean setVsync(CapabilityValue value) {
        return setValue(CapabilityId.VSYNC, value, () -> fallback.setVsync(value));
    }

    @Override
    public Optional<CapabilityValue> getGraphicsMode() {
        return getValue(CapabilityId.GRAPHICS_MODE, fallback::getGraphicsMode);
    }

    @Override
    public boolean setGraphicsMode(CapabilityValue value) {
        return setValue(CapabilityId.GRAPHICS_MODE, value, () -> fallback.setGraphicsMode(value));
    }

    @Override
    public Optional<CapabilityValue> getDistortionEffectScale() {
        return getValue(CapabilityId.DISTORTION_EFFECT, fallback::getDistortionEffectScale);
    }

    @Override
    public boolean setDistortionEffectScale(CapabilityValue value) {
        return setValue(CapabilityId.DISTORTION_EFFECT, value, () -> fallback.setDistortionEffectScale(value));
    }

    @Override
    public Optional<CapabilityValue> getArmorStands() {
        return getValue(CapabilityId.ARMOR_STANDS, fallback::getArmorStands);
    }

    @Override
    public boolean setArmorStands(CapabilityValue value) {
        return setValue(CapabilityId.ARMOR_STANDS, value, () -> fallback.setArmorStands(value));
    }

    @Override
    public Optional<CapabilityValue> getItemFrames() {
        return getValue(CapabilityId.ITEM_FRAMES, fallback::getItemFrames);
    }

    @Override
    public boolean setItemFrames(CapabilityValue value) {
        return setValue(CapabilityId.ITEM_FRAMES, value, () -> fallback.setItemFrames(value));
    }

    @Override
    public Optional<CapabilityValue> getBlockEntities() {
        return getValue(CapabilityId.BLOCK_ENTITIES, fallback::getBlockEntities);
    }

    @Override
    public boolean setBlockEntities(CapabilityValue value) {
        return setValue(CapabilityId.BLOCK_ENTITIES, value, () -> fallback.setBlockEntities(value));
    }

    @Override
    public Optional<CapabilityValue> getAnimations() {
        return getValue(CapabilityId.ANIMATIONS, fallback::getAnimations);
    }

    @Override
    public boolean setAnimations(CapabilityValue value) {
        return setValue(CapabilityId.ANIMATIONS, value, () -> fallback.setAnimations(value));
    }

    private Optional<CapabilityValue> getValue(CapabilityId capability,
            Supplier<Optional<CapabilityValue>> fallbackSupplier) {
        Optional<CompatAdapter> adapterOpt = compatRegistry.getAdapter(capability);
        if (adapterOpt.isPresent()) {
            Optional<CapabilityValue> value = adapterOpt.get().getCurrentValue(capability);
            if (value.isPresent()) {
                return value;
            }
        }
        return fallbackSupplier.get();
    }

    private boolean setValue(CapabilityId capability, CapabilityValue value, Supplier<Boolean> fallbackSupplier) {
        Optional<CompatAdapter> adapterOpt = compatRegistry.getAdapter(capability);
        if (adapterOpt.isPresent()) {
            boolean applied = adapterOpt.get().apply(capability, value);
            if (applied) {
                return true;
            }
        }
        return fallbackSupplier.get();
    }
}
