package dev.nozh.core.compat;

import dev.nozh.NozhConstants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Adapter for Sodium mod integration.
 * Uses reflection to access Sodium options without hard dependency.
 * 
 * <p>Falls back gracefully when Sodium is not present.
 * 
 * @author Nozh Team
 * @since 0.5.0 (Phase 1 Sprint 2)
 */
public final class SodiumOptionsAdapter {
    private static final String SODIUM_OPTIONS_CLASS = "me.jellysquid.mods.sodium.client.SodiumClientMod";
    
    private static boolean sodiumAvailable = false;
    private static Method getOptionsMethod;
    private static Object sodiumOptions;
    private static boolean initializationAttempted = false;
    
    /**
     * Initialize Sodium adapter (lazy initialization).
     */
    private static void initialize() {
        if (initializationAttempted) {
            return;
        }
        
        initializationAttempted = true;
        
        try {
            Class<?> sodiumClass = Class.forName(SODIUM_OPTIONS_CLASS);
            getOptionsMethod = sodiumClass.getMethod("options");
            sodiumOptions = getOptionsMethod.invoke(null);
            sodiumAvailable = true;
            NozhConstants.LOGGER.info("Sodium detected and adapter enabled");
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
        if (!initializationAttempted) {
            initialize();
        }
        return sodiumAvailable;
    }
    
    /**
     * Check if we can control chunk rendering via Sodium.
     */
    public static boolean canControlChunkRendering() {
        if (!isSodiumAvailable()) {
            return false;
        }
        
        try {
            return sodiumOptions != null;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Cannot access Sodium options", e);
            return false;
        }
    }
    
    /**
     * Set chunk render distance via Sodium.
     * 
     * @throws UnsupportedOperationException if Sodium is not available
     */
    public static void setChunkRenderDistance(int distance) {
        if (!canControlChunkRendering()) {
            throw new UnsupportedOperationException("Sodium chunk rendering control not available");
        }
        
        try {
            // Access Sodium's chunk render distance field
            Field distanceField = sodiumOptions.getClass().getDeclaredField("chunkRenderDistance");
            distanceField.setAccessible(true);
            distanceField.set(sodiumOptions, distance);
            
            // Save options
            Method saveMethod = sodiumOptions.getClass().getMethod("save");
            saveMethod.invoke(sodiumOptions);
            
            NozhConstants.LOGGER.debug("Sodium chunk render distance set to: {}", distance);
        } catch (NoSuchFieldException e) {
            NozhConstants.LOGGER.warn("Sodium chunk render distance field not found (version mismatch?)");
            throw new UnsupportedOperationException("Sodium field access failed", e);
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to set Sodium chunk distance", e);
            throw new RuntimeException("Sodium operation failed", e);
        }
    }
    
    /**
     * Get current Sodium chunk render distance.
     * 
     * @return current distance, or -1 if not available
     */
    public static int getChunkRenderDistance() {
        if (!canControlChunkRendering()) {
            return -1;
        }
        
        try {
            Field distanceField = sodiumOptions.getClass().getDeclaredField("chunkRenderDistance");
            distanceField.setAccessible(true);
            Object value = distanceField.get(sodiumOptions);
            return value instanceof Integer ? (Integer) value : -1;
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to get Sodium chunk distance", e);
            return -1;
        }
    }
    
    private SodiumOptionsAdapter() {
        // Private constructor to prevent instantiation
    }
}
