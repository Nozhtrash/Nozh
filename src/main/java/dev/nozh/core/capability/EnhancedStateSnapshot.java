package dev.nozh.core.capability;

import dev.nozh.NozhConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced state snapshot with complete capture/restore.
 * 
 * ROADMAP: Phase 1, Sprint 2 - Complete StateSnapshot
 * 
 * Captures all modifiable options that Nozh can change.
 */
public class EnhancedStateSnapshot extends StateSnapshot {
    
    private final Map<String, Object> values = new HashMap<>();
    private final long captureTime;
    
    public EnhancedStateSnapshot() {
        this.captureTime = System.currentTimeMillis();
    }
    
    /**
     * Capture complete state from Minecraft client.
     */
    public static EnhancedStateSnapshot captureAll(MinecraftClient client) {
        if (client == null || client.options == null) {
            NozhConstants.LOGGER.error("Cannot capture state: client not ready");
            return null;
        }
        
        EnhancedStateSnapshot snapshot = new EnhancedStateSnapshot();
        GameOptions opts = client.options;
        
        try {
            // Capture all modifiable options
            snapshot.put("render_distance", opts.getViewDistance().getValue());
            snapshot.put("simulation_distance", opts.getSimulationDistance().getValue());
            snapshot.put("particles", opts.getParticles().getValue());
            snapshot.put("entity_distance", opts.getEntityDistanceScaling().getValue());
            snapshot.put("graphics_mode", opts.getGraphicsMode().getValue());
            snapshot.put("mipmap_levels", opts.getMipmapLevels().getValue());
            snapshot.put("smooth_lighting", opts.getSmoothLighting().getValue());
            snapshot.put("clouds", opts.getCloudRenderMode().getValue());
            snapshot.put("vsync", opts.getEnableVsync().getValue());
            snapshot.put("max_fps", opts.getMaxFps().getValue());
            
            NozhConstants.LOGGER.debug("State snapshot captured with {} values", 
                snapshot.values.size());
            
            return snapshot;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to capture state", e);
            return null;
        }
    }
    
    /**
     * Restore state to Minecraft client.
     */
    public boolean restore(MinecraftClient client) {
        if (client == null || client.options == null) {
            NozhConstants.LOGGER.error("Cannot restore state: client not ready");
            return false;
        }
        
        if (values.isEmpty()) {
            NozhConstants.LOGGER.warn("Cannot restore: no values in snapshot");
            return false;
        }
        
        try {
            GameOptions opts = client.options;
            int restored = 0;
            
            // Restore each captured value
            if (values.containsKey("render_distance")) {
                opts.getViewDistance().setValue((Integer) values.get("render_distance"));
                restored++;
            }
            
            if (values.containsKey("simulation_distance")) {
                opts.getSimulationDistance().setValue(
                    (Integer) values.get("simulation_distance"));
                restored++;
            }
            
            if (values.containsKey("particles")) {
                opts.getParticles().setValue(
                    (net.minecraft.client.option.ParticlesMode) values.get("particles"));
                restored++;
            }
            
            if (values.containsKey("entity_distance")) {
                opts.getEntityDistanceScaling().setValue(
                    (Double) values.get("entity_distance"));
                restored++;
            }
            
            if (values.containsKey("graphics_mode")) {
                opts.getGraphicsMode().setValue(
                    (net.minecraft.client.option.GraphicsMode) values.get("graphics_mode"));
                restored++;
            }
            
            if (values.containsKey("mipmap_levels")) {
                opts.getMipmapLevels().setValue((Integer) values.get("mipmap_levels"));
                restored++;
            }
            
            if (values.containsKey("smooth_lighting")) {
                opts.getSmoothLighting().setValue((Boolean) values.get("smooth_lighting"));
                restored++;
            }
            
            if (values.containsKey("clouds")) {
                opts.getCloudRenderMode().setValue(
                    (net.minecraft.client.option.CloudRenderMode) values.get("clouds"));
                restored++;
            }
            
            // Write changes to file
            opts.write();
            
            NozhConstants.LOGGER.info("State restored: {} values", restored);
            return true;
            
        } catch (Exception e) {
            NozhConstants.LOGGER.error("Failed to restore state", e);
            return false;
        }
    }
    
    @Override
    public void put(String key, Object value) {
        values.put(key, value);
    }
    
    @Override
    public Object get(String key) {
        return values.get(key);
    }
    
    @Override
    public boolean has(String key) {
        return values.containsKey(key);
    }
    
    /**
     * Get capture timestamp.
     */
    public long getCaptureTime() {
        return captureTime;
    }
    
    /**
     * Get age in milliseconds.
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - captureTime;
    }
    
    /**
     * Get number of captured values.
     */
    public int size() {
        return values.size();
    }
}