package dev.nozh.core.compat;

import dev.nozh.NozhConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Adapter for Sodium mod integration.
 * 
 * ROADMAP: Phase 1, Sprint 2 - Sodium Adapter
 * 
 * Uses reflection to detect and control Sodium when available.
 */
public class SodiumOptionsAdapter {
    
    private static final String SODIUM_OPTIONS_CLASS = 
        "me.jellysquid.mods.sodium.client.SodiumClientMod";
    
    private static boolean sodiumAvailable = false;
    private static Method getOptionsMethod = null;
    private static Object sodiumOptions = null;
    
    static {
        try {
            Class<?> sodiumClass = Class.forName(SODIUM_OPTIONS_CLASS);
            getOptionsMethod = sodiumClass.getMethod("options");
            sodiumAvailable = true;
            NozhConstants.LOGGER.info("Sodium detected, adapter enabled");
        } catch (ClassNotFoundException e) {
            NozhConstants.LOGGER.info("Sodium not found, using vanilla providers");
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Sodium adapter initialization failed", e);
        }
    }
    
    /**
     * Check if Sodium is available.
     */
    public static boolean isSodiumAvailable() {
        return sodiumAvailable;
    }
    
    /**
     * Check if can control chunk rendering.
     */
    public static boolean canControlChunkRendering() {
        if (!sodiumAvailable) {
            return false;
        }
        
        try {
            Object opts = getOptionsMethod.invoke(null);
            return opts != null;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Cannot access Sodium options", e);
            return false;
        }
    }
    
    /**
     * Set chunk render distance via Sodium.
     */
    public static boolean setChunkRenderDistance(int distance) {
        if (!canControlChunkRendering()) {
            NozhConstants.LOGGER.warn(
                "Sodium chunk rendering control not available");
            return false;
        }
        
        try {
            Object opts = getOptionsMethod.invoke(null);
            
            // Use reflection to change chunk render distance
            Field distanceField = opts.getClass()
                .getDeclaredField("chunkRenderDistance");
            distanceField.setAccessible(true);
            distanceField.set(opts, distance);
            
            // Save options
            Method saveMethod = opts.getClass().getMethod("save");
            saveMethod.invoke(opts);
            
            NozhConstants.LOGGER.info(
                "Sodium chunk render distance set to: {}", distance);
            return true;
            
        } catch (NoSuchFieldException e) {
            NozhConstants.LOGGER.warn(
                "Sodium API changed, field 'chunkRenderDistance' not found");
            return false;
        } catch (Exception e) {
            NozhConstants.LOGGER.error(
                "Failed to set Sodium chunk distance", e);
            return false;
        }
    }
    
    /**
     * Get current chunk render distance from Sodium.
     */
    public static int getChunkRenderDistance() {
        if (!canControlChunkRendering()) {
            return -1;
        }
        
        try {
            Object opts = getOptionsMethod.invoke(null);
            Field distanceField = opts.getClass()
                .getDeclaredField("chunkRenderDistance");
            distanceField.setAccessible(true);
            return (Integer) distanceField.get(opts);
        } catch (Exception e) {
            NozhConstants.LOGGER.error(
                "Failed to get Sodium chunk distance", e);
            return -1;
        }
    }
}