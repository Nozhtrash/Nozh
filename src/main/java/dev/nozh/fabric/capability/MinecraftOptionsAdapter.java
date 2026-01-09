package dev.nozh.fabric.capability;

import dev.nozh.core.capability.CapabilityValue;

import java.util.Optional;

/**
 * Abstraction for Minecraft options access.
 * 
 * Allows headless testing without actual MC runtime.
 * Production implementation uses net.minecraft.client.option.GameOptions.
 */
public interface MinecraftOptionsAdapter {

    /**
     * Get current particles setting.
     * 
     * @return Current particles value, or empty if unavailable
     */
    Optional<CapabilityValue> getParticles();

    /**
     * Set particles setting.
     * 
     * @param value New particles value
     * @return true if set successfully, false otherwise
     */
    boolean setParticles(CapabilityValue value);

    /**
     * Get current clouds setting.
     */
    Optional<CapabilityValue> getClouds();

    /**
     * Set clouds setting.
     */
    boolean setClouds(CapabilityValue value);

    /**
     * Get current entity shadows setting.
     */
    Optional<CapabilityValue> getEntityShadows();

    /**
     * Set entity shadows setting.
     */
    boolean setEntityShadows(CapabilityValue value);

    /**
     * Get current FPS cap.
     */
    Optional<CapabilityValue> getFpsCap();

    /**
     * /**
     * Set FPS cap.
     */
    boolean setFpsCap(CapabilityValue value);

    /**
     * Get current render distance (chunks).
     */
    Optional<CapabilityValue> getRenderDistance();

    /**
     * Set render distance (chunks).
     */
    boolean setRenderDistance(CapabilityValue value);

    /**
     * Get current simulation distance (chunks).
     */
    Optional<CapabilityValue> getSimulationDistance();

    /**
     * Set simulation distance (chunks).
     */
    boolean setSimulationDistance(CapabilityValue value);

    /**
     * Get current biome blend radius (blocks).
     */
    Optional<CapabilityValue> getBiomeBlendRadius();

    /**
     * Set biome blend radius (blocks).
     */
    boolean setBiomeBlendRadius(CapabilityValue value);

    /**
     * Get current entity distance (percentage).
     */
    Optional<CapabilityValue> getEntityDistance();

    /**
     * Set entity distance (percentage).
     */
    boolean setEntityDistance(CapabilityValue value);

    /**
     * Get current mipmap levels.
     */
    Optional<CapabilityValue> getMipmapLevels();

    /**
     * Set mipmap levels.
     */
    boolean setMipmapLevels(CapabilityValue value);

    /**
     * Get current fog distance (used for render distance adjustment).
     */
    Optional<CapabilityValue> getFogDistance();

    /**
     * Set fog distance.
     */
    boolean setFogDistance(CapabilityValue value);

    /**
     * Get vsync setting.
     */
    Optional<CapabilityValue> getVsync();

    /**
     * Set vsync setting.
     */
    boolean setVsync(CapabilityValue value);

    /**
     * Get graphics mode (FAST, FANCY, FABULOUS).
     */
    Optional<CapabilityValue> getGraphicsMode();

    /**
     * Set graphics mode.
     */
    boolean setGraphicsMode(CapabilityValue value);

    /**
     * Get distortion effect scale (0.0 - 1.0).
     */
    Optional<CapabilityValue> getDistortionEffectScale();

    /**
     * Set distortion effect scale.
     */
    boolean setDistortionEffectScale(CapabilityValue value);

    /**
     * Get Armor Stands visibility (ON/OFF).
     */
    Optional<CapabilityValue> getArmorStands();

    /**
     * Set Armor Stands visibility.
     */
    boolean setArmorStands(CapabilityValue value);

    /**
     * Get Item Frames visibility (ON/OFF).
     */
    Optional<CapabilityValue> getItemFrames();

    /**
     * Set Item Frames visibility.
     */
    boolean setItemFrames(CapabilityValue value);

    /**
     * Get Block Entities visibility (ON/OFF).
     */
    Optional<CapabilityValue> getBlockEntities();

    /**
     * Set Block Entities visibility.
     */
    boolean setBlockEntities(CapabilityValue value);

    /**
     * Get All Animations visibility (ON/OFF).
     */
    Optional<CapabilityValue> getAnimations();

    /**
     * Set All Animations visibility.
     */
    boolean setAnimations(CapabilityValue value);
}
